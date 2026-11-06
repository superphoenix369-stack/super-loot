package com.superloot.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Adds a settings button for SuperLoot on Mod Menu's "Mods" screen. Only loaded if Mod Menu is installed. */
public class SuperLootModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return SuperLootSettingsScreen::new;
	}
}
