package me.marin.statsplugin;

import com.google.common.io.Resources;
import me.marin.statsplugin.gui.OBSOverlayGUI;
import me.marin.statsplugin.gui.StatsGUI;
import me.marin.statsplugin.io.OldRecordBopperRunnable;
import me.marin.statsplugin.io.RecordsFolderWatcher;
import me.marin.statsplugin.io.StatsFileIO;
import me.marin.statsplugin.io.StatsPluginSettings;
import me.marin.statsplugin.stats.Session;
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
import java.time.Duration;
import java.time.Instant;

public class StatsPlugin implements PluginInitializer {

    public static final String VERSION = "0.3.2";

    public static final Path STATS_FOLDER_PATH = JultiOptions.getJultiDir().resolve("stats-plugin");
    public static final Path GOOGLE_SHEETS_CREDENTIALS_PATH = STATS_FOLDER_PATH.resolve("credentials.json");
    public static final Path STATS_SETTINGS_PATH = STATS_FOLDER_PATH.resolve("settings.json");
    public static final Path OBS_OVERLAY_TEMPLATE_PATH = STATS_FOLDER_PATH.resolve("obs-overlay-template");
    public static final Path OBS_OVERLAY_PATH = STATS_FOLDER_PATH.resolve("obs-overlay.txt");

    public static Session CURRENT_SESSION = new Session();

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
            Julti.log(Level.INFO, "Running StatsPlugin v" + VERSION + "!");

            STATS_FOLDER_PATH.toFile().mkdirs();

            OBSOverlayGUI.createDefaultFile();

            Session previousSession = StatsFileIO.getInstance().getLatestSession();
            if (!previousSession.isEmpty()) {
                String dateTime = previousSession.getLatestRecord().dateTime();

                long timeSince = Math.abs(Duration.between(Instant.now(), StatsPluginUtil.dateTimeToInstant(dateTime)).toMillis());
                Julti.log(Level.DEBUG, "Last record in previous session: " + dateTime + "(" + timeSince + "ms ago)");

                if (timeSince < 1000 * 60 * 60 * 3) {
                    // less than 3 hours since previous session, merge
                    CURRENT_SESSION = previousSession;
                    Julti.log(Level.INFO, "Continuing calculating stats from previous session!");
                }
            }

            CURRENT_SESSION.updateOverlay();

            StatsPluginSettings.load();
            StatsPlugin.reloadGoogleSheets();

            StatsPluginUtil.runAsync("records-folder-watcher", new RecordsFolderWatcher(Paths.get(StatsPluginSettings.getInstance().recordsPath)));
            if (StatsPluginSettings.getInstance().deleteOldRecords) {
                StatsPluginUtil.runAsync("old-record-bopper", new OldRecordBopperRunnable());
            }
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
