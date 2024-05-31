package me.marin.statsplugin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatsPluginUtil {

    public static JsonObject readJSON(File file) {
        try (JsonReader reader = new JsonReader(new FileReader(file))) {
            return new Gson().fromJson(reader, JsonObject.class);
        } catch (Exception ignored) {}
        return null;
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

    public static final DateTimeFormatter TIME_HOURS_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS"); // used in stats
    public static final DateTimeFormatter TIME_MINUTES_FORMATTER = DateTimeFormatter.ofPattern("m:ss.S"); // used in OBS overlay
    public static String formatTime(Long millis) {
        return formatTime(millis, true);
    }

    public static String formatTime(Long millis, boolean hours) {
        if (millis == null) return "";
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), TimeZone.getTimeZone("UTC").toZoneId());
        if (hours) {
            return dateTime.format(TIME_HOURS_FORMATTER);
        } else {
            return dateTime.format(TIME_MINUTES_FORMATTER);
        }
    }

}
