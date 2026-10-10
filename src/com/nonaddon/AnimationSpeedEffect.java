package com.nonaddon;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * The player "state" that speeds up NoN animations.
 *
 * <p>The amplifier is the boost level, so
 * {@code /effect give <player> non-create-addon:animation_speed <seconds> <0|1|2>} gives level 1 / 2 / 3,
 * i.e. 1.5x / 2x / 2.5x playback. Higher amplifiers are capped at level 3
 * (see {@link NoNAddon#speedMultiplierForLevel(int)}), which is why the effect never needs to be
 * granted with an amplifier above 2.
 *
 * <p>The effect does not tick: the boost is read once when an animation starts and again after
 * every stage change, never per tick.
 */
public final class AnimationSpeedEffect extends StatusEffect {
    /** Amber, so the HUD icon reads as "faster" next to vanilla's Speed. */
    public static final int COLOR = 0xFFC24A;

    public AnimationSpeedEffect() {
        super(StatusEffectCategory.BENEFICIAL, COLOR);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return false;
    }
}
