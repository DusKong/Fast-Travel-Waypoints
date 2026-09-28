package com.github.duskong.waystonemap.mixin;

import com.github.duskong.waystonemap.XaeroCommandRewriter;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerCommandMixin {

    @ModifyVariable(method = "sendCommand", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private String fastTravelWaypoints$rewriteCommand(String command) {
        return XaeroCommandRewriter.rewrite(command);
    }

    @ModifyVariable(method = "sendChat", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private String fastTravelWaypoints$rewriteChat(String message) {
        return XaeroCommandRewriter.rewrite(message);
    }
}
