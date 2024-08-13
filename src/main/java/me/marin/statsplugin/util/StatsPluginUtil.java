package me.marin.statsplugin.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.util.ExceptionUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.marin.statsplugin.io.RecordsFolderWatcher.DATETIME_FORMATTER;

public class StatsPluginUtil {

    public static JsonObject readJSON(File file) {
        try {
            String json = StatsPluginUtil.readFile(file.toPath());
            return new Gson().fromJson(json, JsonObject.class);
        } catch (FileStillEmptyException e) {
            Julti.log(Level.ERROR, file.getName() + " is empty?\n" + ExceptionUtil.toDetailedString(e));
            return null;
        } catch (Exception e) {
            Julti.log(Level.ERROR, "Could not read JSON file '" + file.getName() + "':\n" + ExceptionUtil.toDetailedString(e));
            return null;
        }
    }

    public static String extractGoogleSheetsID(String url) {
        if (url == null) return null;
        String regex = "https://docs\\.google\\.com/spreadsheets/d/([a-zA-Z0-9-_]+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            // Return the first (and only) capturing group, which is the ID
            return matcher.group(1);
        } else {
            return null;
        }
    }

    private static final ZoneId UTC_ID = TimeZone.getTimeZone("UTC").toZoneId();

    public static final DateTimeFormatter TIME_HOURS_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("HH:mm:ss.SSS")
            .toFormatter()
            .withZone(UTC_ID); // used in stats

    public static final DateTimeFormatter TIME_MINUTES_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("m:ss.S")
            .toFormatter()
            .withZone(UTC_ID); // used in OBS overlay


    public static String formatTime(Long millis) {
        return formatTime(millis, true);
    }

    public static String formatTime(Long millis, boolean hours) {
        if (millis == null) return "";
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), UTC_ID);
        if (hours) {
            return dateTime.format(TIME_HOURS_FORMATTER);
        } else {
            return dateTime.format(TIME_MINUTES_FORMATTER);
        }
    }
    public static Long parseTime(String s) {
        if (s == null || StringUtils.isBlank(s)) return null;

        if (s.matches(".*\\.[0-9]{6}")) { // old tracker format
            s = s.substring(0, s.lastIndexOf(".") + 3 + 1);
        }

        LocalTime localTime = LocalTime.parse(s, TIME_HOURS_FORMATTER);
        long time = 0;

        time += localTime.getHour() * 60 * 60 * 1000;
        time += localTime.getMinute() * 60 * 1000;
        time += localTime.getSecond() * 1000;
        time += localTime.get(ChronoField.MILLI_OF_SECOND);

        return time;
    }

    public static void runAsync(String threadName, Runnable runnable) {
        new Thread(runnable, threadName).start();
    }

    public static void runTimerAsync(Runnable runnable, int delayMs) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(runnable, 0, delayMs, TimeUnit.MILLISECONDS);
    }

    public static Instant dateTimeToInstant(String dateTime) {
        LocalDateTime ldt;
        if (dateTime.matches("\\d*-\\d*-\\d* .*\\.[0-9]{6}")) { // old tracker format
            DateTimeFormatter oldFormatter = new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
                    .toFormatter()
                    .withZone(ZoneId.systemDefault());
            ldt = LocalDateTime.parse(dateTime, oldFormatter);
        } else {
            ldt = LocalDateTime.parse(dateTime, DATETIME_FORMATTER);
        }

        return ldt.atZone(DATETIME_FORMATTER.getZone()).toInstant();
    }

    /**
     * Attempts to read a file 50 times with 10ms delay in between.
     * This is done to prevent some rare race conditions where file is being modified while it's trying to be read, which returns an empty string.
     * @param path  Path to the file
     * @return File contents in a string, UTF_8 charset
     * @throws FileStillEmptyException If the file is still empty after 50 attempts.
     */
    public static String readFile(Path path) throws FileStillEmptyException {
        int attempts = 0;
        while (attempts < 50) {
            try {
                byte[] text = Files.readAllBytes(path);
                if (text.length > 0) {
                    return new String(text, StandardCharsets.UTF_8);
                }
                Thread.sleep(10);
                attempts++;
            } catch (IOException | InterruptedException e) {
                Julti.log(Level.DEBUG, "Could not read " + path.getFileName() + ":\n" + ExceptionUtil.toDetailedString(e));
            }
        }
        throw new FileStillEmptyException("file is still empty after 50 attempts.");
    }


}
