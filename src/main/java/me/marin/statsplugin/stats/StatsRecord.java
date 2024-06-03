package me.marin.statsplugin.stats;

import com.google.api.services.sheets.v4.model.*;
import me.marin.statsplugin.StatsPluginUtil;

import java.util.ArrayList;
import java.util.List;

import static me.marin.statsplugin.StatsPluginUtil.formatTime;

public record StatsRecord(String dateTime,
                          String ironSource, String enterType, String spawnBiome,
                          Long RTA, Long wood, Long ironPickaxe, Long nether, Long bastion, Long fortress, Long netherExit, Long stronghold, Long end,
                          Long RTT, Long IGT, String blazeRods, String blazesKilled,
                          Long iron, String wallResetsSincePrev, String playedSincePrev, Long RTASincePrev, Long breakRTASincePrev, Long wallTimeSincePrev,
                          String sessionMarker, String RTADistribution) {


    public String toCSVLine() {
        return String.format("%s,%s,%s,None,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,,%s,%s,,,,,,,%s,%s,%s,%s,%s,%s,%s,%s",
                dateTime, ironSource, enterType, spawnBiome,
                formatTime(RTA), formatTime(wood), formatTime(ironPickaxe), formatTime(nether), formatTime(bastion), formatTime(fortress), formatTime(netherExit), formatTime(stronghold), formatTime(end),
                formatTime(RTT), formatTime(IGT), blazeRods, blazesKilled,
                formatTime(iron), wallResetsSincePrev, playedSincePrev, formatTime(RTASincePrev), formatTime(breakRTASincePrev), formatTime(wallTimeSincePrev),
                sessionMarker,RTADistribution);
    }

    public RowData getGoogleSheetsRowData() {
        RowData rowData = new RowData();
        List<CellData> cellData = new ArrayList<>();
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(dateTime)));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(ironSource)));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(enterType)));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("None")));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(spawnBiome)));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(formatForSheets(RTA))));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(formatForSheets(wood))));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(formatForSheets(ironPickaxe))));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(formatForSheets(nether))));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(formatForSheets(bastion))));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(formatForSheets(fortress))));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(formatForSheets(netherExit))));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(formatForSheets(stronghold))));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(formatForSheets(end))));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(formatForSheets(RTT))));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(formatForSheets(IGT))));
        cellData.add(new CellData()); // gold dropped, not supported
        cellData.add(formatNumber(blazeRods, true));
        cellData.add(formatNumber(blazesKilled, true));
        cellData.add(new CellData()); // empty
        cellData.add(new CellData()); // empty
        cellData.add(new CellData()); // empty
        cellData.add(new CellData()); // empty
        cellData.add(new CellData()); // empty
        cellData.add(new CellData()); // empty
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(formatForSheets(iron))));
        cellData.add(formatNumber(wallResetsSincePrev, false));
        cellData.add(formatNumber(playedSincePrev, false));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(formatForSheets(RTASincePrev))));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(formatForSheets(breakRTASincePrev))));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(formatForSheets(wallTimeSincePrev))));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(sessionMarker)));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(RTADistribution)));

        rowData.setValues(cellData);
        return rowData;
    }

    private static String formatForSheets(Long time) {
        if (time == null) return "";
        return "*" + formatTime(time);
    }

    private static CellData formatNumber(String number, boolean emptyIfZero) {
        CellData cell = new CellData();
        if (number != null && number.equals("0") && emptyIfZero) {
            number = null;
        }
        if (number != null) cell.setUserEnteredValue(new ExtendedValue().setNumberValue((double) Long.parseLong(number)));
        return cell;
    }

    public static StatsRecord fromCSVLine(String line) {
        String[] parts = line.split(",");
        return new StatsRecord(
                parts[0],
                parts[1],
                parts[2],
                parts[4],
                StatsPluginUtil.parseTime(parts[5]),
                StatsPluginUtil.parseTime(parts[6]),
                StatsPluginUtil.parseTime(parts[7]),
                StatsPluginUtil.parseTime(parts[8]),
                StatsPluginUtil.parseTime(parts[9]),
                StatsPluginUtil.parseTime(parts[10]),
                StatsPluginUtil.parseTime(parts[11]),
                StatsPluginUtil.parseTime(parts[12]),
                StatsPluginUtil.parseTime(parts[13]),
                StatsPluginUtil.parseTime(parts[14]),
                StatsPluginUtil.parseTime(parts[15]),
                parts[17],
                parts[18],
                StatsPluginUtil.parseTime(parts[25]),
                parts[26],
                parts[27],
                StatsPluginUtil.parseTime(parts[28]),
                StatsPluginUtil.parseTime(parts[29]),
                StatsPluginUtil.parseTime(parts[30]),
                parts[31],
                parts[32]
        );
    }

    private static Long parseLong(String s) {
        if (s.isBlank()) return null;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

}
