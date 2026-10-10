package com.nonaddon.mixin;

import com.nonid.entity.HorseLiquidCollectorEntity;
import com.nonid.NonAddonBridge;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets automation take the liquid bottle out of a full horse liquid collector.
 *
 * <p>NoN only takes the bottle in a {@code UseEntityCallback} handler, which Fabric fires from
 * {@code ServerPlayNetworkHandler}'s intercept-entity packet handler. Anything that calls
 * {@link PlayerEntity#interact(Entity, Hand)} directly - Create's Deployer does exactly that with
 * its fake player - never reaches that handler, and the collector does not override
 * {@code Entity.interact}, so the interaction used to end in a silent PASS.
 *
 * <p>This hook runs at the head of {@code PlayerEntity.interact}, the very method the Deployer
 * calls, and repeats what NoN does: eat one glass bottle from the interacted hand, hand the player
 * a filled liquid bottle and clear the collector. A real player never gets here while holding a
 * glass bottle, because NoN's own handler accepts the interaction first and Fabric cancels the
 * vanilla path for it, so the bottle is never consumed twice.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerInteractMixin {
    @Inject(
            method = "method_7287(Lnet/minecraft/class_1297;Lnet/minecraft/class_1268;)Lnet/minecraft/class_1269;",
            at = @At("HEAD"),
            cancellable = true)
    private void nonaddon$fillBottleFromCollector(Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!(entity instanceof HorseLiquidCollectorEntity collector) || !collector.isCollectorFull()) {
            return;
        }
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity player)) {
            return;
        }
        ItemStack held = self.getStackInHand(hand);
        if (held.isEmpty() || !held.isOf(Items.GLASS_BOTTLE)) {
            return;
        }
        if (!NonAddonBridge.consumeOneGlassBottle(player, hand)) {
            return;
        }
        ItemStack bottle = NonAddonBridge.createLiquidBottleStack(collector.getBottleLiquidEntityTypeId());
        collector.clearStoredLiquid();
        NonAddonBridge.giveItemOrDrop(player, bottle);
        self.getEntityWorld().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.NEUTRAL, 1.0F, 1.0F);
        cir.setReturnValue(ActionResult.SUCCESS);
    }
}
