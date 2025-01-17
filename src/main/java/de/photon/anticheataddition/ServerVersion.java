package de.photon.anticheataddition;

import de.photon.anticheataddition.exception.UnknownMinecraftException;
import de.photon.anticheataddition.protocol.ProtocolVersion;
import de.photon.anticheataddition.util.datastructure.SetUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;

@RequiredArgsConstructor
@Getter
public enum ServerVersion
{
    // As we compare the versions these MUST be sorted.

    MC18("1.8.8", true),
    MC19("1.9", false),
    MC110("1.10", false),
    MC111("1.11.2", false),
    MC112("1.12.2", true),
    MC113("1.13", false),
    MC114("1.14", false),
    MC115("1.15.2", true),
    MC116("1.16.5", true),
    MC117("1.17.1", true),
    MC118("1.18", true),
    MC119("1.19", true);


    public static final Set<ServerVersion> ALL_SUPPORTED_VERSIONS = MC18.getSupVersionsFrom();
    public static final Set<ServerVersion> LEGACY_PLUGIN_MESSAGE_VERSIONS = MC112.getSupVersionsTo();
    public static final Set<ServerVersion> LEGACY_EVENT_VERSIONS = MC113.getSupVersionsTo();
    public static final Set<ServerVersion> NON_188_VERSIONS = MC19.getSupVersionsFrom();

    /**
     * The server version of the currently running {@link Bukkit} instance.
     */
    @NotNull
    public static final ServerVersion ACTIVE = Arrays.stream(ServerVersion.values())
                                                     .filter(serverVersion -> Bukkit.getVersion().contains(serverVersion.getVersionOutputString()))
                                                     .findFirst()
                                                     .orElseThrow(UnknownMinecraftException::new);

    private final String versionOutputString;
    private final boolean supported;

    // Lazy getting as most versions are not supported or used.
    // Also, this is important to avoid loading errors (as the generate methods access values() when not fully loaded)
    @Getter(lazy = true) private final Set<ServerVersion> supVersionsTo = getSupportedVersions(version -> this.compareTo(version) >= 0);
    @Getter(lazy = true) private final Set<ServerVersion> supVersionsFrom = getSupportedVersions(version -> this.compareTo(version) <= 0);

    /**
     * Shorthand for activeServerVersion == MC18.
     * Checks if the current ServerVersion is minecraft 1.8.8.
     * This method reduces both code and improves the maintainability as activeServerVersion is only used by those statements that might need changes for a new version.
     */
    public static boolean is18()
    {
        return ACTIVE == MC18;
    }

    /**
     * Used to get the client version. Might only differ from {@link #ACTIVE} if ViaVersion is installed.
     */
    @NotNull
    public static ServerVersion getClientServerVersion(final Player player)
    {
        if (player == null) return ACTIVE;
        val viaAPI = AntiCheatAddition.getInstance().getViaAPI();
        if (viaAPI == null) return ACTIVE;

        val clientVersion = ProtocolVersion.getByVersionNumber(viaAPI.getPlayerVersion(player.getUniqueId()));
        return clientVersion == null ? ACTIVE : clientVersion.getEquivalentServerVersion();
    }

    /**
     * Used to check whether the current server version is included in a set of supported server versions.
     *
     * @param supportedServerVersions the {@link Set} of supported server versions of the module
     *
     * @return true if the active server version is included in the provided {@link Set} or false if it is not.
     */
    public static boolean containsActive(Set<ServerVersion> supportedServerVersions)
    {
        return supportedServerVersions.contains(ACTIVE);
    }

    private static Set<ServerVersion> getSupportedVersions(Predicate<ServerVersion> filter)
    {
        return Arrays.stream(values())
                     .filter(ServerVersion::isSupported)
                     .filter(filter)
                     .collect(SetUtil.toImmutableEnumSet());
    }
}
