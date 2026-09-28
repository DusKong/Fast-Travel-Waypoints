package com.github.duskong.waystonemap.utils;

import com.github.duskong.waystonemap.ModConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class SkyUtils {

    public static boolean isOpenSkyCheckEnabledInThisDimension(ServerLevel level) {
        // Only enforce open-sky in whitelisted dimensions (defaults: overworld + the_end).
        // If the whitelist is empty, treat it as "disabled everywhere".
        var list = ModConfigs.OPEN_SKY_DIMENSION_WHITELIST.get();
        if (list == null || list.isEmpty()) return false;

        Identifier dim = level.dimension().identifier();
        for (String s : list) {
            if (s == null || s.isBlank()) continue;
            Identifier rl = Identifier.tryParse(s);
            if (rl != null && rl.equals(dim)) return true;
        }
        return false;
    }

    public static boolean canSeeSkyIgnoringLeaves(ServerLevel level, BlockPos pos) {
        // "Open sky" check that:
        // - ignores leaves
        // - ignores ALL fluids (water, lava, modded fluids)
        // - allows transparent skylight-propagating blocks like glass
        //
        // We treat the sky as "visible" if there is no block above that *stops skylight propagation*.
        // This matches the intuitive "outdoor" feel and still blocks under solid ceilings (including slabs/stairs),
        // while allowing glass roofs.
        int maxY = level.getMaxY() - 1;
        int x = pos.getX();
        int z = pos.getZ();

        for (int y = pos.getY(); y <= maxY; y++) {
            BlockPos p = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(p);

            if (state.isAir()) continue;
            // Treat pure fluids (water/lava/modded) like air, but do NOT ignore waterlogged solid blocks.
            if (!state.getFluidState().isEmpty() && state.getCollisionShape(level, p).isEmpty()) continue;
            if (state.is(net.minecraft.tags.BlockTags.LEAVES)) continue;

            // If skylight can't propagate through this block, it's not "open sky".
            if (!state.propagatesSkylightDown()) {
                return false;
            }
        }
        return true;
    }

}
