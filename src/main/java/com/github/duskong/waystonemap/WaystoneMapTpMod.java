package com.github.duskong.waystonemap;

import com.github.duskong.waystonemap.utils.SkyUtils;
import com.github.duskong.waystonemap.utils.TeleportPos;
import com.github.duskong.waystonemap.utils.Utils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WaystoneMapTpMod implements ModInitializer {

    @Override
    public void onInitialize() {
        ModConfigs.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
    }

    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("wstp")
                        .requires(source -> source.getEntity() instanceof ServerPlayer)
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    if(!(source.getEntity() instanceof ServerPlayer player)) {
                                        source.sendFailure(Component.literal("This command can only be used by players."));
                                        return 0;
                                    }

                                    Vec3 vec = Vec3Argument.getVec3(ctx, "pos");
                                    BlockPos targetPos = BlockPos.containing(vec.x, vec.y, vec.z);
                                    ServerLevel level = source.getLevel();

                                    return handleTeleport(player, level, targetPos, vec, null);
                                })
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            CommandSourceStack source = ctx.getSource();
                                            if (!(source.getEntity() instanceof ServerPlayer player)) {
                                                source.sendFailure(Component.literal("This command can only be used by players."));
                                                return 0;
                                            }

                                            Vec3 vec = Vec3Argument.getVec3(ctx, "pos");
                                            BlockPos targetPos = BlockPos.containing(vec.x, vec.y, vec.z);
                                            ServerLevel level = source.getLevel();
                                            String name = StringArgumentType.getString(ctx, "name");

                                            return handleTeleport(player, level, targetPos, vec, name);
                                        })
                                )
                        )
        );

        /*dispatcher.register(
                Commands.literal("wstpxd")
                        .requires(source -> source.getEntity() instanceof ServerPlayer)
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    if (!(source.getEntity() instanceof ServerPlayer player)) {
                                        source.sendFailure(Component.literal("This command can only be used by players."));
                                        return 0;
                                    }

                                    Vec3 vec = Vec3Argument.getVec3(ctx, "pos");
                                    Identifier dimId = ResourceLocationArgument.getId(ctx, "dimension");
                                    String dim = dimId.toString();
                                })
                        )
        );*/
    }

    private int handleTeleport(ServerPlayer player, ServerLevel level, BlockPos targetPos, Vec3 exactPos, String waypointName) {
        // まずは、読み込み済みのチャンクのみを対象とした低コストなチェックを行います。
        // これにより、管理者やマップ機能による未読み込みエリアへのテレポートの際、移動先が
        // Waystone（ウェイストーン）かどうかを確認するためだけに、目的地のチャンクが同期的に読み込まれてしまうのを防ぎます。
        Optional<BlockPos> waystoneBottom = findWaystoneBottom(level, targetPos, false);

        //すでに読み込まれているWaystoneのウェイポイントであれば、必ず「セーフ・テレポート（safe-teleport）」を使用してください。
        if(waystoneBottom.isPresent()) {
            if(!canStartFastTravel(player, level)) {
                return 0;
            }
            FastTravelCost travelCost = FastTravelCost.free();
            startWaystoneTeleport(player, level, waystoneBottom.get(), exactPos, waypointName, travelCost);
            return 1;
        }

        // クリエイティブモードまたはスペクテイターモードのOPのみが、「どこへでもテレポートできる」機能を維持します。
        // サーバーのメインスレッドが一時停止（ヒッチ）するのを防ぐため、チャンク読み込みを伴うWaystoneの検証処理の前にこの処理を行います。
        // サバイバルモードやアドベンチャーモードのOP（管理者権限保持者）については、引き続き後続の通常のWaystone検証処理が行われます。
        if (canBypassWaystoneVerification(player)) {
            teleportPlayer(player, level, exactPos);
            return 1;
        }

        // サバイバル/アドベンチャーモードのOP（管理者権限保持者）およびOP権限を持たないすべてのプレイヤーは、依然として正規のWaystone（ウェイストーン）認証を行う必要があります。
        // 移動先のチャンクがアンロードされている場合、そのチャンクの読み込みや読み込み待ちが発生することがありますが、
        // これは意図的な挙動です。制限付きの高速移動を許可する前に、そのウェイポイントが実際に「Waystone（ウェイストーン）」であることをMOD側で確認する必要があるためです。
        waystoneBottom = findWaystoneBottom(level, targetPos, true);
        if (waystoneBottom.isPresent()) {
            if (!canStartFastTravel(player, level)) {
                return 0;
            }
            FastTravelCost travelCost = FastTravelCost.free();
            startWaystoneTeleport(player, level, waystoneBottom.get(), exactPos, waypointName, travelCost);
            return 1;
        }

        player.sendSystemMessage(Component.literal("No Waystone at the selected waypoint."), true);
        return 0;
    }

    private Optional<BlockPos> findWaystoneBottom(ServerLevel level, BlockPos targetPos, boolean allowChunkLoad) {
        int radiusXZ = 1;
        int baseY = targetPos.getY();
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

        for (int dy = -2; dy <= 2; dy++) {
            int y = baseY + dy;
            for (int dx = -radiusXZ; dx <= radiusXZ; dx++) {
                for (int dz = -radiusXZ; dz <= radiusXZ; dz++) {
                    checkPos.set(targetPos.getX() + dx, y, targetPos.getZ() + dz);
                    if (Utils.isWaystoneBlock(level, checkPos, allowChunkLoad)) {
                        BlockPos bottom = checkPos.immutable();
                        while (Utils.isWaystoneBlock(level, bottom.below(), allowChunkLoad)) {
                            bottom = bottom.below();
                        }
                        return Optional.of(bottom);
                    }
                }
            }
        }

        BlockPos belowTarget = targetPos.below();
        if (Utils.isWaystoneBlock(level, belowTarget, allowChunkLoad)) {
            BlockPos bottom = belowTarget;
            while (Utils.isWaystoneBlock(level, bottom.below(), allowChunkLoad)) {
                bottom = bottom.below();
            }
            return Optional.of(bottom);
        }

        return Optional.empty();
    }

    private boolean canStartFastTravel(ServerPlayer player, ServerLevel targetLevel) {
        ServerLevel level = player.level();
        boolean crossDimensional = isCrossDimensionalFastTravel(player, targetLevel);

        if (crossDimensional && !ModConfigs.ENABLE_CROSS_DIMENSIONAL_TRAVEL.get()) {
            player.sendSystemMessage(
                    Component.translatable("waystonemap.message.disable.cross-dimensional").withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }

        // クリエイティブモードやスペクテイターモードでは、制限や経験値（XP）の消費を無視、上記のサーバーにおける次元間移動の切り替え設定は無視しない
        if (player.gameMode.getGameModeForPlayer() == GameType.CREATIVE || player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) return true;

        // 「空が開けていること」という条件は、プレイヤーの現在の次元で判定されます。
        if (ModConfigs.requireOpenSkyPlayer() && SkyUtils.isOpenSkyCheckEnabledInThisDimension(level)) {
            BlockPos pos = player.blockPosition().above();
            if (!SkyUtils.canSeeSkyIgnoringLeaves(level, pos)) {
                player.sendSystemMessage(
                        Component.literal("Fast travel requires open sky.").withStyle(ChatFormatting.RED),
                        true
                );
                return false;
            }
        }

        return true;
    }

    private void startWaystoneTeleport(ServerPlayer player, ServerLevel level, BlockPos waystoneBottom, Vec3 fallbackExact, String waypointName, FastTravelCost travelCost) {
        Optional<Vec3> safeNow = TeleportPos.findSafeTeleportPos(level, waystoneBottom);
        if (safeNow.isEmpty()) {
            player.sendSystemMessage(Component.literal("No empty space next to that waystone.")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        if (!(player.gameMode.getGameModeForPlayer() == GameType.CREATIVE || player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR)) {
            if (ModConfigs.REQUIRE_OPEN_SKY_DESTINATION.get() && SkyUtils.isOpenSkyCheckEnabledInThisDimension(level)) {
                BlockPos destCheck = BlockPos.containing(safeNow.get()).above();
                if (!SkyUtils.canSeeSkyIgnoringLeaves(level, destCheck)) {
                    player.sendSystemMessage(Component.literal("Fast travel destination requires open sky.")
                            .withStyle(ChatFormatting.RED), true);
                    return;
                }
            }
        }

        ServerLevel pl = player.level();
        completeTeleportWithFx(player, level, waystoneBottom, fallbackExact, travelCost);
    }

    private void completeTeleportWithFx(ServerPlayer player, ServerLevel level, BlockPos waystoneBottom, Vec3 fallbackExact, FastTravelCost travelCost) {
        safeTeleportToWaystoneNow(player, level, waystoneBottom, fallbackExact);
    }

    private void safeTeleportToWaystoneNow(ServerPlayer player, ServerLevel level, BlockPos waystoneBottom, Vec3 fallbackExact) {
        Optional<Vec3> safe = TeleportPos.findSafeTeleportPos(level, waystoneBottom);
        if (safe.isPresent()) {
            teleportPlayer(player, level, safe.get());
            return;
        }

        // Never force-teleport into blocks (even for OPs). If no safe spot exists, fail with a clear message.
        player.sendSystemMessage(Component.literal("No empty space next to that waystone.")
                .withStyle(net.minecraft.ChatFormatting.RED), true);
    }

    private boolean isCrossDimensionalFastTravel(ServerPlayer player, ServerLevel targetLevel) {
        return targetLevel != null && !player.level().dimension().equals(targetLevel.dimension());
    }

    private boolean canBypassWaystoneVerification(ServerPlayer player) {
        GameType gameMode = player.gameMode.getGameModeForPlayer();
        return /*player.hasPermissions(2) &&*/ (gameMode == GameType.CREATIVE || gameMode == GameType.SPECTATOR);
    }


    private void teleportPlayer(ServerPlayer player, ServerLevel level, Vec3 exactPos) {
        double x = exactPos.x;
        double y = exactPos.y;
        double z = exactPos.z;
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        player.teleportTo(level, x, y, z, Set.of(), yaw, pitch, true);
    }
}
