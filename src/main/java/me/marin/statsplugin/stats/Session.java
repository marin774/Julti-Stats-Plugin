package me.marin.statsplugin.stats;

import me.marin.statsplugin.util.StatsPluginUtil;
import me.marin.statsplugin.util.VersionUtil;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.util.ExceptionUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static me.marin.statsplugin.StatsPlugin.OBS_OVERLAY_PATH;
import static me.marin.statsplugin.StatsPlugin.OBS_OVERLAY_TEMPLATE_PATH;

public class Session {

    private final List<StatsRecord> records = new ArrayList<>();

    public static final String SESSION_MARKER = "$J" + VersionUtil.CURRENT_VERSION;

    /*public static Session merge(Session older, Session newer) {
        Session session = new Session();
        for (StatsRecord record : older.records) {
            session.addRun(record, false);
        }
        for (StatsRecord record : newer.records) {
            session.addRun(record, false);
        }
        return session;
    }*/

    public void addRun(StatsRecord record, boolean isNewRun) {
        records.add(record);
        if (isNewRun) {
            Julti.log(Level.DEBUG, "Added this run, updating overlay: " + record);
            updateOverlay();
        }
    }

    public void updateOverlay() {
        long enters = calculateEnters();
        double average = calculateAverage();
        double nph = calculateNPH();
        if (Double.isNaN(nph)) {
            nph = 0;
        }
        double rpe = calculateRPE();
        if (Double.isNaN(rpe)) {
            rpe = 0;
        }

        try {
            String template = new String(Files.readAllBytes(OBS_OVERLAY_TEMPLATE_PATH), StandardCharsets.UTF_8);
            template = template.replaceAll("%enters%", String.valueOf(enters));
            template = template.replaceAll("%nph%", String.format(Locale.US, "%.1f", nph));
            template = template.replaceAll("%average%", StatsPluginUtil.formatTime((long)average, false));
            template = template.replaceAll("%rpe%", String.format(Locale.US, "%.0f", rpe));

            Julti.log(Level.DEBUG, "Setting overlay to:\n" + template);
            Files.write(OBS_OVERLAY_PATH, template.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            Julti.log(Level.ERROR, "Failed to update OBS overlay:\n" + ExceptionUtil.toDetailedString(e));
        }
    }

    public long calculateEnters() {
        return records.stream().filter(record -> record.nether() != null).count();
    }

    public double calculateAverage() {
        long totalMillis = 0;
        int count = 0;
        for (StatsRecord record : records) {
            if (record.nether() != null) {
                totalMillis += record.nether();
                count += 1;
            }
        }

        return (double) totalMillis / count;
    }

    public double calculateNPH() {
        long totalTimePlayedMillis = 0;
        long enters = calculateEnters();
        for (StatsRecord record : records) {
            totalTimePlayedMillis += record.wallTimeSincePrev();
            if (record.nether() != null) {
                totalTimePlayedMillis += record.nether();
            } else {
                totalTimePlayedMillis += record.RTA();
            }
            totalTimePlayedMillis += record.RTASincePrev();
        }
        double totalTimePlayedMinutes = (double) totalTimePlayedMillis / 1000 / 60;
        return 60 / (totalTimePlayedMinutes / enters);
    }

    // Resets per enter
    public double calculateRPE() {
        long enters = calculateEnters();
        long resets = records.size();
        for (StatsRecord record : records) {
            resets += Long.parseLong(record.wallResetsSincePrev());
            resets += Long.parseLong(record.playedSincePrev());
        }
        return (double) resets / enters;
    }

    public StatsRecord getLatestRecord() {
        if (records.isEmpty()) return null;
        return records.get(records.size() - 1);
    }

    public boolean isEmpty() {
        return records.isEmpty();
    }

}
