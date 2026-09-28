package com.github.duskong.waystonemap;

public class FastTravelCost {

    final int xpPoints;
    final int levels;
    final boolean resolved;

    private FastTravelCost(int xpPoints, int levels, boolean resolved) {
        this.xpPoints = Math.max(0, xpPoints);
        this.levels = Math.max(0, levels);
        this.resolved = resolved;
    }

    static FastTravelCost free() {
        return new FastTravelCost(0, 0, true);
    }

    static FastTravelCost configuredLevels(int levels) {
        return new FastTravelCost(0, levels, true);
    }

    static FastTravelCost waystones(int xpPoints, int levels) {
        return new FastTravelCost(xpPoints, levels, true);
    }

    static FastTravelCost failed() {
        return new FastTravelCost(0, 0, false);
    }

    boolean isFree() {
        return resolved && xpPoints <= 0 && levels <= 0;
    }
}
