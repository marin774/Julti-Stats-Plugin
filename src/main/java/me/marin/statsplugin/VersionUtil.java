package me.marin.statsplugin;

import javax.annotation.Nullable;

public class VersionUtil {

    public static final Version CURRENT_VERSION = new Version(0, 5, 3);

    public static Version version(String version) {
        String[] parts = version.split("\\.");
        if (parts.length == 2) {
            return new Version(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } else {
            return new Version(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
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
