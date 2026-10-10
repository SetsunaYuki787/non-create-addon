package com.nonaddon;

import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.player.PlayerEntity;

import java.lang.reflect.Method;
import java.util.function.Predicate;

/**
 * Feature 5, the behaviour half: makes the andesite plug wearable in the {@code legs/v} and {@code
 * legs/a} slots and makes wearing it count as wearing Create's engineer goggles.
 *
 * <p>This class must stay separate from {@link AndesitePlug} and is only touched when Trinkets is
 * present: it links against Trinkets types, so loading it without the mod would fail with a {@code
 * NoClassDefFoundError} and take the item down with it.
 *
 * <p>Create is reached reflectively on purpose. {@code GogglesItem.addIsWearingPredicate} is a
 * public extension point, but referencing it directly would put {@code com.zurrtum.create} into our
 * bytecode and drag Create into the remap classpath, where it conflicts with the mixing-free remap
 * this project uses. Reflection keeps that coupling at runtime only, and a missing Create simply
 * means the plug stops granting goggles.
 */
final class PlugGoggles {
    /** Engineer goggles, resolved reflectively so no Create type ends up in our bytecode. */
    private static final String GOGGLES_ITEM = "com.zurrtum.create.content.equipment.goggles.GogglesItem";

    /** The plug behaves like every other trinket: no ticking, no modifiers, no equip sound of its own. */
    private static final Trinket PLUG_TRINKET = new Trinket() {
    };

    private PlugGoggles() {
    }

    static void register() {
        TrinketsApi.registerTrinket(AndesitePlug.ITEM, PLUG_TRINKET);
        registerGogglesPredicate();
    }

    /** True while the player has the plug in any trinket slot, whichever slot it ended up in. */
    static boolean isWearingPlug(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        return TrinketsApi.getTrinketComponent(player)
                .map(component -> component.isEquipped(AndesitePlug.ITEM))
                .orElse(false);
    }

    private static void registerGogglesPredicate() {
        try {
            Class<?> goggles = Class.forName(GOGGLES_ITEM);
            Method add = goggles.getMethod("addIsWearingPredicate", Predicate.class);
            add.invoke(null, (Predicate<PlayerEntity>) PlugGoggles::isWearingPlug);
        } catch (Throwable ignored) {
            // Create missing or moved: the plug is still wearable, it just does not grant goggles.
        }
    }
}
