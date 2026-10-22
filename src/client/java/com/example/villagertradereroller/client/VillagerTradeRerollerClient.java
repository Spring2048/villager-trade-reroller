
package com.example.villagertradereroller.client;

import com.example.villagertradereroller.VillagerTradeReroller;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
//import net.minecraft.client.util.InputConstants;
import org.lwjgl.glfw.GLFW;

public class VillagerTradeRerollerClient implements ClientModInitializer {

	private static KeyMapping openSelectionKey;
	private static boolean wasKeyDown = false;

	@Override
	public void onInitializeClient() {
		openSelectionKey = KeyBindingHelper.registerKeyBinding(
				new KeyMapping(
						"key.villager-trade-reroller.open_selection",
						GLFW.GLFW_KEY_O,
						"category.villager-trade-reroller"
				)
		);

		ScreenEvents.AFTER_INIT.register(
				(client, screen, scaledWidth, scaledHeight) -> {
					if (!(screen instanceof MerchantScreen)) {
						return;
					}

					ScreenKeyboardEvents.beforeKeyPress(screen).register(
							(currentScreen, key, scancode, modifiers) -> {
								if (!openSelectionKey.matches(key, scancode)) {
									return;
								}

								client.setScreen(
										new EnchantmentSelectionScreen(currentScreen)
								);
							}
					);
				}
		);

		VillagerTradeReroller.LOGGER.info(
				"Villager Trade Reroller client initialized."
		);
	}
}
