package de.photon.anticheataddition.util.violationlevels;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.events.SentinelEvent;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.util.violationlevels.threshold.ThresholdManagement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DetectionManagement extends ViolationManagement implements Listener
{
    private final Set<UUID> detectionSet = ConcurrentHashMap.newKeySet(AntiCheatAddition.SERVER_EXPECTED_PLAYERS);

    public DetectionManagement(ViolationModule module)
    {
        super(module, ThresholdManagement.loadCommands(module.getConfigString() + ".commands"));
        AntiCheatAddition.getInstance().registerListener(this);
    }

    @Override
    public void flag(@NotNull Flag flag)
    {
        Preconditions.checkNotNull(flag.getPlayer(), "Tried to flag null player.");
        Preconditions.checkArgument(flag.getAddedVl() == 1, "Tried to add more than 1 vl in detection management.");

        if (!SentinelEvent.build(flag.getPlayer(), this.module.getModuleId()).call().isCancelled()) {
            this.addVL(flag.getPlayer(), flag.getAddedVl());
            // No execution of the runnables of flag.
        }
    }

    @Override
    public int getVL(@NotNull UUID uuid)
    {
        return detectionSet.contains(uuid) ? 1 : 0;
    }

    @Override
    public void setVL(@NotNull Player player, int newVl)
    {
        switch (newVl) {
            case 0:
                this.detectionSet.remove(player.getUniqueId());
                break;
            case 1:
                // Only punish if the vl actually changes (no prior detection).
                if (detectionSet.add(player.getUniqueId())) this.punishPlayer(player, 0, 1);
                break;
            default: throw new IllegalArgumentException("A Sentinel detection management only supports the vls 0 (no detection) and 1 (detection).");
        }

        postVlUpdate(player);
    }

    @Override
    protected void addVL(@NotNull Player player, int vl)
    {
        // If vl is 0, just return as no change is supposed to happen (would otherwise set the vl to 0).
        if (vl != 0) this.setVL(player, vl);
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event)
    {
        this.detectionSet.remove(event.getPlayer().getUniqueId());
    }
}
