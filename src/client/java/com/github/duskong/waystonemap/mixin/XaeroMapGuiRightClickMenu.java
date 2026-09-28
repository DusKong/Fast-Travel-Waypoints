package com.github.duskong.waystonemap.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.lib.client.gui.widget.dropdown.IDropDownContainer;
import xaero.map.element.HoveredMapElementHolder;
import xaero.map.gui.GuiMap;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.GuiRightClickMenu;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaero.map.mods.SupportMods;
import xaero.map.mods.gui.WaypointReader;

import java.util.ArrayList;
import java.util.Objects;

@Mixin(GuiRightClickMenu.class)
public class XaeroMapGuiRightClickMenu {

    @Inject(
            method = "<init>(Lxaero/map/gui/IRightClickableElement;Ljava/util/ArrayList;Lnet/minecraft/client/gui/screens/Screen;IIIILxaero/lib/client/gui/widget/dropdown/IDropDownContainer;)V",
            at = @At("HEAD")
    )
    private static void onConstructHead(IRightClickableElement target, ArrayList<RightClickOption> actionOptions, Screen screen, int x, int y, int w, int h, IDropDownContainer container, CallbackInfo ci) {
        if(target instanceof HoveredMapElementHolder<?,?> elementHolder) {
            if(elementHolder.getElement() instanceof xaero.map.mods.gui.Waypoint guiWaypoint) {
                if(guiWaypoint.getOriginal() instanceof xaero.common.minimap.waypoints.Waypoint waypoint) {
                    if(waypoint.isThirdParty()) {
                        if(waypoint.getThirdPartyOrigin().toString().equals("waystones:waystone")) {
                            if(actionOptions.size() == 8) {
                                actionOptions.subList(4, 7).clear();
                                actionOptions.remove(2);
                            }
                        }
                    }
                }
            }
        }
    }
}
