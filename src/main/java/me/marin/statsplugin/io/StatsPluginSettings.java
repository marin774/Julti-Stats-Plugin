package me.marin.statsplugin.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import me.marin.statsplugin.util.VersionUtil;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.util.ExceptionUtil;
import xyz.duncanruns.julti.util.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static me.marin.statsplugin.StatsPlugin.STATS_SETTINGS_PATH;

/**
 * Code template from draconix6's <a href="https://github.com/draconix6/Julti-CustomWall/blob/main/src/main/java/xyz/draconix6/customwallplugin/CustomWallOptions.java">Custom Wall plugin options</a>
 */
public class StatsPluginSettings {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path DEFAULT_RECORDS_FOLDER_PATH = Paths.get(System.getProperty("user.home")).resolve("speedrunigt").resolve("records");

    private static StatsPluginSettings instance = null;

    @SerializedName("tracker enabled")
    public boolean trackerEnabled = false;

    @SerializedName("sheet link")
    public String sheetLink = null;

    @SerializedName("records path")
    public String recordsPath = null;

    @SerializedName("break threshold")
    public int breakThreshold = 5;

    @SerializedName("use sheets")
    public boolean useSheets = false;

    @SerializedName(value = "delete old records", alternate = {"delete-old-records"})
    public boolean deleteOldRecords = false;

    @SerializedName("completed setup")
    public boolean completedSetup = false;

    @SerializedName("version")
    public String version;

    /*
    @SerializedName("simulate no internet")
    public boolean simulateNoInternet = false;*/

    public static StatsPluginSettings getInstance() {
        return instance;
    }

    public static void load() {
        if (!Files.exists(STATS_SETTINGS_PATH)) {
            instance = new StatsPluginSettings();
            instance.recordsPath = DEFAULT_RECORDS_FOLDER_PATH.toString();
            instance.version = VersionUtil.CURRENT_VERSION.toString();
            save();
        } else {
            String s;
            try {
                s = FileUtil.readString(STATS_SETTINGS_PATH);
            } catch (IOException e) {
                instance = new StatsPluginSettings();
                return;
            }
            instance = GSON.fromJson(s, StatsPluginSettings.class);
        }
    }

    public static void save() {
        try {
            FileUtil.writeString(STATS_SETTINGS_PATH, GSON.toJson(instance));
        } catch (IOException e) {
            Julti.log(Level.ERROR, "Failed to save Stats Settings: " + ExceptionUtil.toDetailedString(e));
        }
    }

}
