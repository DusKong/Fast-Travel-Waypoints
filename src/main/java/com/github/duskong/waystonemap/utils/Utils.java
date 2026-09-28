package com.github.duskong.waystonemap.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Utils {

    private static final Map<Block, Boolean> WAYSTONE_BLOCK_CACHE = new ConcurrentHashMap<>();

    public static boolean isWaystoneBlock(Level level, BlockPos pos) {
        return isWaystoneBlock(level, pos, true);
    }

    public static boolean isWaystoneBlock(Level level, BlockPos pos, boolean allowChunkLoad) {
        if (level.isOutsideBuildHeight(pos)) return false;

        // getBlockState() on an unloaded server chunk can synchronously load/wait for that chunk.
        // hasChunkAt() lets detection/interception paths avoid causing ServerChunkCache.waitForTasks().
        if (!allowChunkLoad && !level.hasChunkAt(pos)) return false;

        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;

        // Cache per Block instance so repeated scans do not repeatedly ask the registry.
        return WAYSTONE_BLOCK_CACHE.computeIfAbsent(state.getBlock(), block -> {
            Identifier rl = BuiltInRegistries.BLOCK.getKey(block);
            return rl != null && "waystones".equals(rl.getNamespace()) && rl.getPath().contains("waystone");
        });
    }
}
