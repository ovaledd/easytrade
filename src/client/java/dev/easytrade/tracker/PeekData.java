package dev.easytrade.tracker;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public record PeekData(
	String title,
	String levelText,
	String itemName,
	String price,
	String mainCost,
	ItemStack mainIcon,
	ItemStack costA,
	ItemStack costB,
	List<OfferLine> lines,
	boolean matched,
	long seenAtTick) {

	public record OfferLine(String name, ItemStack output, boolean match, String price) {
	}
}