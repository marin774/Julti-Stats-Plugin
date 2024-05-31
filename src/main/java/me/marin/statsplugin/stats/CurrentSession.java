package me.marin.statsplugin.stats;

import me.marin.statsplugin.StatsPlugin;
import me.marin.statsplugin.StatsPluginUtil;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.util.ExceptionUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class CurrentSession {

    private final List<StatsRecord> records = new ArrayList<>();

    public void addRun(StatsRecord record) {
        records.add(record);
        updateOverlay();
    }

    public void updateOverlay() {
        long enters = calculateEnters();
        double average = calculateAverage();
        double nph = calculateNPH();

        try {
            String template = Files.readString(StatsPlugin.OBS_OVERLAY_TEMPLATE_PATH);
            template = template.replaceAll("%enters%", String.valueOf(enters));
            template = template.replaceAll("%nph%", Double.isNaN(nph) ? "" : String.format("%.1f", nph));
            template = template.replaceAll("%average%", StatsPluginUtil.formatTime((long)average, false));

            Files.writeString(StatsPlugin.OBS_OVERLAY_PATH, template, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
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
        for (StatsRecord record : records.stream().filter(record -> record.nether() != null).toList()) {
            totalMillis += record.nether();
            count += 1;
        }

        return (double) totalMillis / count;
    }

    public double calculateNPH() {
        long totalTimePlayedMillis = 0;
        long enters = calculateEnters();
        for (StatsRecord record : records) {
            totalTimePlayedMillis += record.wallTimeSincePrev();
            totalTimePlayedMillis += record.RTA();
            totalTimePlayedMillis += record.RTASincePrev();
        }

        return 60 / ((double) totalTimePlayedMillis / enters);
    }





}
