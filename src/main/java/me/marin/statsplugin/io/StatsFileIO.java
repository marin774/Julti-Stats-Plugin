package me.marin.statsplugin.io;

import me.marin.statsplugin.stats.StatsCSVRecord;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.util.ExceptionUtil;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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

    public void writeStats(StatsCSVRecord statsRecord) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(STATS_CSV_PATH.toFile(), true))) {
            writer.write(statsRecord.toCSVLine());
            writer.newLine();
        } catch (IOException e) {
            Julti.log(Level.ERROR, "Error while writing stats to stats.csv:\n" + ExceptionUtil.toDetailedString(e));
        }
        /*try (ReversedLinesFileReader reader = new ReversedLinesFileReader(path, StandardCharsets.UTF_8); RandomAccessFile writer = new RandomAccessFile(path.toFile(), "rw")) {
            String lastLine = reader.readLine();
            String[] parts = lastLine.split(", ");
            String worldId = parts[parts.length - 1];
            writer.seek(path.toFile().length());
            if (statsRecord.worldId().equals(worldId)) {
                // Delete last line (we're updating it)
                long l = path.toFile().length() - ("\n" + lastLine).getBytes().length;
                writer.setLength(l);
                writer.seek(l);
            }
            writer.write(("\n" + statsRecord.toCSVLine()).getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/
    }

    public List<StatsCSVRecord> getAllStats() {
        /*try (BufferedReader reader = Files.newReader(STATS_CSV_PATH.toFile(), StandardCharsets.UTF_8)) {
            List<StatsCSVRecord> records = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                records.add(StatsCSVRecord.fromCSVLine(line));
            }
            return records;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/
        throw new UnsupportedOperationException();
    }

    public Path getPath() {
        return STATS_CSV_PATH;
    }
}
