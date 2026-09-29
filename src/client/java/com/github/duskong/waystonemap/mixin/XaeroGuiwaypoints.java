package com.github.duskong.waystonemap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.gui.GuiWaypoints;
import xaero.common.minimap.waypoints.Waypoint;

import java.util.ArrayList;

@Mixin(value = GuiWaypoints.class, remap = false)
public class XaeroGuiwaypoints {

    @Shadow
    private ArrayList<Waypoint> thirdPartyWaypointsSorted;

    @Inject(method = "updateSortedList", at = @At("TAIL"))
    private void onUpdateSortedList(CallbackInfo ci) {
        if (this.thirdPartyWaypointsSorted == null) return;

        this.thirdPartyWaypointsSorted.removeIf(waypoint ->
                waypoint.getThirdPartyOrigin().toString().equals("waystones:waystone")
        );
    }
}
