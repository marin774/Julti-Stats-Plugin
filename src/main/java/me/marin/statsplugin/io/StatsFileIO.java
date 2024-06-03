package me.marin.statsplugin.io;

import me.marin.statsplugin.stats.Session;
import me.marin.statsplugin.stats.StatsRecord;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.util.ExceptionUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Stack;

public class StatsFileIO {

    public static final Path STATS_CSV_PATH = Paths.get(System.getProperty("user.home")).resolve(".Julti").resolve("stats-plugin").resolve("stats.csv");

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
        Session session = new Session();
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
        while (!runs.isEmpty()) {
            session.addRun(runs.pop(), false);
        }
        return session;
    }

    public Path getPath() {
        return STATS_CSV_PATH;
    }
}
