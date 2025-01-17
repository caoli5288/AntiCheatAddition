package de.photon.anticheataddition.util.config;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.val;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class ConfigurationRepresentation
{
    @NotNull private final File configFile;
    @Getter(lazy = true) private final YamlConfiguration yamlConfiguration = loadYaml();
    private final List<ConfigChange> requestedChanges = new ArrayList<>();

    public ConfigurationRepresentation(@NotNull File configFile)
    {
        this.configFile = Preconditions.checkNotNull(configFile, "Tried to create ConfigurationRepresentation of null config file.");
    }

    private static void deleteLines(final List<String> lines, int startPosition, int lineCount)
    {
        while (lineCount-- > 0) lines.remove(startPosition);
    }

    private static int linesOfKey(final List<String> lines, int firstLineOfKey)
    {
        final long depthOfKey = ConfigUtil.depth(lines.get(firstLineOfKey));
        return (int) lines.stream()
                          // Skip firstLineOfKey to get to the first line and 1 as the initial line should not be iterated over to avoid stopping there in takeWhile.
                          .skip(firstLineOfKey + 1L)
                          // Smaller or equal depth is the indicator of a new key, and we only want the lines of the current key.
                          .takeWhile(line -> ConfigUtil.depth(line) > depthOfKey)
                          // + 1 as the first line is always there.
                          .count() + 1;
    }

    private YamlConfiguration loadYaml()
    {
        return YamlConfiguration.loadConfiguration(this.configFile);
    }

    public synchronized void requestValueChange(final String path, final Object value)
    {
        this.requestedChanges.add(new ConfigChange(path, value));
    }

    public synchronized void save() throws IOException
    {
        // Directly inject changes.
        if (requestedChanges.isEmpty()) return;

        // Load the whole config.
        val configLines = new ArrayList<>(Files.readAllLines(this.configFile.toPath()));

        for (ConfigChange requestedChange : requestedChanges) {
            val value = requestedChange.value();

            final int lineIndexOfKey = requestedChange.lineIndexOfPath(configLines);
            final String originalLine = configLines.get(lineIndexOfKey);
            final int affectedLines = linesOfKey(configLines, lineIndexOfKey);

            // We want to delete all lines after the initial one.
            deleteLines(configLines, lineIndexOfKey + 1, affectedLines - 1);

            // +1 to add the otherwise removed ':' right back.
            val replacementLine = new StringBuilder(originalLine.substring(0, originalLine.lastIndexOf(':') + 1));

            // Set the new value.
            if (value instanceof Boolean || value instanceof Number) replacementLine.append(' ').append(value);
            else if (value instanceof String str) replacementLine.append(' ').append('\"').append(str).append('\"');
            else if (value instanceof List<?> list) {
                if (list.isEmpty()) replacementLine.append(" []");
                else {
                    val preString = "- ".indent((int) ConfigUtil.depth(originalLine));
                    for (Object o : list) configLines.add(lineIndexOfKey + 1, preString + o);
                }
            }

            configLines.set(lineIndexOfKey, replacementLine.toString());
        }

        Files.write(this.configFile.toPath(), configLines);
    }

    private record ConfigChange(String path, Object value)
    {
        /**
         * Searches for the first config line the path points to.
         */
        public int lineIndexOfPath(@NotNull List<String> lines)
        {
            val pathParts = path.trim().split("\\.");
            int partIndex = 0;

            int lineIndex = 0;
            long partDepth = 0;
            long lineDepth;

            String trimmed;
            for (String line : lines) {
                lineDepth = ConfigUtil.depth(line);

                // The sub-part we search for does not exist.
                if (partDepth > lineDepth) throw new IllegalArgumentException("Path " + path + " could not be found.");

                trimmed = line.strip();
                // New "deeper" subpart found?
                if (!ConfigUtil.isConfigComment(trimmed) && trimmed.startsWith(pathParts[partIndex])) {
                    partDepth = lineDepth;
                    // Whole path found.
                    if (++partIndex == pathParts.length) return lineIndex;
                }
                ++lineIndex;
            }
            throw new IllegalArgumentException("Path " + path + " could not be found (full iteration).");
        }
    }
}
