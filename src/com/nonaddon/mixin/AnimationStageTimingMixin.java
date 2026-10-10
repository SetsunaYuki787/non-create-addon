package com.nonaddon.mixin;

import com.nonaddon.NoNAddon;

import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shortens the total run time of <em>looping</em> animation stages.
 *
 * <p>Speed alone cannot do that. NoN recomputes every stage's duration each tick from the live
 * instance speed, and for a looping stage that duration is a fixed real time budget: the authored
 * cycle is divided by the speed and then repeated until the budget is used up
 * ({@code cycles = max(1, round(targetTicks / (cycleTicks / speed)))}), so a faster animation just
 * fits more cycles into the same number of ticks. Non-looping stages do scale with speed
 * ({@code ceil(authoredTicks / speed)}), which is why only the loops look unaffected.
 *
 * <p>The budget itself comes from {@code AnimationStageTiming.resolveLoopTargetTicks}, and that
 * value is exactly what this mixin scales down before NoN quantises it into whole cycles. Doing it
 * there - instead of scaling the finished duration - keeps NoN's own arithmetic in charge, so the
 * shortened stage still ends on a cycle boundary instead of in the middle of one.
 *
 * <p>Injecting into the private target resolution rather than into the public duration resolver
 * also means non-looping stages are never touched twice: they do not go through this method at all.
 * The boost table itself lives in {@link NoNAddon}, which fills it from the public
 * {@code NonAnimationEvents} and empties it again when the instance stops.
 */
@Mixin(targets = "com.nonid.internal.animation.server.AnimationStageTiming")
public class AnimationStageTimingMixin {
    @Inject(method = "resolveLoopTargetTicks", at = @At("RETURN"), cancellable = true)
    private static void nonaddon$shortenBoostedLoop(Identifier animationId, Identifier stageId,
            int normalStageSeconds, CallbackInfoReturnable<Long> cir) {
        try {
            Long original = cir.getReturnValue();
            if (original == null) {
                return;
            }
            long shortened = NoNAddon.shortenedLoopTarget(original, animationId);
            if (shortened != original.longValue()) {
                cir.setReturnValue(shortened);
            }
        } catch (Throwable ignored) {
            // Never break NoN's stage timing for a cosmetic addon.
        }
    }
}
