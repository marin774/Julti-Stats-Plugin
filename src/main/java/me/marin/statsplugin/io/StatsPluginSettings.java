package me.marin.statsplugin.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.util.ExceptionUtil;
import xyz.duncanruns.julti.util.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static me.marin.statsplugin.StatsPlugin.STATS_SETTINGS_PATH;

/**
 * Code template stolen from @draconix6's custom wall plugin
 */
public class StatsPluginSettings {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path DEFAULT_RECORDS_FOLDER_PATH = Path.of(System.getProperty("user.home")).resolve("speedrunigt").resolve("records");

    private static StatsPluginSettings instance = null;

    @SerializedName("tracker enabled")
    public boolean trackerEnabled = true;

    @SerializedName("sheet link")
    public String sheetLink = null;

    @SerializedName("records path")
    public String recordsPath = null;

    @SerializedName("MultiMC directory")
    public String multiMCDirectory = null; // NOT SUPPORTED

    @SerializedName("break threshold")
    public int breakThreshold = 30;

    @SerializedName("use sheets")
    public boolean useSheets = true;

    @SerializedName("detect RSG")
    public boolean detectRSG = true;

    @SerializedName("delete-old-records")
    public boolean deleteOldRecords = false; // NOT SUPPORTED

    @SerializedName("track seed")
    public boolean trackSeed = false; // NOT SUPPORTED

    @SerializedName("multi instance")
    public boolean multiInstance = true;

    public static StatsPluginSettings getInstance() {
        return instance;
    }

    public static void load() {
        if (!Files.exists(STATS_SETTINGS_PATH)) {
            instance = new StatsPluginSettings();
            instance.recordsPath = DEFAULT_RECORDS_FOLDER_PATH.toString();
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


    @Override
    public String toString() {
        return "StatsPluginSettings{" +
                "sheetLink='" + sheetLink + '\'' +
                ", recordsPath='" + recordsPath + '\'' +
                ", multiMCDirectory='" + multiMCDirectory + '\'' +
                ", breakThreshold=" + breakThreshold +
                ", useSheets=" + useSheets +
                ", detectRSG=" + detectRSG +
                ", deleteOldRecords=" + deleteOldRecords +
                ", trackSeed=" + trackSeed +
                ", multiInstance=" + multiInstance +
                '}';
    }
}
