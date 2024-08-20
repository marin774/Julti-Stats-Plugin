package me.marin.statsplugin;

import com.google.common.io.Resources;
import me.marin.statsplugin.gui.OBSOverlayGUI;
import me.marin.statsplugin.gui.StatsGUI;
import me.marin.statsplugin.io.*;
import me.marin.statsplugin.stats.Session;
import me.marin.statsplugin.util.GoogleSheets;
import me.marin.statsplugin.util.StatsPluginUtil;
import me.marin.statsplugin.util.UpdateUtil;
import me.marin.statsplugin.util.VersionUtil;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiAppLaunch;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.plugin.PluginEvents;
import xyz.duncanruns.julti.plugin.PluginInitializer;
import xyz.duncanruns.julti.plugin.PluginManager;
import xyz.duncanruns.julti.util.ExceptionUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;

import static me.marin.statsplugin.util.VersionUtil.CURRENT_VERSION;
import static me.marin.statsplugin.util.VersionUtil.version;

public class StatsPlugin implements PluginInitializer {

    public static final Path STATS_FOLDER_PATH = JultiOptions.getJultiDir().resolve("stats-plugin");
    public static final Path PLUGINS_PATH = JultiOptions.getJultiDir().resolve("plugins");
    public static final Path GOOGLE_SHEETS_CREDENTIALS_PATH = STATS_FOLDER_PATH.resolve("credentials.json");
    public static final Path STATS_SETTINGS_PATH = STATS_FOLDER_PATH.resolve("settings.json");
    public static final Path OBS_OVERLAY_TEMPLATE_PATH = STATS_FOLDER_PATH.resolve("obs-overlay-template");
    public static final Path OBS_OVERLAY_PATH = STATS_FOLDER_PATH.resolve("obs-overlay.txt");

    public static Session CURRENT_SESSION = new Session();

    public static StatsGUI statsGUI;
    public static GoogleSheets googleSheets;

    private static final int THREE_HOURS_MS = 1000 * 60 * 60 * 3;

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
            Julti.log(Level.INFO, "Running StatsPlugin v" + CURRENT_VERSION + "!");

            STATS_FOLDER_PATH.toFile().mkdirs();

            OBSOverlayGUI.createDefaultFile();

            Session previousSession = StatsFileIO.getInstance().getLatestSession();
            if (!previousSession.isEmpty()) {
                String dateTime = previousSession.getLatestRecord().dateTime();

                long timeSince = Math.abs(Duration.between(Instant.now(), StatsPluginUtil.dateTimeToInstant(dateTime)).toMillis());
                Julti.log(Level.DEBUG, "Last record in previous session: " + dateTime + "(" + timeSince + "ms ago)");

                if (timeSince < THREE_HOURS_MS) {
                    // less than 3 hours since previous session, merge
                    CURRENT_SESSION = previousSession;
                    Julti.log(Level.INFO, "Continuing calculating stats from previous session!");
                }
            }

            CURRENT_SESSION.updateOverlay();

            StatsPluginSettings.load();
            VersionUtil.Version version = getVersionFromSettings();
            if (version.isOlderThan(CURRENT_VERSION)) {
                updateFrom(version);
            }

            StatsPlugin.reloadGoogleSheets();

            if (StatsPluginSettings.getInstance().deleteOldRecords) {
                StatsPluginUtil.runAsync("old-record-bopper", new OldRecordBopperRunnable());
            }

            Path recordsPath;
            try {
                recordsPath = Paths.get(StatsPluginSettings.getInstance().recordsPath);
            } catch (Exception e) {
                Julti.log(Level.ERROR, "Invalid SpeedrunIGT records folder in settings, change it manually settings.json and restart Julti!\n"+ ExceptionUtil.toDetailedString(e));
                return;
            }
            StatsPluginUtil.runAsync("records-folder-watcher", new RecordsFolderWatcher(recordsPath));
            StatsPluginUtil.runTimerAsync(new InstanceManagerRunnable(), 1000);

            VersionUtil.deleteOldVersionJars();
            UpdateUtil.checkForUpdatesAndUpdate(true);
        });
    }

    private static VersionUtil.Version getVersionFromSettings() {
        String versionString = StatsPluginSettings.getInstance().version;
        if (versionString == null) {
            versionString = "0.3.2"; // This is hardcoded because versioning was added after 0.3.2
        }
        return VersionUtil.version(versionString);
    }

    public static void updateFrom(VersionUtil.Version version) {
        Julti.log(Level.INFO, "Updating data from version " + version + ".");
        if (version.isOlderThan(version("0.4.0"))) {
            StatsPluginSettings.getInstance().completedSetup = true;
            Julti.log(Level.INFO, "[0.4.0] 'completed setup' set to true.");
            if (StatsPluginSettings.getInstance().breakThreshold == 30) {
                StatsPluginSettings.getInstance().breakThreshold = 5;
                Julti.log(Level.INFO, "[0.4.0] 'break threshold' set to 5s (from default 30s).");
            }
        }

        StatsPluginSettings.getInstance().version = CURRENT_VERSION.toString();
        StatsPluginSettings.save();
        Julti.log(Level.INFO, "Updated all data to v" + CURRENT_VERSION + "!");
    }

    public static boolean reloadGoogleSheets() {
        if (StatsPluginSettings.getInstance().useSheets && Files.exists(GOOGLE_SHEETS_CREDENTIALS_PATH) && StatsPluginSettings.getInstance().sheetLink != null) {
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
