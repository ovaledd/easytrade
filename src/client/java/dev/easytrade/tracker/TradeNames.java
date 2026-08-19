package dev.easytrade.tracker;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TradeNames {

	private TradeNames() {
	}

	public static String costTotal(List<MerchantOffer> offers) {
		Map<String, Integer> totals = new LinkedHashMap<>();
		for (MerchantOffer offer : offers) {
			addCost(totals, offer.getBaseCostA());
			ItemStack b = offer.getCostB();
			if (!b.isEmpty()) {
				addCost(totals, b);
			}
		}
		if (totals.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, Integer> entry : totals.entrySet()) {
			if (sb.length() > 0) {
				sb.append(" + ");
			}
			sb.append(entry.getValue()).append(' ').append(entry.getKey());
			if (entry.getValue() != 1) {
				sb.append('s');
			}
		}
		return sb.toString();
	}

	private static void addCost(Map<String, Integer> totals, ItemStack stack) {
		totals.merge(stack.getHoverName().getString(), stack.getCount(), Integer::sum);
	}

	public static String offerSignature(MerchantOffer offer) {
		StringBuilder sb = new StringBuilder();
		sb.append(itemKey(offer.getResult())).append(':').append(offer.getResult().getCount());
ItemEnchantments ench = enchantmentsOf(offer.getResult());
		for (Object2IntMap.Entry<net.minecraft.core.Holder<Enchantment>> entry : ench.entrySet()) {
			sb.append('~').append(entry.getKey().unwrapKey().map(k -> k.identifier().toString()).orElse("?"))
				.append(':').append(entry.getIntValue());
		}
		sb.append('|').append(itemKey(offer.getBaseCostA())).append(':').append(offer.getBaseCostA().getCount());
		if (!offer.getCostB().isEmpty()) {
			sb.append('|').append(itemKey(offer.getCostB())).append(':').append(offer.getCostB().getCount());
		}
		return sb.toString();
	}

	private static String itemKey(ItemStack stack) {
		Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return key != null ? key.toString() : "?";
	}

	public static ItemEnchantments enchantmentsOf(ItemStack stack) {
		if (stack.is(Items.ENCHANTED_BOOK)) {
			return stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
		}
		return stack.getEnchantments();
	}

	public static String offerTitle(MerchantOffer offer) {
		ItemStack out = offer.getResult();
		if (out.isEmpty()) {
			return "?";
		}
		ItemEnchantments enchantments = enchantmentsOf(out);
		if (!enchantments.isEmpty()) {
			Iterator<Object2IntMap.Entry<net.minecraft.core.Holder<Enchantment>>> it = enchantments.entrySet().iterator();
			Object2IntMap.Entry<net.minecraft.core.Holder<Enchantment>> first = it.next();
			Component name = Enchantment.getFullname(first.getKey(), first.getIntValue());
			if (it.hasNext()) {
				name = name.copy().append(" +");
			}
			return name.getString();
		}
		return out.getHoverName().getString();
	}

	public static String emeraldCost(MerchantOffer offer) {
		int total = 0;
		boolean any = false;
		ItemStack a = offer.getBaseCostA();
		if (a.is(Items.EMERALD)) {
			total += a.getCount();
			any = true;
		}
		ItemStack b = offer.getCostB();
		if (!b.isEmpty() && b.is(Items.EMERALD)) {
			total += b.getCount();
			any = true;
		}
		return any ? Integer.toString(total) : "";
	}

	public static String villagerTitle(AbstractVillager villager) {
		if (villager instanceof WanderingTrader) {
			return "Wandering Trader";
		}
		if (villager instanceof Villager v) {
			String profession = v.getVillagerData().profession().value().name().getString();
			if (profession == null || profession.isEmpty()) {
				profession = "Villager";
			}
			return profession;
		}
		return "Villager";
	}
}