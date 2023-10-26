package me.marin.statsplugin.io;

import com.google.gson.JsonObject;
import me.marin.statsplugin.StatsPlugin;
import me.marin.statsplugin.StatsPluginUtil;
import me.marin.statsplugin.stats.StatsCSVRecord;
import xyz.duncanruns.julti.management.ActiveWindowManager;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TimeZone;

public class RecordsFolderWatcher extends FileWatcher {

    public RecordsFolderWatcher(Path path) {
        super(path.toFile());
    }

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("M/d/yyyy HH:mm:ss");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final String SESSION_MARKER = "$J" + StatsPlugin.VERSION;

    private int wallResetsSincePrev = 0;
    private int splitlessResets = 0;
    private long RTASincePrev = 0;
    private long breakRTASincePrev = 0;
    private long wallTimeSincePrev = 0;

    private String RTADistribution = "";

    private long lastActionMillis = 0;

    private boolean isFirstRun = true;

    /**
     * Record files get updated on world load start, completion and on reset.
     */
    @Override
    protected void handleFileUpdated(File file) {
        if (!StatsPluginSettings.getInstance().trackerEnabled) {
            return;
        }

        JsonObject recordJSON = StatsPluginUtil.readJSON(file);
        if (recordJSON == null || recordJSON.isJsonNull()) {
            return;
        }

        RecordParser recordParser = new RecordParser(file);
        if (!recordParser.validateRSG()) return;

        long now = System.currentTimeMillis();
        long finalRTA = recordJSON.get("final_rta").getAsLong();

        // Calculate wall breaks
        if (lastActionMillis > 0) {
            long delta = now - lastActionMillis - finalRTA;
            if (delta > 0 && ActiveWindowManager.isWallActive()) {
                if (delta > StatsPluginSettings.getInstance().breakThreshold) {
                    breakRTASincePrev += delta;
                } else {
                    wallTimeSincePrev += delta;
                }
            }
        }

        this.lastActionMillis = now;

        if (finalRTA == 0) {
            wallResetsSincePrev++;
            return;
        }

        RTADistribution += finalRTA/1000 + "$";

        if (!recordParser.hasDoneAnySplit()) {
            splitlessResets++;
            RTASincePrev += finalRTA;
            return;
        }

        String date = LocalDateTime.ofInstant(Instant.ofEpochMilli(recordParser.getDate()), ZoneId.systemDefault()).format(DATETIME_FORMATTER);
        Map<String, Long> timelines = recordParser.getTimelinesMap();

        StatsCSVRecord csvRecord = new StatsCSVRecord(
                date,
                recordParser.getIronSource(),
                recordParser.getEnterType(),
                recordParser.getSpawnBiome(),
                formatTime(recordParser.getRTA()),
                formatTime(recordParser.getWoodObtainedTime()),
                formatTime(recordParser.getPickaxeTime()),
                formatTime(timelines.get("enter_nether")),
                formatTime(timelines.get("enter_bastion")),
                formatTime(timelines.get("enter_fortress")),
                formatTime(timelines.get("nether_travel")),
                formatTime(timelines.get("enter_stronghold")),
                formatTime(timelines.get("enter_end")),
                formatTime(recordParser.getRTT()),
                formatTime(recordParser.getIGT()),
                String.valueOf(recordParser.getBlazeRodsPickedUp()),
                String.valueOf(recordParser.getBlazesKilled()),
                formatTime(recordParser.getIronObtainedTime()),
                String.valueOf(wallResetsSincePrev),
                String.valueOf(splitlessResets),
                formatTime(Math.max(0, RTASincePrev)),
                formatTime(breakRTASincePrev),
                formatTime(wallTimeSincePrev),
                isFirstRun ? SESSION_MARKER : "",
                RTADistribution
        );

        StatsFileIO.getInstance().writeStats(csvRecord);
        if (StatsPluginSettings.getInstance().useSheets && StatsPlugin.googleSheets.isConnected()) {
            StatsPlugin.googleSheets.insertRecord(csvRecord);
        }

        isFirstRun = false;
        wallResetsSincePrev = 0;
        splitlessResets = 0;
        RTASincePrev = 0;
        breakRTASincePrev = 0;
        wallTimeSincePrev = 0;
        RTADistribution = "";

    }

    private String formatTime(Long millis) {
        if (millis == null) return "";
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), TimeZone.getTimeZone("UTC").toZoneId()).format(TIME_FORMATTER);
    }

    @Override
    protected void handleFileCreated(File file) {
        // ignored
    }


}
