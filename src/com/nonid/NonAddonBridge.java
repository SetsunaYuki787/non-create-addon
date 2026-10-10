package com.nonid;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

/**
 * Bridge into NoN's package-private item helpers.
 *
 * <p>{@code com.nonid.NonItemSystem} is a package-private final class, so a mixin living in
 * {@code com.nonaddon.mixin} cannot call it: the JVM resolves the owner class of the call site
 * against the caller's runtime package and throws
 * {@code IllegalAccessError: failed to access class com.nonid.NonItemSystem}. Declaring this class
 * in NoN's own package makes the two share a runtime package (same name, same knot class loader),
 * which is what grants access.
 *
 * <p>This forwards to the exact helpers NoN itself uses when a real player takes a bottle, so the
 * produced stack (tint, food components, tooltip, entity type id, name) is identical instead of
 * being a re-implementation.
 */
public final class NonAddonBridge {
    private NonAddonBridge() {
    }

    public static boolean consumeOneGlassBottle(ServerPlayerEntity player, Hand hand) {
        return NonItemSystem.consumeOneGlassBottle(player, hand);
    }

    public static ItemStack createLiquidBottleStack(Identifier entityTypeId) {
        return NonItemSystem.createLiquidBottleStack(entityTypeId);
    }

    public static void giveItemOrDrop(ServerPlayerEntity player, ItemStack stack) {
        NonItemSystem.giveItemOrDrop(player, stack);
    }

    /**
     * The colour NoN gives a liquid whose donor entity has no colour of its own: the entity fallback
     * {@code #FFFCEB} (16776427), i.e. exactly what {@code NonLiquidSystem.resolveEntityLiquidColorRgb}
     * returns when neither the config nor the data pack knows the entity.
     *
     * <p>NoN always resolves a bottle's tint through {@code NonLiquidSystem.resolveBottleTintRgb} with
     * the entity type id, so asking for a deliberately unknown id is the documented-adjacent way to
     * read that fallback without duplicating the constant here. Used by
     * {@code com.nonaddon.mixin.PotionFluidMixin} to give every drained semen fluid one colour.
     */
    public static int fallbackLiquidColorRgb() {
        return NonLiquidSystem.resolveBottleTintRgb(Identifier.of("nonaddon", "__no_such_entity__"));
    }
}
