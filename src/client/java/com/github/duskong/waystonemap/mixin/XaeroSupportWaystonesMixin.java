package com.github.duskong.waystonemap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.WaypointVisibilityType;
import xaero.hud.minimap.waypoint.thirdparty.ThirdPartyWaypoints;

@Mixin(targets = "xaero.hud.compat.mods.SupportWaystones")
public class XaeroSupportWaystonesMixin {

    @Redirect(
            method = "addWaypoint(Lnet/blay09/mods/waystones/api/Waystone;Lxaero/hud/minimap/world/container/MinimapWorldRootContainer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/hud/minimap/waypoint/thirdparty/ThirdPartyWaypoints;add(Ljava/lang/String;Lxaero/common/minimap/waypoints/Waypoint;)V",
                    remap = false
            ),
            require = 0
    )
    private void fastTravelWaypoints$add(ThirdPartyWaypoints instance, String id, Waypoint waypoint) {
        waypoint.setName("waystones.waystone.name");
        waypoint.setInitials("☰");
        waypoint.setVisibility(WaypointVisibilityType.WORLD_MAP_LOCAL);
        instance.add(id, waypoint);
    }
}
