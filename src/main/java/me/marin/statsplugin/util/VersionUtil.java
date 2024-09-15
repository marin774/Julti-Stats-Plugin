package me.marin.statsplugin.util;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.plugin.PluginManager;
import xyz.duncanruns.julti.util.ExceptionUtil;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VersionUtil {

    public static final Version CURRENT_VERSION = new Version(0, 5, 7);

    public static Version version(String version) {
        String[] parts = version.split("\\.");
        if (parts.length == 2) {
            return new Version(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } else {
            return new Version(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        }
    }

    public static void deleteOldVersionJars() {
        PluginManager pluginManager = PluginManager.getPluginManager();
        List<Pair<Path, PluginManager.JultiPluginData>> plugins = Collections.emptyList();
        try {
            plugins = pluginManager.getFolderPlugins();
        } catch (IOException e) {
            Julti.log(Level.ERROR, "Failed to load plugins from folder:\n" + ExceptionUtil.toDetailedString(e));
        }

        // Mod ID -> path and data
        Map<String, Pair<Path, PluginManager.JultiPluginData>> bestPluginVersions = new HashMap<>();

        plugins.stream().forEach(pair -> {
            PluginManager.JultiPluginData data = pair.getRight();
            if (!data.id.equals("julti-stats-plugin")) {
                return;
            }

            if (bestPluginVersions.containsKey(data.id)) {
                if (xyz.duncanruns.julti.util.VersionUtil.tryCompare(data.version.split("\\+")[0], bestPluginVersions.get(data.id).getRight().version.split("\\+")[0], 0) > 0) {
                    Pair<Path, PluginManager.JultiPluginData> oldPlugin = bestPluginVersions.get(data.id);
                    deletePluginJar(oldPlugin.getLeft());
                    bestPluginVersions.put(data.id, pair);
                } else {
                    deletePluginJar(pair.getLeft());
                }
            } else {
                bestPluginVersions.put(data.id, pair);
            }
        });
    }

    private static void deletePluginJar(Path path) {
        try {
            Files.delete(path);
        } catch (Exception e) {
            Julti.log(Level.ERROR, "Failed to delete " + path.getFileName() + " plugin:\n" + ExceptionUtil.toDetailedString(e));
        }
    }

    public static class Version implements Comparable<Version> {
        public final int major;
        public final int minor;
        public final int patch;

        public Version(int major, int minor) {
            this(major, minor, 0);
        }
        public Version(int major, int minor, Integer patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }

        @Override
        public String toString() {
            return major + "." + minor + "." + patch;
        }

        public boolean isOlderThan(Version version) {
            return this.compareTo(version) < 0;
        }

        @Override
        public int compareTo(@Nullable Version o) {
            if (o == null) return 1;
            if (o == this) return 0;
            if (this.major - o.major != 0) return this.major - o.major;
            if (this.minor - o.minor != 0) return this.minor - o.minor;

            return this.patch - o.patch;
        }
    }

}
