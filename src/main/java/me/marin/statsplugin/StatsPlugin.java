package me.marin.statsplugin;

import com.google.common.io.Resources;
import me.marin.statsplugin.gui.OBSOverlayGUI;
import me.marin.statsplugin.gui.StatsGUI;
import me.marin.statsplugin.io.OldRecordBopperRunnable;
import me.marin.statsplugin.io.RecordsFolderWatcher;
import me.marin.statsplugin.io.StatsPluginSettings;
import me.marin.statsplugin.stats.CurrentSession;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiAppLaunch;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.plugin.PluginEvents;
import xyz.duncanruns.julti.plugin.PluginInitializer;
import xyz.duncanruns.julti.plugin.PluginManager;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StatsPlugin implements PluginInitializer {

    public static final String VERSION = "0.2";

    public static final Path STATS_FOLDER_PATH = JultiOptions.getJultiDir().resolve("stats-plugin");
    public static final Path GOOGLE_SHEETS_CREDENTIALS_PATH = STATS_FOLDER_PATH.resolve("credentials.json");
    public static final Path STATS_SETTINGS_PATH = STATS_FOLDER_PATH.resolve("settings.json");
    public static final Path OBS_OVERLAY_TEMPLATE_PATH = STATS_FOLDER_PATH.resolve("obs-overlay-template");
    public static final Path OBS_OVERLAY_PATH = STATS_FOLDER_PATH.resolve("obs-overlay.txt");

    public static final CurrentSession CURRENT_SESSION = new CurrentSession();

    public static StatsGUI statsGUI;
    public static GoogleSheets googleSheets;

    /**
     * Used for dev testing only.
     */
    public static void main(String[] args) throws IOException {
        JultiAppLaunch.launchWithDevPlugin(args, PluginManager.JultiPluginData.fromString(
                Resources.toString(Resources.getResource(StatsPlugin.class, "/julti.plugin.json"), Charset.defaultCharset())
        ), new StatsPlugin());
    }

    @Override
    public void initialize() {
        PluginEvents.RunnableEventType.LAUNCH.register(() -> {
            STATS_FOLDER_PATH.toFile().mkdirs();
            OBSOverlayGUI.createDefaultFile();
            StatsPluginSettings.load();
            reloadGoogleSheets();
            new Thread(new RecordsFolderWatcher(Paths.get(StatsPluginSettings.getInstance().recordsPath)), "records-folder-watcher").start();
            if (StatsPluginSettings.getInstance().deleteOldRecords) {
                new Thread(new OldRecordBopperRunnable(), "old-record-bopper").start();
            }
            Julti.log(Level.INFO, "Running StatsPlugin v" + VERSION + "!");
        });
    }

    public static boolean reloadGoogleSheets() {
        if (StatsPluginSettings.getInstance().useSheets && Files.exists(GOOGLE_SHEETS_CREDENTIALS_PATH)) {
            googleSheets = new GoogleSheets(GOOGLE_SHEETS_CREDENTIALS_PATH);
            return googleSheets.connect();
        }
        return false;
    }

    @Override
    public String getMenuButtonName() {
        return "Open Config";
    }

    @Override
    public void onMenuButtonPress() {
        if (statsGUI == null || statsGUI.isClosed()) {
            statsGUI = new StatsGUI();
        } else {
            statsGUI.requestFocus();
        }
    }

}
