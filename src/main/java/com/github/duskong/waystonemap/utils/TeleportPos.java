package com.github.duskong.waystonemap.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class TeleportPos {

    private static final Direction[] SIDE_ORDER = new Direction[]{
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    public static Optional<Vec3> findSafeTeleportPos(ServerLevel level, BlockPos waystoneBottom) {
        // 1) 床と同一の高さ
        Optional<Vec3> same = findAdjacent(level, waystoneBottom, true, new int[]{0});
        if (same.isPresent()) return same;

        // 2) 高さ1ブロックの隣接ブロック（ウェイストーンの隣のブロックにプレイヤーが立つことができます）
        Optional<Vec3> atopNeighbor = findAdjacentTop(level, waystoneBottom);
        if (atopNeighbor.isPresent()) return atopNeighbor;

        // 3) ウェイストーンの上（ウェイストーンは高さ2ブロックなので、下から2ブロック目）
        // プレイヤーを底のない崖の縁に配置する前に、こちらを優先してください。
        BlockPos aboveFeet = waystoneBottom.above(2);
        if (isTwoTallFree(level, aboveFeet, true)) {
            return Optional.of(centerFeet(aboveFeet));
        }

        // 4) 床付きの低層レベル（最大2ブロック下まで）
        Optional<Vec3> lower = findAdjacent(level, waystoneBottom, true, new int[]{-1, -2});
        if (lower.isPresent()) return lower;

        // 5) 下限要件なしのフォールバック（同等 → 下位 → 上位）
        Optional<Vec3> sameNoFloor = findAdjacent(level, waystoneBottom, false, new int[]{0, -1, -2});
        if (sameNoFloor.isPresent()) return sameNoFloor;

        if (isTwoTallFree(level, aboveFeet, false)) {
            return Optional.of(centerFeet(aboveFeet));
        }

        return Optional.empty();
    }

    public static Optional<Vec3> findAdjacentTop(ServerLevel level, BlockPos waystoneBottom) {
        for (Direction dir : SIDE_ORDER) {
            BlockPos neighbor = waystoneBottom.relative(dir);
            BlockPos feet = neighbor.above();
            // Require a floor (the neighbor block) and 2 blocks of headroom at the feet position.
            if (isTwoTallFree(level, feet, true)) {
                return Optional.of(centerFeet(feet));
            }
        }
        return Optional.empty();
    }


    public static Optional<Vec3> findAdjacent(ServerLevel level, BlockPos waystoneBottom, boolean requireFloor, int[] yOffsets) {
        for (int yOff : yOffsets) {
            for (Direction dir : SIDE_ORDER) {
                BlockPos feet = waystoneBottom.relative(dir).offset(0, yOff, 0);
                if (isTwoTallFree(level, feet, requireFloor)) {
                    return Optional.of(centerFeet(feet));
                }
            }
        }
        return Optional.empty();
    }


    public static boolean isTwoTallFree(ServerLevel level, BlockPos feet, boolean requireFloor) {
        if (!isNonColliding(level, feet)) return false;
        if (!isNonColliding(level, feet.above())) return false;

        if (requireFloor) {
            BlockPos floorPos = feet.below();
            BlockState floor = level.getBlockState(floorPos);
            // Waystone tops may not report as "sturdy" even though you can stand on them.
            // Allow standing on the waystone itself (used for the "above waystone" case).
            if (!floor.isFaceSturdy(level, floorPos, Direction.UP) && !Utils.isWaystoneBlock(level, floorPos)) return false;
        }

        return true;
    }

    public static boolean isNonColliding(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty();
    }

    public static Vec3 centerFeet(BlockPos feet) {
        return new Vec3(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D);
    }
}
