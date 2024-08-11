package me.marin.statsplugin.io;

import com.sun.jna.platform.win32.WinDef;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.management.ActiveWindowManager;
import xyz.duncanruns.julti.util.ExceptionUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Reads Atum's rsg-attempts.txt for reset counts, and then checks wpstateout.txt in the same instance.
 * If the state is "wall", it calculates wall/break time.
 * <p>
 * This data is then used by {@link RecordsFolderWatcher}.
 */
public class RSGAttemptsWatcher extends FileWatcher {

    private final static String RSG_ATTEMPTS = "rsg-attempts.txt";
    private final Path wpStateoutPath;

    public RSGAttemptsWatcher(Path atumDirectory, Path wpStateoutPath) {
        super(atumDirectory.toFile());
        this.wpStateoutPath = wpStateoutPath;

        Julti.log(Level.DEBUG, "rsg-attempts.txt watcher is running...");
        previousAtumResets = getAtumResets();
    }

    private String read(Path path) {
        int attempts = 0;
        while (true) {
            try {
                byte[] text = Files.readAllBytes(path);
                if (text.length > 0) {
                    return new String(text, StandardCharsets.UTF_8);
                }
                Thread.sleep(10);
                if (attempts++ > 50) {
                    throw new IOException("file is still empty after 50 attempts.");
                }
            } catch (IOException | InterruptedException e) {
                Julti.log(Level.DEBUG, "Could not read " + path.getFileName() + ":\n" + ExceptionUtil.toDetailedString(e));
            }
        }
    }

    private String getWpStateout() {
        return read(wpStateoutPath);
    }

    private long getAtumResets() {
        try {
            Path path = Paths.get(file.toPath().toString(), RSG_ATTEMPTS);
            return Long.parseLong(read(path));
        } catch (NumberFormatException e) {
            Julti.log(Level.ERROR, "Could not parse number in rsg-attempts.txt:\n" + ExceptionUtil.toDetailedString(e));
            return -1;
        }
    }

    private int wallResetsSincePrev = 0;
    private long breakRTASincePrev = 0;
    private long wallTimeSincePrev = 0;

    private long lastActionMillis = 0;

    private long previousAtumResets = 0;

    /**
     * Important: This method is called after each world is created, and considering that
     *  SeedQueue could be creating worlds in the background for seconds after
     *  players reset a world, *wall time and break time can not be fully accurate*.
     */
    @Override
    protected void handleFileUpdated(File file) {
        if (!StatsPluginSettings.getInstance().trackerEnabled) {
            return;
        }
        if (!file.getName().equals(RSG_ATTEMPTS)) {
            return;
        }
        String state = getWpStateout();
        boolean isWallActive = "wall".equals(state) && ActiveWindowManager.isMinecraftActive();

        long atumResets = getAtumResets();
        if (atumResets < 0 || atumResets == previousAtumResets) {
            return;
        }
        Julti.log(Level.DEBUG, "Resets: " + atumResets + ", state: " + state);

        if (isWallActive) {
            long delta = atumResets - previousAtumResets;
            wallResetsSincePrev += (int) delta;
            Julti.log(Level.DEBUG, "Wall resets +" + delta + " (" + wallResetsSincePrev + " total).");
        }
        previousAtumResets = atumResets;

        long now = System.currentTimeMillis();

        // Calculate wall breaks
        if (lastActionMillis > 0) {
            long delta = now - lastActionMillis;
            if (isWallActive) {
                if (delta > StatsPluginSettings.getInstance().breakThreshold * 1000L) {
                    breakRTASincePrev += delta;
                    Julti.log(Level.DEBUG, "Break RTA +" + delta + "ms (" + breakRTASincePrev + "ms total).");
                } else {
                    wallTimeSincePrev += delta;
                    Julti.log(Level.DEBUG, "Wall time since prev. +" + delta + "ms (" + wallTimeSincePrev + "ms total).");
                }
            }
        }
        this.lastActionMillis = now;
    }

    @Override
    protected void handleFileCreated(File file) {
        // ignored
    }

    public int getWallResetsSincePrev() {
        return wallResetsSincePrev;
    }

    public long getBreakRTASincePrev() {
        return breakRTASincePrev;
    }

    public long getWallTimeSincePrev() {
        return wallTimeSincePrev;
    }

    public void reset() {
        wallResetsSincePrev = 0;
        breakRTASincePrev = 0;
        wallTimeSincePrev = 0;
    }

}
