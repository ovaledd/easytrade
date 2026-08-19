package dev.easytrade.mixin;

import dev.easytrade.tracker.TradeWatcher;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

	@Inject(method = "interact", at = @At("HEAD"))
	private void easytrade$onManualInteract(Player player, Entity entity,
		InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (!TradeWatcher.isPeekInteract() && entity instanceof AbstractVillager) {
			TradeWatcher.onPlayerInteract();
		}
	}
}