package me.marin.statsplugin.io;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.util.ExceptionUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class OldRecordBopperRunnable implements Runnable {

    @Override
    public void run() {
        try {
            Julti.log(Level.INFO, "Clearing old SpeedrunIGT records...");
            Files.list(Paths.get(StatsPluginSettings.getInstance().recordsPath)).forEach(path -> {
                try {
                    if (path.toString().endsWith(".json")) {
                        Files.deleteIfExists(path);
                    }
                } catch (IOException e) {
                    Julti.log(Level.ERROR, "Failed to delete old records:\n" + ExceptionUtil.toDetailedString(e));
                }
            });
            Julti.log(Level.INFO, "Cleared all SpeedrunIGT records.");
        } catch (Exception e) {
            Julti.log(Level.ERROR, "Failed to delete old records:\n" + ExceptionUtil.toDetailedString(e));
        }
    }

}
