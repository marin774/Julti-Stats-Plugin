package me.marin.statsplugin.stats;

import com.google.api.services.sheets.v4.model.*;

import java.util.ArrayList;
import java.util.List;

public record StatsCSVRecord(String dateTime,
                             String ironSource, String enterType, String spawnBiome,
                             String RTA, String wood, String ironPickaxe, String nether, String bastion, String fortress, String netherExit, String stronghold, String end,
                             String RTT, String IGT, String blazeRods, String blazesKilled,
                             String iron, String wallResetsSincePrev, String playedSincePrev, String RTASincePrev, String breakRTASincePrev, String wallTimeSincePrev,
                             String sessionMarker, String RTADistribution) {

    public String toCSVLine() {
        return String.format("%s,%s,%s,None,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,,%s,%s,,,,,,,%s,%s,%s,%s,%s,%s,%s,%s",
                dateTime, ironSource, enterType, spawnBiome,
                RTA, wood, ironPickaxe, nether, bastion, fortress, netherExit, stronghold, end,
                RTT, IGT, blazeRods, blazesKilled,
                iron, wallResetsSincePrev, playedSincePrev, RTASincePrev, breakRTASincePrev, wallTimeSincePrev,
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
        cellData.add(formatNumber(blazeRods, false));
        cellData.add(formatNumber(blazesKilled, false));
        cellData.add(new CellData()); // empty
        cellData.add(new CellData()); // empty
        cellData.add(new CellData()); // empty
        cellData.add(new CellData()); // empty
        cellData.add(new CellData()); // empty
        cellData.add(new CellData()); // empty
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(formatForSheets(iron))));
        cellData.add(formatNumber(wallResetsSincePrev, true));
        cellData.add(formatNumber(playedSincePrev, true));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(formatForSheets(RTASincePrev))));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(formatForSheets(breakRTASincePrev))));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(formatForSheets(wallTimeSincePrev))));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(sessionMarker)));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(RTADistribution)));

        rowData.setValues(cellData);
        return rowData;
    }

    private static CellData formatNumber(String number, boolean zeroIfNull) {
        CellData cell = new CellData();
        if (number == null && zeroIfNull) {
            number = "0";
        }
        if (number != null) cell.setUserEnteredValue(new ExtendedValue().setNumberValue((double) Long.parseLong(number)));
        return cell;
    }

    private String formatForSheets(String time) {
        if (time.isBlank()) return time;
        return "*" + time;
    }

    public static StatsCSVRecord fromCSVLine(String line) {
        String[] parts = line.split(",");
        return new StatsCSVRecord(parts[0], parts[1], parts[2], parts[4], parts[5], parts[6], parts[7], parts[8], parts[9], parts[10], parts[11], parts[12], parts[13], parts[14], parts[15], parts[17], parts[18], parts[25], parts[26], parts[27], parts[28], parts[29], parts[30], parts[31], parts[32]);
    }

    public RunData toRunData() {
        return new RunData(parseString(iron), parseString(wood), parseString(ironPickaxe), parseString(nether), parseString(bastion), parseString(fortress), parseString(netherExit), parseString(stronghold), parseString(end));
    }

    private static Long parseString(String s) {
        return s.equals("null") ? null : Long.parseLong(s);
    }

}
