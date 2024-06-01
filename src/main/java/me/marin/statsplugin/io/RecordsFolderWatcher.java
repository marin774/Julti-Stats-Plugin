package me.marin.statsplugin.io;

import com.google.gson.JsonObject;
import me.marin.statsplugin.StatsPlugin;
import me.marin.statsplugin.StatsPluginUtil;
import me.marin.statsplugin.stats.StatsRecord;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.management.ActiveWindowManager;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecordsFolderWatcher extends FileWatcher {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("M/d/yyyy HH:mm:ss");
    private static final String SESSION_MARKER = "$J" + StatsPlugin.VERSION;

    private int wallResetsSincePrev = 0;
    private int splitlessResets = 0;
    private long RTASincePrev = 0;
    private long breakRTASincePrev = 0;
    private long wallTimeSincePrev = 0;

    private String RTADistribution = "";

    private long lastActionMillis = 0;

    private boolean isFirstRun = true;

    // Completed runs get updated on completion and on reset, which means they would get tracked twice - this prevents that.
    private final List<String> completedRunsRecordIds = new ArrayList<>();

    public RecordsFolderWatcher(Path path) {
        super(path.toFile());
    }

    /**
     * Record files get updated on world load start, completion and on reset.
     */
    @Override
    protected void handleFileUpdated(File file) {
        if (!StatsPluginSettings.getInstance().trackerEnabled) {
            return;
        }
        if (completedRunsRecordIds.contains(file.getName())) {
            return;
        }
        if (JultiOptions.getJultiOptions().resetStyle.equals("Benchmark")) {
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
        Long LAN = recordParser.getOpenLAN();
        if (LAN != null && LAN <= finalRTA) {
            finalRTA = LAN;
        }

        // Calculate wall breaks
        if (lastActionMillis > 0) {
            long delta = now - lastActionMillis - finalRTA;
            if (delta > 0 && ActiveWindowManager.isWallActive()) {
                if (delta > StatsPluginSettings.getInstance().breakThreshold * 1000L) {
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

        Julti.log(Level.DEBUG, "Done split: " + recordParser.hasObtainedIron() + ", " + recordParser.hasObtainedWood() + ", " + recordParser.hasObtainedWood() + ", " + recordParser.getTimelinesMap());

        String date = LocalDateTime.ofInstant(Instant.ofEpochMilli(recordParser.getDate()), ZoneId.systemDefault()).format(DATETIME_FORMATTER);
        Map<String, Long> timelines = recordParser.getTimelinesMap();

        StatsRecord csvRecord = new StatsRecord(
                date,
                recordParser.getIronSource(),
                recordParser.getEnterType(),
                recordParser.getSpawnBiome(),
                recordParser.getRTA(),
                recordParser.getWoodObtainedTime(),
                recordParser.getPickaxeTime(),
                timelines.get("enter_nether"),
                timelines.get("enter_bastion"),
                timelines.get("enter_fortress"),
                timelines.get("nether_travel"),
                timelines.get("enter_stronghold"),
                timelines.get("enter_end"),
                recordParser.getRTT(),
                recordParser.getIGT(),
                String.valueOf(recordParser.getBlazeRodsPickedUp()),
                String.valueOf(recordParser.getBlazesKilled()),
                recordParser.getIronObtainedTime(),
                String.valueOf(wallResetsSincePrev),
                String.valueOf(splitlessResets),
                Math.max(0, RTASincePrev),
                breakRTASincePrev,
                wallTimeSincePrev,
                isFirstRun ? SESSION_MARKER : "",
                RTADistribution
        );

        if (recordParser.isCompleted()) {
            completedRunsRecordIds.add(file.getName());
        }

        StatsPlugin.CURRENT_SESSION.addRun(csvRecord);
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



    @Override
    protected void handleFileCreated(File file) {
        // ignored
    }


}
