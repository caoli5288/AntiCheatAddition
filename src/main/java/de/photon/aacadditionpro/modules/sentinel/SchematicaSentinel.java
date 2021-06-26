package de.photon.aacadditionpro.modules.sentinel;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.pluginmessage.MessageChannel;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.val;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SchematicaSentinel extends SentinelModule
{
    private static final MessageChannel SCHEMATICA_CHANNEL = MessageChannel.ofLegacy("schematica");

    private final ByteBuf sentMessage;

    public SchematicaSentinel()
    {
        super("Schematica");
        val byteBuf = Unpooled.buffer();
        byteBuf.writeByte(0);

        /*
         * This array holds what features of schematica should be disabled.
         * SENDING A 1 MEANS ALLOWING THE FEATURE -> NEGATION.
         * Link to the original plugin: https://www.spigotmc.org/resources/schematicaplugin.14411/
         */
        byteBuf.writeBoolean(!AACAdditionPro.getInstance().getConfig().getBoolean(this.getConfigString() + ".disable.printer"));
        byteBuf.writeBoolean(!AACAdditionPro.getInstance().getConfig().getBoolean(this.getConfigString() + ".disable.saveToFile"));
        byteBuf.writeBoolean(!AACAdditionPro.getInstance().getConfig().getBoolean(this.getConfigString() + ".disable.load"));

        this.sentMessage = Unpooled.unmodifiableBuffer(byteBuf);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message)
    {
        if (new String(message).contains("schematica")) {
            val user = User.getUser(player);
            if (User.isUserInvalid(user, this)) return;

            detection(player);
            user.getPlayer().sendPluginMessage(AACAdditionPro.getInstance(), SCHEMATICA_CHANNEL.getChannel(), sentMessage.array());
        }
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .addIncomingMessageChannels(MessageChannel.of("minecraft", "register", "REGISTER"))
                           .addOutgoingMessageChannels(SCHEMATICA_CHANNEL)
                           .build();
    }
}