package me.marin.statsplugin.io;

import com.google.gson.JsonObject;
import me.marin.statsplugin.StatsPlugin;
import me.marin.statsplugin.StatsPluginUtil;
import me.marin.statsplugin.VersionUtil;
import me.marin.statsplugin.stats.Session;
import me.marin.statsplugin.stats.StatsRecord;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.management.ActiveWindowManager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecordsFolderWatcher extends FileWatcher {

    // https://stackoverflow.com/questions/69389964/how-to-convert-a-temporalaccessor-a-milliseconds-timestamp-using-instant
    public static final DateTimeFormatter DATETIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("M/d/yyyy HH:mm:ss")
            .toFormatter()
            .withZone(ZoneId.systemDefault());

    private int wallResetsSincePrev = 0;
    private int splitlessResets = 0;
    private long RTASincePrev = 0;
    private long breakRTASincePrev = 0;
    private long wallTimeSincePrev = 0;

    private String RTADistribution = "";

    // Completed runs get updated on completion and on reset, which means they would get tracked twice - this prevents that.
    private final List<String> completedRunsRecordIds = new ArrayList<>();

    // Sometimes record files get updated twice (without changing)
    // FIXME: if more splits get added, ignore this
    private final List<String> mostRecentRecordIds = new ArrayList<>();

    public RecordsFolderWatcher(Path path) {
        super(path.toFile());
        Julti.log(Level.DEBUG, "Records folder watcher is running...");
    }

    @Override
    protected void handleFileUpdated(File file) {
        Julti.log(Level.DEBUG, "records> " + file.getName());
        if (!StatsPluginSettings.getInstance().trackerEnabled) {
            return;
        }
        if (file.getName().startsWith("Benchmark Reset #")) {
            return;
        }
        if (completedRunsRecordIds.contains(file.getName()) || mostRecentRecordIds.contains(file.getName())) {
            Julti.log(Level.DEBUG, "Not saving run because it was already completed/recently updated.");
            return;
        }

        JsonObject recordJSON = StatsPluginUtil.readJSON(file);
        if (recordJSON == null || recordJSON.isJsonNull()) {
            Julti.log(Level.DEBUG, "Not saving run because record is null.");
            return;
        }

        RecordParser recordParser = new RecordParser(file);
        if (!recordParser.validateRSG()) {
            Julti.log(Level.DEBUG, "Not saving run because it's not rsg.");
            return;
        }

        Julti.log(Level.DEBUG, "records> it is rsg");


        long finalRTA = recordJSON.get("final_rta").getAsLong();
        Long LAN = recordParser.getOpenLAN();
        if (LAN != null && LAN <= finalRTA) {
            finalRTA = LAN;
        }

        Julti.log(Level.DEBUG, "records> finalRTA " + finalRTA);


        // wall time calculation
        for (RSGAttemptsWatcher attemptsWatcher : InstanceManagerRunnable.instanceWatcherMap.values()) {
            breakRTASincePrev += attemptsWatcher.getBreakRTASincePrev();
            wallResetsSincePrev += attemptsWatcher.getWallResetsSincePrev();
            wallTimeSincePrev += attemptsWatcher.getWallTimeSincePrev();
            attemptsWatcher.reset();
        }

        Julti.log(Level.DEBUG, "records> read from attempts watcher");


        if (finalRTA == 0) {
            // wallResetsSincePrev++;
            Julti.log(Level.DEBUG, "final rta = 0, skipping");
            return;
        }

        RTADistribution += finalRTA/1000 + "$";

        if (!recordParser.hasDoneAnySplit()) {
            Julti.log(Level.DEBUG, "Not saving run because it has no splits. (" + finalRTA + "ms rta)");
            splitlessResets++;
            RTASincePrev += finalRTA;
            return;
        }

        Julti.log(Level.DEBUG, "Done splits: " + recordParser.hasObtainedIron() + ", " + recordParser.hasObtainedWood() + ", " + recordParser.hasObtainedWood() + ", " + recordParser.getTimelinesMap());

        String date = DATETIME_FORMATTER.format(Instant.ofEpochMilli(recordParser.getDate()));
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
                StatsPlugin.CURRENT_SESSION.isEmpty() ? Session.SESSION_MARKER : "",
                RTADistribution
        );

        if (recordParser.isCompleted()) {
            completedRunsRecordIds.add(file.getName());
        }
        mostRecentRecordIds.add(file.getName());
        if (mostRecentRecordIds.size() > 5) {
            mostRecentRecordIds.remove(0);
        }

        StatsPlugin.CURRENT_SESSION.addRun(csvRecord, true);
        StatsFileIO.getInstance().writeStats(csvRecord);
        if (StatsPluginSettings.getInstance().useSheets && StatsPlugin.googleSheets.isConnected()) {
            StatsPlugin.googleSheets.insertRecord(csvRecord);
        }

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
