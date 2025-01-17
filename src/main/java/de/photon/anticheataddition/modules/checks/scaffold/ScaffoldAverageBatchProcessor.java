package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.user.data.batch.ScaffoldBatch;
import de.photon.anticheataddition.util.datastructure.batch.AsyncBatchProcessor;
import de.photon.anticheataddition.util.datastructure.batch.BatchPreprocessors;
import de.photon.anticheataddition.util.inventory.InventoryUtil;
import de.photon.anticheataddition.util.mathematics.Polynomial;
import de.photon.anticheataddition.util.violationlevels.Flag;
import lombok.val;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

final class ScaffoldAverageBatchProcessor extends AsyncBatchProcessor<ScaffoldBatch.ScaffoldBlockPlace>
{
    private static final Polynomial VL_CALCULATOR = new Polynomial(1.2222222, 20);

    public final double normalDelay = loadDouble(".parts.Average.delays.normal", 238);
    public final double sneakingAddition = loadDouble(".parts.Average.delays.sneaking_addition", 30);
    public final double sneakingSlowAddition = loadDouble(".parts.Average.delays.sneaking_slow_addition", 40);
    public final double diagonalDelay = loadDouble(".parts.Average.delays.diagonal", 138);
    private final int cancelVl = loadInt(".cancel_vl", 110);

    ScaffoldAverageBatchProcessor(ViolationModule module)
    {
        super(module, Set.of(ScaffoldBatch.SCAFFOLD_BATCH_EVENTBUS));
    }

    @Override
    public void processBatch(User user, List<ScaffoldBatch.ScaffoldBlockPlace> batch)
    {
        final boolean moonwalk = batch.stream().filter(Predicate.not(ScaffoldBatch.ScaffoldBlockPlace::sneaked)).count() >= batch.size() / 2;
        val delays = BatchPreprocessors.zipReduceToDoubleStatistics(batch,
                                                                    (old, cur) -> old.speedModifier() * cur.timeOffset(old),
                                                                    (old, cur) -> calculateMinExpectedDelay(old, cur, moonwalk));

        final double actualAverage = delays.get(0).getAverage();
        final double minExpectedAverage = delays.get(1).getAverage();

        // delta-times are too low -> flag
        if (actualAverage < minExpectedAverage) {
            final int vlIncrease = Math.min(130, VL_CALCULATOR.apply(minExpectedAverage - actualAverage).intValue());
            this.getModule().getManagement().flag(Flag.of(user)
                                                      .setAddedVl(vlIncrease)
                                                      .setCancelAction(cancelVl, () -> {
                                                          user.getTimeMap().at(TimeKey.SCAFFOLD_TIMEOUT).update();
                                                          InventoryUtil.syncUpdateInventory(user.getPlayer());
                                                      })
                                                      .setDebug(() -> "Scaffold-Debug | Player: %s enforced delay: %f | real: %f | vl increase: %d".formatted(user.getPlayer().getName(), minExpectedAverage, actualAverage, vlIncrease)));
        }
    }

    private double calculateMinExpectedDelay(ScaffoldBatch.ScaffoldBlockPlace old, ScaffoldBatch.ScaffoldBlockPlace current, boolean moonwalk)
    {
        if (current.blockFace() == old.blockFace() || current.blockFace() == old.blockFace().getOppositeFace()) {
            return !moonwalk && current.sneaked() && old.sneaked() ?
                   // Sneaking handling
                   normalDelay + sneakingAddition + (sneakingSlowAddition * Math.abs(Math.cos(2D * current.location().getYaw()))) :
                   // Moonwalking.
                   normalDelay;
            // Not the same blockfaces means that something is built diagonally or a new build position which means higher actual delay anyway and can be ignored.
        } else return diagonalDelay;
    }
}
