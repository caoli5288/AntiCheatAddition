package de.photon.anticheataddition.protocol.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.protocol.EntityMetadataIndex;

import java.util.List;
import java.util.Optional;

public abstract class MetadataPacket extends AbstractPacket
{
    /**
     * Constructs a new strongly typed wrapper for the given packet.
     *
     * @param handle - handle to the raw packet data.
     * @param type   - the packet type.
     */
    protected MetadataPacket(PacketContainer handle, PacketType type)
    {
        super(handle, type);
    }

    public abstract List<WrappedWatchableObject> getRawMetadata();

    /**
     * Searches for an index in the metadata as it is unsorted.
     * An index is not necessarily found, even if it is defined in entity metadata.
     *
     * @return the {@link WrappedWatchableObject} wrapped in an {@link Optional} or {@link Optional#empty()} if the index was not found.
     */
    public Optional<WrappedWatchableObject> getMetadataIndex(final int index)
    {
        for (WrappedWatchableObject watch : getRawMetadata()) {
            if (index == watch.getIndex()) return Optional.of(watch);
        }
        return Optional.empty();
    }

    public static MetadataBuilder builder()
    {
        return new MetadataBuilder();
    }

    public static class MetadataBuilder
    {
        final WrappedDataWatcher dataWatcher = new WrappedDataWatcher();

        // ------------------------------------------------- Entity ------------------------------------------------- //

        /**
         * Sets a metadata.
         */
        public MetadataBuilder setMetadata(final int index, final Class<?> classOfValue, final Object value)
        {
            if (ServerVersion.is18()) dataWatcher.setObject(index, value);
            else dataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(index, WrappedDataWatcher.Registry.get(classOfValue)), value);
            return this;
        }

        /**
         * Sets the zero index metadata, which can be used for several settings.
         *
         * @see <a href="https://wiki.vg/Entity_metadata#Entity">https://wiki.vg/Entity_metadata#Entity</a>
         */
        public MetadataBuilder setZeroIndex(final byte zeroByte)
        {
            return this.setMetadata(0, Byte.class, zeroByte);
        }


        // ------------------------------------------------- Living ------------------------------------------------- //

        /**
         * Sets the health metadata.
         *
         * @see <a href="https://wiki.vg/Entity_metadata#Living">https://wiki.vg/Entity_metadata#Living</a>
         */
        public MetadataBuilder setHealthMetadata(final float health)
        {
            return this.setMetadata(EntityMetadataIndex.HEALTH, Float.class, health);
        }

        /**
         * Sets the arrows in entity metadata.
         *
         * @see <a href="https://wiki.vg/Entity_metadata#Living">https://wiki.vg/Entity_metadata#Living</a>
         */
        public MetadataBuilder setArrowInEntityMetadata(final int arrows)
        {
            return ServerVersion.is18() ?
                   // IN 1.8.8 THIS IS A BYTE, NOT AN INTEGER!
                   this.setMetadata(EntityMetadataIndex.ARROWS_IN_ENTITY, Byte.class, (byte) arrows) :
                   this.setMetadata(EntityMetadataIndex.ARROWS_IN_ENTITY, Integer.class, arrows);
        }

        // ------------------------------------------------- Player ------------------------------------------------- //

        /**
         * Sets the skin part metadata
         *
         * @see <a href="https://wiki.vg/Entity_metadata#Player">https://wiki.vg/Entity_metadata#Player</a>
         */
        public MetadataBuilder setSkinMetadata(final byte skinParts)
        {
            return this.setMetadata(EntityMetadataIndex.SKIN_PARTS, Byte.class, skinParts);
        }

        /**
         * Get the metadata in form of a {@link WrappedDataWatcher}
         */
        public WrappedDataWatcher asWatcher()
        {
            return this.dataWatcher;
        }

        /**
         * Get the metadata in form of a {@link List} of {@link WrappedWatchableObject}s.
         */
        public List<WrappedWatchableObject> asList()
        {
            return this.dataWatcher.getWatchableObjects();
        }
    }
}
