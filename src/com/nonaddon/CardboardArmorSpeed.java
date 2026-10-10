package com.nonaddon;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Feature 4: Create's cardboard armor grants one animation speed level per worn piece.
 *
 * <p>Every worn piece of {@code create:cardboard_helmet}, {@code create:cardboard_chestplate},
 * {@code create:cardboard_leggings} and {@code create:cardboard_boots} adds one level, capped at
 * {@link NoNAddon#MAX_SPEED_LEVEL}, which is the animation speed state's own ceiling. The level is
 * applied through the very same {@link NoNAddon#ANIMATION_SPEED} effect the animation hooks read,
 * so the playback code does not have to know anything about armor: wearing 3 pieces means the
 * player carries level 3 and NoN animations play at 2.5x.
 *
 * <p>A server tick keeps a short lived instance alive while the armor is worn, so the effect is
 * gone on the next tick after the last piece comes off. Only instances this class handed out are
 * ever removed ({@link #GRANTED}), so an effect the player got from anywhere else - a command, a
 * potion, another mod - is left untouched, and an outside effect that is at least as strong is not
 * overwritten either.
 */
public final class CardboardArmorSpeed {
    /** Namespace of the armor set. */
    private static final String ARMOR_NAMESPACE = "create";

    /** Item paths that count, one per armor slot. */
    private static final Set<String> PIECES = Set.of(
            "cardboard_helmet", "cardboard_chestplate", "cardboard_leggings", "cardboard_boots");

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /**
     * Lifetime of every granted instance, in ticks. It is refreshed while the armor is worn, so it
     * never runs out and never flickers either: at {@link #REFRESH_AT} ticks left it is written
     * again.
     */
    private static final int GRANT_TICKS = 20;

    /** Remaining time at which the instance is refreshed. */
    private static final int REFRESH_AT = 10;

    /**
     * Amplifier this class last wrote per player. It is what makes the removal safe: an instance is
     * only taken back when it looks like the one we handed out.
     */
    private static final Map<UUID, Integer> GRANTED = new ConcurrentHashMap<>();

    private CardboardArmorSpeed() {
    }

    /** Registers the per tick armor check; called once from the mod initializer. */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(CardboardArmorSpeed::tick);
    }

    private static void tick(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            try {
                update(player);
            } catch (Throwable ignored) {
                // Armor must never be able to break a server tick.
            }
        }
    }

    /**
     * Brings one player's animation speed effect in line with the cardboard armor they wear.
     *
     * @return the level the worn armor asks for, capped at {@link NoNAddon#MAX_SPEED_LEVEL}; 0 when
     *         no piece is worn
     */
    public static int update(ServerPlayerEntity player) {
        if (player == null) {
            return 0;
        }
        RegistryEntry<StatusEffect> entry = Registries.STATUS_EFFECT.getEntry(NoNAddon.ANIMATION_SPEED);
        if (entry == null) {
            return 0;
        }

        UUID uuid = player.getUuid();
        int level = Math.min(countPieces(player), NoNAddon.MAX_SPEED_LEVEL);
        StatusEffectInstance existing = player.getStatusEffect(entry);
        Integer granted = GRANTED.get(uuid);

        if (level <= 0) {
            // No piece is worn: take back our own grant, and only our own grant.
            GRANTED.remove(uuid);
            if (isOurs(existing, granted)) {
                player.removeStatusEffect(entry);
            }
            return 0;
        }

        int amplifier = level - 1;
        if (existing != null && !isOurs(existing, granted) && existing.getAmplifier() >= amplifier) {
            // Somebody else handed out an effect that is at least as strong: leave it alone and stop
            // managing this player until the armor asks for more than that.
            GRANTED.remove(uuid);
            return level;
        }

        if (existing == null || existing.getAmplifier() != amplifier || existing.getDuration() <= REFRESH_AT) {
            if (existing != null) {
                player.removeStatusEffect(entry);
            }
            player.addStatusEffect(new StatusEffectInstance(entry, GRANT_TICKS, amplifier, false, true, true));
        }
        GRANTED.put(uuid, amplifier);
        return level;
    }

    /**
     * True when the running instance is the short lived one this class keeps alive: same amplifier
     * and a duration that only our refresh can explain.
     */
    private static boolean isOurs(StatusEffectInstance instance, Integer granted) {
        return instance != null
                && granted != null
                && instance.getAmplifier() == granted
                && instance.getDuration() <= GRANT_TICKS;
    }

    /** How many of Create's cardboard armor pieces the player wears, 0 to 4. */
    private static int countPieces(ServerPlayerEntity player) {
        int count = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getEquippedStack(slot);
            if (stack.isEmpty()) {
                continue;
            }
            Identifier itemId = Registries.ITEM.getId(stack.getItem());
            if (ARMOR_NAMESPACE.equals(itemId.getNamespace()) && PIECES.contains(itemId.getPath())) {
                count++;
            }
        }
        return count;
    }
}
