package com.superloot.client;

import java.util.Arrays;
import java.util.Locale;

import com.mojang.serialization.Codec;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

import com.superloot.config.SuperLootConfig;
import com.superloot.config.SuperLootConfig.ChestAmount;
import com.superloot.config.SuperLootConfig.LootQuality;

/** The SuperLoot settings page, opened from the Mods screen (Mod Menu). */
public class SuperLootSettingsScreen extends OptionsSubScreen {
	public SuperLootSettingsScreen(Screen previous) {
		super(previous, Minecraft.getInstance().options, Component.translatable("superloot.options.title"));
	}

	@Override
	protected void addOptions() {
		if (this.list != null) {
			this.list.addBig(enumOption("superloot.option.chest_amount", ChestAmount.class,
					SuperLootConfig.chestAmount(), SuperLootConfig::setChestAmount));
			this.list.addBig(enumOption("superloot.option.loot_quality", LootQuality.class,
					SuperLootConfig.lootQuality(), SuperLootConfig::setLootQuality));
		}
	}

	@Override
	public void removed() {
		super.removed();
		SuperLootConfig.save();
	}

	private static <E extends Enum<E>> OptionInstance<E> enumOption(String key, Class<E> type, E current, java.util.function.Consumer<E> setter) {
		E[] values = type.getEnumConstants();

		return new OptionInstance<>(
				key,
				value -> Tooltip.create(Component.translatable(valueKey(key, value) + ".tooltip")),
				(caption, value) -> Component.translatable(valueKey(key, value)),
				new OptionInstance.Enum<>(Arrays.asList(values), Codec.STRING.xmap(
						name -> Enum.valueOf(type, name.toUpperCase(Locale.ROOT)),
						value -> value.name().toLowerCase(Locale.ROOT))),
				current,
				setter::accept
		);
	}

	private static String valueKey(String key, Enum<?> value) {
		return key + "." + value.name().toLowerCase(Locale.ROOT);
	}
}
