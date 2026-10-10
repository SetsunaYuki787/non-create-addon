package com.nonaddon;

import com.nonid.api.animation.NonAnimationApi;
import com.nonid.api.animation.NonAnimationEvents;
import com.nonid.entity.HorseLiquidCollectorEntity;
import com.nonid.internal.animation.server.ServerAnimationController;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NoN Addon.
 *
 * <p>Feature 1 is data driven: {@code data/non-create-addon/recipe/andesite_alloy.json} turns
 * 2 iron ingots + 2 liquid bottles into 9 andesite alloy.
 *
 * <p>Feature 2 lives here: NoN publishes {@link NonAnimationEvents#STOPPED} once an animation
 * instance ends, so the horse liquid collector does not need a mixin. Every collector that took
 * part in the finished animation gets one redstone pulse in the 3x3 area at its own Y level.
 *
 * <p>Feature 3 is the animation speed state: {@link #ANIMATION_SPEED} is a level 1..3 player
 * effect, and {@link NonAnimationApi#multiplySpeed} is NoN's public hook for changing playback
 * speed. {@link NonAnimationEvents#STARTED} covers the first stage. Every later stage resets the
 * instance speed back to the value its animdef declares, so
 * {@link NonAnimationEvents#STAGE_CHANGED} has to multiply again - NoN fires that event after the
 * reset, which is why the boost survives stage changes instead of being overwritten.
 *
 * <p>Speed shortens the stages that have an authored length, but a looping stage is timed by a
 * budget rather than by its cycle, so a faster loop only cycles more often. {@link
 * #registerLoopDurationBoost} therefore also records the level per animation, and
 * {@code AnimationStageTimingMixin} divides the loop budget NoN is about to use.
 *
 * <p>Feature 4 hands that same effect out for armor: {@link CardboardArmorSpeed} gives one
 * animation speed level per worn piece of Create's cardboard set, so a player can carry level
 * 1..3 by dressing instead of by drinking or by a command.
 *
 * <p>Feature 5 is the andesite plug: {@link AndesitePlug} registers the wearable and {@link
 * PlugGoggles} teaches Trinkets about it and makes wearing it count as wearing Create's engineer
 * goggles. Trinkets is optional - the item exists and crafts without it, only the goggles hook
 * needs it.
 */
public final class NoNAddon implements ModInitializer {
    public static final String MOD_ID = "non-create-addon";

    /** Length of the emitted redstone pulse, in game ticks. */
    public static final int PULSE_TICKS = 2;

    /** Highest animation speed level. Amplifiers above this one are capped to it. */
    public static final int MAX_SPEED_LEVEL = 3;

    /** Playback speed added per level: level 1 -> 1.5x, level 2 -> 2.0x, level 3 -> 2.5x. */
    public static final double SPEED_PER_LEVEL = 0.5;

    /** The player state that speeds NoN animations up. */
    public static final StatusEffect ANIMATION_SPEED = new AnimationSpeedEffect();

    /**
     * Boost level per running animation id, filled from the animation events and read by
     * {@code AnimationStageTimingMixin}. A looping stage is shortened by the level's multiplier, so
     * that a boosted animation really is over sooner instead of only cycling faster.
     *
     * <p>Not an {@code instanceId} table on purpose: the hook that has to see it is NoN's stage
     * timing, which is reached with the animation id and cannot tell which instance asked. Two
     * simultaneous instances of the same animation therefore share one entry - the alternative
     * would be to reproduce NoN's timing rules from the outside.
     */
    private static final Map<Identifier, Integer> LOOP_DURATION_BOOSTS = new ConcurrentHashMap<>();

    /** Invisible, non solid block that only exists to push redstone power into its neighbours. */
    public static final RegistryKey<Block> REDSTONE_PULSE_KEY =
            RegistryKey.of(RegistryKeys.BLOCK, id("redstone_pulse"));

    /**
     * 1.21.11 requires a registry key on {@link AbstractBlock.Settings}: the {@code AbstractBlock}
     * constructor calls {@code Settings.getLootTableKey()} / {@code getTranslationKey()}, and both
     * throw {@code NullPointerException("Block id not set")} when it is missing. An explicitly empty
     * loot table keeps the block drop free without needing a loot table file.
     */
    public static final Block REDSTONE_PULSE = new RedstonePulseBlock(
            AbstractBlock.Settings.create()
                    .registryKey(REDSTONE_PULSE_KEY)
                    .lootTable(Optional.empty())
                    .noCollision()
                    .breakInstantly()
                    .dropsNothing()
                    .replaceable());

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        Registry.register(Registries.BLOCK, REDSTONE_PULSE_KEY, REDSTONE_PULSE);
        Registry.register(Registries.STATUS_EFFECT, id("animation_speed"), ANIMATION_SPEED);
        NonAnimationEvents.STOPPED.register(NoNAddon::onAnimationStopped);
        NonAnimationEvents.STARTED.register(NoNAddon::onAnimationStarted);
        NonAnimationEvents.STAGE_CHANGED.register(NoNAddon::onAnimationStageChanged);
        CardboardArmorSpeed.register();
        AndesitePlug.register();
        if (FabricLoader.getInstance().isModLoaded("trinkets")) {
            try {
                PlugGoggles.register();
            } catch (Throwable ignored) {
                // Trinkets present but unusable: the plug stays a craftable item without a slot.
            }
        }
    }

    private static void onAnimationStopped(NonAnimationEvents.StoppedContext context) {
        clearLoopDurationBoost(context.animationId());
        ServerWorld world = context.world();
        if (world == null) {
            return;
        }
        for (UUID uuid : context.actorUuids()) {
            Entity entity = world.getEntity(uuid);
            if (entity instanceof HorseLiquidCollectorEntity collector) {
                emitPulse(world, collector.getBlockPos());
            }
        }
    }

    /**
     * Fills every still empty block of the 3x3 square around {@code center} with a short lived
     * pulse block. Occupied positions are skipped, so existing redstone dust, repeaters or any
     * other build is never overwritten - those components still receive the pulse because the
     * freshly placed pulse blocks are adjacent to them.
     */
    private static void emitPulse(ServerWorld world, BlockPos center) {
        BlockState pulse = REDSTONE_PULSE.getDefaultState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = center.add(dx, 0, dz);
                BlockState existing = world.getBlockState(pos);
                if (existing.isOf(REDSTONE_PULSE) || !existing.isAir()) {
                    continue;
                }
                world.setBlockState(pos, pulse, Block.NOTIFY_ALL);
            }
        }
    }

    /**
     * Playback multiplier for a boost level: 0 means "not boosted" and gives 1.0, levels 1 / 2 / 3
     * give 1.5x / 2.0x / 2.5x. Levels above {@link #MAX_SPEED_LEVEL} are capped, so an amplifier
     * of 5 still plays at 2.5x.
     */
    public static double speedMultiplierForLevel(int level) {
        int capped = Math.min(Math.max(level, 0), MAX_SPEED_LEVEL);
        return capped == 0 ? 1.0 : 1.0 + SPEED_PER_LEVEL * capped;
    }

    /**
     * Highest boost level carried by the player actors of an animation, or 0 when none of them is a
     * player under the influence of {@link #ANIMATION_SPEED}. Only players who actually take part
     * in the animation count, so a player merely triggering a mob-only animation does not change
     * that animation's speed.
     *
     * <p>The amplifier is the level: amplifier 0 is level 1.
     */
    public static int animationSpeedLevel(ServerWorld world, List<UUID> actorUuids) {
        if (world == null || actorUuids == null || actorUuids.isEmpty()) {
            return 0;
        }
        int best = 0;
        for (UUID uuid : actorUuids) {
            if (world.getEntity(uuid) instanceof ServerPlayerEntity player) {
                best = Math.max(best, actorSpeedLevel(player));
            }
        }
        return best;
    }

    /**
     * Boost level of a single animation actor: 0 for anything that is not a player or is not under
     * the influence of {@link #ANIMATION_SPEED}, otherwise the amplifier + 1. Null safe.
     *
     * <p>This takes the entity directly instead of a UUID because an actor does not have to be part
     * of the world's entity table for this lookup to be meaningful.
     */
    public static int actorSpeedLevel(Entity actor) {
        if (!(actor instanceof ServerPlayerEntity player)) {
            return 0;
        }
        RegistryEntry<StatusEffect> entry = Registries.STATUS_EFFECT.getEntry(ANIMATION_SPEED);
        if (entry == null) {
            return 0;
        }
        StatusEffectInstance instance = player.getStatusEffect(entry);
        return instance == null ? 0 : instance.getAmplifier() + 1;
    }

    /**
     * Records that a running animation's looping stages should be shortened by {@code level}. Called
     * by the animation listeners; level 0 removes the entry, which is also what
     * {@link #clearLoopDurationBoost} does when the instance ends.
     */
    public static void registerLoopDurationBoost(Identifier animationId, int level) {
        Identifier key = boostKey(animationId);
        if (key == null) {
            return;
        }
        if (level <= 0) {
            LOOP_DURATION_BOOSTS.remove(key);
        } else {
            LOOP_DURATION_BOOSTS.put(key, level);
        }
    }

    /** Forgets an animation's duration boost, so later runs of it play at their authored length. */
    public static void clearLoopDurationBoost(Identifier animationId) {
        Identifier key = boostKey(animationId);
        if (key != null) {
            LOOP_DURATION_BOOSTS.remove(key);
        }
    }

    /**
     * Divides a looping stage's target length by the boost multiplier, or returns it unchanged when
     * the animation is not boosted (which includes every check that happens outside an animation
     * and on the client, where the table stays empty).
     *
     * <p>Called from {@code AnimationStageTimingMixin} with the value NoN is about to quantise into
     * whole cycles. A target of {@code -1} means "this loop never ends by itself" and is left alone.
     */
    public static long shortenedLoopTarget(long targetTicks, Identifier animationId) {
        if (targetTicks < 0L) {
            return targetTicks;
        }
        Integer level = animationId == null ? null : LOOP_DURATION_BOOSTS.get(boostKey(animationId));
        if (level == null || level <= 0) {
            return targetTicks;
        }
        double multiplier = speedMultiplierForLevel(level);
        return multiplier <= 1.0 ? targetTicks : Math.max(1L, Math.round((double) targetTicks / multiplier));
    }

    /**
     * Table key of an animation: the base animation id, with a {@code .p<number>} stage suffix
     * stripped the same way NoN's own {@code AnimationIds.baseId} does it. Callers pass either the
     * base id (the server advances stages with it) or a stage id, and both have to hit the same row.
     */
    private static Identifier boostKey(Identifier animationId) {
        if (animationId == null) {
            return null;
        }
        String path = animationId.getPath();
        int marker = path.lastIndexOf(".p");
        if (marker <= 0 || marker + 2 >= path.length()) {
            return animationId;
        }
        String suffix = path.substring(marker + 2);
        for (int i = 0; i < suffix.length(); i++) {
            char c = suffix.charAt(i);
            if (c < '0' || c > '9') {
                return animationId;
            }
        }
        return Identifier.of(animationId.getNamespace(), path.substring(0, marker));
    }

    private static void onAnimationStarted(NonAnimationEvents.StartedContext context) {
        ServerWorld world = context.world();
        if (world == null) {
            return;
        }
        int level = animationSpeedLevel(world, context.session().actorUuids());
        applyBoost(world, context.session().instanceId(), context.session().animationId(), level);
    }

    private static void onAnimationStageChanged(NonAnimationEvents.StageChangedContext context) {
        int level = animationSpeedLevel(context.world(), context.actorUuids());
        applyBoost(context.world(), context.instanceId(), context.animationId(), level);
    }

    /**
     * Applies (or clears) the whole boost for one live instance.
     *
     * <p>The order inside is load bearing. NoN's {@code adjustSpeed} recomputes and broadcasts the
     * stage end tick in the same call, and that recomputation reads the looping budget through the
     * boosted table - so the table has to be written before the speed is multiplied. Otherwise the
     * packet the HUD progress bar follows still carries the un-shortened tick, and the stage flips
     * at roughly 1/multiplier of the bar's width.
     *
     * <p>{@link #republishStageEndTick} covers the one path where {@code adjustSpeed} sends nothing:
     * when the authored stage speed already sits on the [0.1, 4.0] ceiling, multiplying cannot change
     * the clamped speed and NoN returns early.
     */
    private static void applyBoost(ServerWorld world, UUID instanceId, Identifier animationId, int level) {
        registerLoopDurationBoost(animationId, level);
        double speedBefore = instanceSpeed(world, instanceId);
        applySpeedBoost(world, instanceId, level);
        if (level > 0 && speedBefore >= 0.0 && instanceSpeed(world, instanceId) == speedBefore) {
            republishStageEndTick(world, instanceId);
        }
    }

    /** Current speed of a live instance, or -1 when NoN has no such instance in this world. */
    private static double instanceSpeed(ServerWorld world, UUID instanceId) {
        ServerAnimationController.ActiveInstanceSnapshot snapshot = instanceSnapshot(world, instanceId);
        return snapshot == null ? -1.0 : snapshot.speed();
    }

    /**
     * Re-broadcasts the stage end tick NoN currently publishes for that instance. The snapshot
     * resolves it from the live instance, so it already folds in the boosted table and the current
     * speed - it is exactly the value the next stage advance will use, which keeps the progress bar
     * in step even when NoN itself never sent a speed update.
     */
    private static void republishStageEndTick(ServerWorld world, UUID instanceId) {
        ServerAnimationController.ActiveInstanceSnapshot snapshot = instanceSnapshot(world, instanceId);
        if (snapshot == null) {
            return;
        }
        long duration = snapshot.stageEndTick() - snapshot.stageStartTick();
        if (duration <= 0L) {
            return;
        }
        try {
            ServerAnimationController.setCurrentStageProgressDuration(world, instanceId, duration);
        } catch (Throwable ignored) {
            // NoN internals moved; the animation keeps running unmodified.
        }
    }

    private static ServerAnimationController.ActiveInstanceSnapshot instanceSnapshot(ServerWorld world, UUID instanceId) {
        if (world == null || instanceId == null) {
            return null;
        }
        try {
            return ServerAnimationController.getActiveInstanceSnapshot(world, instanceId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Multiplies the instance speed by the actor's level. Both call sites sit on the server thread
     * right after NoN has (re)set the instance speed, so multiplying is exact and never compounds:
     * NoN's own stage advance assigns the new stage speed before it fires
     * {@link NonAnimationEvents#STAGE_CHANGED}.
     */
    private static void applySpeedBoost(ServerWorld world, UUID instanceId, int level) {
        if (world == null || instanceId == null || level <= 0) {
            return;
        }
        NonAnimationApi.multiplySpeed(world, instanceId, speedMultiplierForLevel(level));
    }
}
