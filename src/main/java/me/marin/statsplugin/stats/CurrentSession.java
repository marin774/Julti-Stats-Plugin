package me.marin.statsplugin.stats;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static me.marin.statsplugin.io.RecordsFolderWatcher.TIME_FORMATTER;

public class CurrentSession {

    private final List<StatsCSVRecord> records = new ArrayList<>();

    public void addRun(StatsCSVRecord record) {
        records.add(record);
    }

    /*
    TODO: Change StatsCSVRecord to include longs for millis instead of strings (only format when necessary), so i
    dont have to do all of this magic.
     */
    public String calculateEnters() {
        long enters = records.stream().filter(record -> record.nether().isBlank()).count();
        return String.valueOf(enters);
    }

    public String calculateAverage() {
        long totalMillis = 0;
        int count = 0;
        for (StatsCSVRecord record : records.stream().filter(record -> !record.nether().isBlank()).collect(Collectors.toList())) {
            TemporalAccessor accessor = TIME_FORMATTER.parse(record.nether());
            Instant instant = Instant.from(accessor);
            totalMillis += instant.toEpochMilli();
            count += 1;
        }

        return String.format("%.1d", totalMillis / count);
    }

    public String calculateNPH() {
        double nph = 0;

        long totalTimePlayedMillis = 0;
        /*
        for (StatsCSVRecord record : records.stream().filter(record -> !record.nether().isBlank()).collect(Collectors.toList())) {
            TemporalAccessor accessor = TIME_FORMATTER.parse(record.nether());
            Instant instant = Instant.from(accessor);
            totalTimePlayedMillis += instant.toEpochMilli();
            count += 1;
        }*/

        return String.format("%.1d", nph);
    }





}
