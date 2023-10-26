package me.marin.statsplugin.io;

import me.marin.statsplugin.StatsPlugin;

import java.io.File;

/**
 * The reason why this is used instead of refreshing Stats GUI internally is because the user can edit & save the file,
 * and the table should reflect that change.
 */
public class StatsFileWatcherRunnable extends FileWatcher {

    public StatsFileWatcherRunnable(File file) {
        super(file);
    }

    @Override
    protected void handleFileUpdated(File file) {
        /*if (StatsPlugin.statsGUI != null && !StatsPlugin.statsGUI.isClosed()) {
            StatsPlugin.statsGUI.reload();
        }*/

    }

    @Override
    protected void handleFileCreated(File file) {
        // ignored
    }

}
