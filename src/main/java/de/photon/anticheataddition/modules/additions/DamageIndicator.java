package de.photon.anticheataddition.modules.additions;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.protocol.EntityMetadataIndex;
import de.photon.anticheataddition.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.protocol.packetwrappers.MetadataPacket;
import de.photon.anticheataddition.protocol.packetwrappers.sentbyserver.WrapperPlayServerEntityMetadata;
import de.photon.anticheataddition.protocol.packetwrappers.sentbyserver.WrapperPlayServerNamedEntitySpawn;
import de.photon.anticheataddition.util.minecraft.entity.EntityUtil;
import lombok.val;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;

import java.util.Set;

public final class DamageIndicator extends Module
{
    public static final DamageIndicator INSTANCE = new DamageIndicator();

    private final boolean spoofAnimals = loadBoolean(".spoof.animals", false);
    private final boolean spoofMonsters = loadBoolean(".spoof.monsters", true);
    private final boolean spoofPlayers = loadBoolean(".spoof.players", true);

    private DamageIndicator()
    {
        super("DamageIndicator");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        val packetTypes = ServerVersion.is18() ?
                          // Only register NAMED_ENTITY_SPAWN on 1.8 as it doesn't work on newer versions.
                          Set.of(PacketType.Play.Server.ENTITY_METADATA, PacketType.Play.Server.NAMED_ENTITY_SPAWN) :
                          Set.of(PacketType.Play.Server.ENTITY_METADATA);

        return ModuleLoader.of(this, PacketAdapterBuilder
                .of(this, packetTypes)
                .priority(ListenerPriority.HIGH)
                .onSending((event, user) -> {
                    val entity = event.getPacket().getEntityModifier(event.getPlayer().getWorld()).read(0);
                    // Clientside entities will be null in the world's entity list.
                    if (entity == null) return;

                    val entityType = entity.getType();

                    // Should spoof?
                    // Entity has health to begin with.
                    if (entityType.isAlive() &&
                        // Bossbar problems
                        // Cannot use Boss interface as that doesn't exist on 1.8.8
                        entityType != EntityType.ENDER_DRAGON &&
                        entityType != EntityType.WITHER &&

                        // Entity must be living to have health; all categories extend LivingEntity.
                        ((entityType == EntityType.PLAYER && spoofPlayers) ||
                         (entity instanceof Monster && spoofMonsters) ||
                         (entity instanceof Animals && spoofAnimals)) &&

                        // Not the player himself.
                        // Offline mode servers have name-based UUIDs, so that should be no problem.
                        event.getPlayer().getEntityId() != entity.getEntityId() &&

                        // Entity has no passengers.
                        EntityUtil.INSTANCE.getPassengers(entity).isEmpty())
                    {
                        // Clone the packet to prevent a serversided connection of the health.
                        event.setPacket(event.getPacket().deepClone());

                        final MetadataPacket read;

                        if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) read = new WrapperPlayServerEntityMetadata(event.getPacket());
                        else if (event.getPacketType() == PacketType.Play.Server.NAMED_ENTITY_SPAWN) read = new WrapperPlayServerNamedEntitySpawn(event.getPacket());
                        else throw new IllegalStateException("Unregistered packet type.");

                        read.getMetadataIndex(EntityMetadataIndex.HEALTH).ifPresent(health -> {
                            // Only set it if the entity is not yet dead to prevent problems on the clientside.
                            // Set the health to 1.0F as that is the default value.
                            if (((Float) health.getValue() > 0.0F)) health.setValue(1.0F);
                        });
                    }
                }).build());
    }
}