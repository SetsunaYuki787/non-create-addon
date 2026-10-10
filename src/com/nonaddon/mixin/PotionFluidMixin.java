package com.nonaddon.mixin;

import java.util.Optional;

import com.nonid.NonAddonBridge;
import com.zurrtum.create.infrastructure.component.BottleType;
import com.zurrtum.create.infrastructure.fluids.FluidStack;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.potion.Potion;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives every NoN liquid bottle one and the same potion fluid colour, so a single Create recipe can
 * accept all of them.
 *
 * <p>A NoN bottle stores its liquid as a vanilla {@code minecraft:potion_contents} component whose
 * {@code potion} is {@code needsofnature:liquid} and whose {@code custom_color} is derived from the
 * donor entity (a per-entity colour from the config, or NoN's fallback {@code #FFFCEB}). Create's
 * {@code FluidStackIngredient} matches components entry by entry with plain {@code equals}, so a
 * recipe can only ever describe one exact colour - with the colour left as-is a recipe either has
 * to drop the component entirely (and then any vanilla potion matches too) or be duplicated once
 * per colour.
 *
 * <p>Rewriting the colour here - at Create's single funnel for potion fluids, which the Item Drain
 * reaches through {@code GenericItemEmptying.emptyItem -> PotionFluidHandler.emptyPotion ->
 * getFluidFromPotionItem -> getFluidFromPotion -> getFluidStack} - makes every drained semen fluid
 * identical to the fallback-coloured one, which is exactly what
 * {@code non-create-addon:brass_ingot_from_semen} describes. Vanilla potions are untouched: their component
 * carries a different {@code potion} entry, so they still fail the recipe.
 *
 * <p>The injection point is at {@code RETURN}, so the fluid (including Create's bottle-type
 * component) is already built and only the potion component is replaced. The potion entry itself is
 * copied over rather than looked up again, which keeps registry-entry identity intact for the
 * recipe comparison.
 */
@Mixin(targets = "com.zurrtum.create.content.fluids.potion.PotionFluidHandler")
public class PotionFluidMixin {
    /** {@code needsofnature:liquid}, the potion behind every NoN liquid bottle. */
    private static final Identifier NONADDON_LIQUID = Identifier.of("needsofnature", "liquid");

    /** NoN's own fallback, used only if the bridge cannot answer (see NonAddonBridge). */
    private static final int NONADDON_FALLBACK_COLOR = 16776427;

    @Inject(method = "getFluidStack", at = @At("RETURN"))
    private static void nonaddon$uniformLiquidColor(int amount, PotionContentsComponent potionContents,
            BottleType bottleType, CallbackInfoReturnable<FluidStack> cir) {
        try {
            Optional<RegistryKey<Potion>> key = potionContents.potion().flatMap(entry -> entry.getKey());
            if (key.isEmpty() || !NONADDON_LIQUID.equals(key.get().getValue())) {
                return;
            }
            FluidStack fluid = cir.getReturnValue();
            if (fluid == null || fluid.isEmpty()) {
                return;
            }
            fluid.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                    potionContents.potion(), Optional.of(nonaddon$fallbackColor()),
                    potionContents.customEffects(), potionContents.customName()));
        } catch (Throwable ignored) {
            // Never break Create's fluid handling for a cosmetic addon.
        }
    }

    private static int nonaddon$fallbackColor() {
        try {
            return NonAddonBridge.fallbackLiquidColorRgb();
        } catch (Throwable t) {
            return NONADDON_FALLBACK_COLOR;
        }
    }
}
