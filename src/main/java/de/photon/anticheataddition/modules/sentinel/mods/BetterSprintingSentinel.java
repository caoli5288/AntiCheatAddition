package de.photon.anticheataddition.modules.sentinel.mods;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.sentinel.SentinelModule;
import de.photon.anticheataddition.util.pluginmessage.MessageChannel;
import io.netty.buffer.Unpooled;
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

// API_DOCS of BetterSprinting:
/*
 * OUTGOING PACKETS
 * =================
 *
 *** [byte 0] [bool <enableSurvivalFlyBoost>] [bool <enableAllDirs>]
 ***
 *** Notifies the player about which non-vanilla settings are enabled on the server (both are disabled by default).
 *** Sent to player when their [byte 0] message is processed, and either or both settings are enabled.
 *** Sent to all players with the mod after using the '/bettersprinting setting (...)' command.
 *
 *
 *** [byte 1]
 ***
 *** Disables basic functionality of the mod on client side.
 *** Sent to player when their [byte 0] message is processed, and the server wants to disable the mod.
 *** Sent to all players with the mod after using the '/bettersprinting disablemod true' command.
 *
 *
 *** [byte 2]
 ***
 *** Re-enables basic functionality of the mod on client side.
 *** Sent to all players with the mod after using the '/bettersprinting disablemod false' command.
 */
public final class BetterSprintingSentinel extends SentinelModule implements PluginMessageListener
{
    public static final BetterSprintingSentinel INSTANCE = new BetterSprintingSentinel();

    private final byte[] settingsBufArray;
    private final byte[] disableBufArray;

    private BetterSprintingSentinel()
    {
        super("BetterSprinting");

        val settingsBuffer = Unpooled.buffer();
        settingsBuffer.writeByte(0);

        settingsBuffer.writeBoolean(!loadBoolean(".disable.survival_fly_boost", true));
        settingsBuffer.writeBoolean(!loadBoolean(".disable.enable_all_dirs", true));

        this.settingsBufArray = settingsBuffer.array();
        settingsBuffer.release();

        val disableBuffer = Unpooled.buffer();
        // Bypassed players are already filtered out.
        // The mod provides a method to disable it
        val disableGeneral = loadBoolean(".disable.general", false);
        disableBuffer.writeByte(disableGeneral ? 1 : 2);
        this.disableBufArray = disableBuffer.array();
        disableBuffer.release();
    }

    @Override
    public void onPluginMessageReceived(@NotNull final String channel, @NotNull final Player player, final byte[] message)
    {
        detection(player);

        val sendChannel = "BSprint".equals(channel) ? "BSM" : MessageChannel.BETTER_SPRINTING_CHANNEL.getChannel().orElseThrow();

        player.sendPluginMessage(AntiCheatAddition.getInstance(), sendChannel, this.settingsBufArray);
        player.sendPluginMessage(AntiCheatAddition.getInstance(), sendChannel, this.disableBufArray);
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .addIncomingMessageChannel(MessageChannel.BETTER_SPRINTING_CHANNEL)
                           .addIncomingMessageChannel(MessageChannel.ofLegacy("BSprint"))
                           // No message is sent in BSprint.
                           .addOutgoingMessageChannel(MessageChannel.BETTER_SPRINTING_CHANNEL)
                           .build();
    }
}
