package com.github.duskong.waystonemap;

import net.blay09.mods.waystones.client.gui.screen.WaystoneSelectionScreenBase;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

public class WaystoneMapToClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof WaystoneSelectionScreenBase) {
				client.gui.setScreen(null);
			}
		});
	}
}