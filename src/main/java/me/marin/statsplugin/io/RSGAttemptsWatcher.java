package me.marin.statsplugin.io;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.util.ExceptionUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Reads rsg-attempts.txt for reset counts, and then checks wpstateout.txt in the same instance.
 * If the state is "wall", it calculates wall/break time.
 * <p>
 * This data is then used by {@link RecordsFolderWatcher}.
 */
public class RSGAttemptsWatcher extends FileWatcher {

    public RSGAttemptsWatcher(Path path) {
        super(path.toFile());
        Julti.log(Level.DEBUG, "rsg-attempts.txt watcher is running...");
        previousAtumResets = getAtumResets();
    }

    private String getWpStateout() {
        try {
            Path path = Paths.get(file.getParentFile().getParentFile().getParentFile().toPath().toString(), "wpstateout.txt");
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "null";
        }
    }

    private long getAtumResets() {
        try {
            return Long.parseLong(new String(Files.readAllBytes(Paths.get(file.toPath().toString(), "rsg-attempts.txt")), StandardCharsets.UTF_8));
        } catch (IOException | NumberFormatException e) {
            Julti.log(Level.ERROR, "Could not read atum rsg-attempts.txt:\n" + ExceptionUtil.toDetailedString(e));
            return -1;
        }
    }

    private int wallResetsSincePrev = 0;
    private long breakRTASincePrev = 0;
    private long wallTimeSincePrev = 0;

    private long lastActionMillis = 0;

    private long previousAtumResets = 0;

    @Override
    protected void handleFileUpdated(File file) {
        if (!file.getName().equals("rsg-attempts.txt")) {
            return;
        }
        String state = getWpStateout();
        boolean isWallActive = state.equals("wall");

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
                    Julti.log(Level.DEBUG, "Walltime since prev +" + delta + "ms (" + wallTimeSincePrev + "ms total).");
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
