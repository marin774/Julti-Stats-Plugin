package me.marin.statsplugin.io;

import me.marin.statsplugin.stats.Session;
import me.marin.statsplugin.stats.StatsRecord;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.util.ExceptionUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Stack;

public class StatsFileIO {

    public static final Path STATS_CSV_PATH = Paths.get(System.getProperty("user.home")).resolve(".Julti").resolve("stats-plugin").resolve("stats.csv");
    public static final Path TEMP_STATS_CSV_PATH = Paths.get(System.getProperty("user.home")).resolve(".Julti").resolve("stats-plugin").resolve("temp-stats");

    private static final StatsFileIO INSTANCE = new StatsFileIO();

    private StatsFileIO() {
        createFile();
    }

    public static StatsFileIO getInstance() {
        return INSTANCE;
    }

    private void createFile() {
        try {
            STATS_CSV_PATH.toFile().getParentFile().mkdirs();
            STATS_CSV_PATH.toFile().createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeStats(StatsRecord statsRecord) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(STATS_CSV_PATH.toFile(), true))) {
            writer.write(statsRecord.toCSVLine());
            writer.newLine();
        } catch (IOException e) {
            Julti.log(Level.ERROR, "Error while writing stats to stats.csv:\n" + ExceptionUtil.toDetailedString(e));
        }
    }

    public Session getLatestSession() {
        Stack<StatsRecord> runs = new Stack<>();
        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(STATS_CSV_PATH.toFile(), StandardCharsets.UTF_8)) {
            boolean sessionMarkerFound = false;
            while (!sessionMarkerFound) {
                String lastLine = reader.readLine();
                if (lastLine == null) {
                    break;
                }
                StatsRecord statsRecord = StatsRecord.fromCSVLine(lastLine);
                runs.add(statsRecord);
                if (statsRecord.sessionMarker().startsWith("$")) {
                    sessionMarkerFound = true;
                }
            }
        } catch (IOException e) {
            Julti.log(Level.ERROR, "Error while reading stats.csv for latest session:\n" + ExceptionUtil.toDetailedString(e));
        }
        Session session = new Session();
        while (!runs.isEmpty()) {
            session.addRun(runs.pop(), false);
        }
        return session;
    }

    public void writeTempStats(StatsRecord statsRecord) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEMP_STATS_CSV_PATH.toFile(), true))) {
            writer.write(statsRecord.toCSVLine());
            writer.newLine();
        } catch (IOException e) {
            Julti.log(Level.ERROR, "Error while writing stats to stats.csv:\n" + ExceptionUtil.toDetailedString(e));
        }
    }

    public List<StatsRecord> getTempStats() {
        Stack<StatsRecord> runs = new Stack<>();
        try {
            List<String> list = Files.readAllLines(STATS_CSV_PATH);
            for (String line : list) {
                StatsRecord statsRecord = StatsRecord.fromCSVLine(line);
                runs.add(statsRecord);
            }
        } catch (IOException e) {
            Julti.log(Level.ERROR, "Error while reading temp-stats:\n" + ExceptionUtil.toDetailedString(e));
        }
        return runs;
    }

    public Path getPath() {
        return STATS_CSV_PATH;
    }
}
