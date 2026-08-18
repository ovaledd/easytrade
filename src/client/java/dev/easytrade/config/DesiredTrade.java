package dev.easytrade.config;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

public record DesiredTrade(String type, String id, int level) {

	public boolean matches(ItemStack output) {
		if (output.isEmpty()) {
			return false;
		}
		if (this.type.equals("item")) {
			Identifier key = BuiltInRegistries.ITEM.getKey(output.getItem());
			return key != null && key.toString().equals(this.id);
		}
		if (this.type.equals("enchantment")) {
			ItemEnchantments enchantments = output.is(Items.ENCHANTED_BOOK)
				? output.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY)
				: output.getEnchantments();
			for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
				ResourceKey<Enchantment> key = entry.getKey().unwrapKey().orElse(null);
				if (key != null && key.identifier().toString().equals(this.id) && (this.level <= 0 || entry.getIntValue() >= this.level)) {
					return true;
				}
			}
		}
		return false;
	}
}