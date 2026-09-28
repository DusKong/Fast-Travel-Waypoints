package com.github.duskong.waystonemap.mixin;

import com.github.duskong.waystonemap.XaeroWorldSafetyBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "xaero.hud.minimap.waypoint.WaypointTeleport", remap = false)
public class XaeroTeleportSafetyMixin {

    @Inject(
            method = "isTeleportationSafe(Lxaero/hud/minimap/world/MinimapWorld;)Z",
            at = @At("RETURN"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void fastTravelWaypoints$allowMatchingCrossDimensionSubWorld(
            @Coerce Object targetWorld,
            CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())
                && XaeroWorldSafetyBridge.isSameSubWorldAcrossDimensions(this, targetWorld)) {
            cir.setReturnValue(true);
        }
    }
}
