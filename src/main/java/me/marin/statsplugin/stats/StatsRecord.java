package me.marin.statsplugin.stats;

import com.google.api.services.sheets.v4.model.*;
import me.marin.statsplugin.StatsPluginUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static me.marin.statsplugin.StatsPluginUtil.formatTime;

public final class StatsRecord {
    private final String dateTime;
    private final String ironSource;
    private final String enterType;
    private final String spawnBiome;
    private final Long RTA;
    private final Long wood;
    private final Long ironPickaxe;
    private final Long nether;
    private final Long bastion;
    private final Long fortress;
    private final Long netherExit;
    private final Long stronghold;
    private final Long end;
    private final Long RTT;
    private final Long IGT;
    private final String blazeRods;
    private final String blazesKilled;
    private final Long iron;
    private final String wallResetsSincePrev;
    private final String playedSincePrev;
    private final Long RTASincePrev;
    private final Long breakRTASincePrev;
    private final Long wallTimeSincePrev;
    private final String sessionMarker;
    private final String RTADistribution;

    public StatsRecord(String dateTime,
                       String ironSource, String enterType, String spawnBiome,
                       Long RTA, Long wood, Long ironPickaxe, Long nether, Long bastion, Long fortress, Long netherExit, Long stronghold, Long end,
                       Long RTT, Long IGT, String blazeRods, String blazesKilled,
                       Long iron, String wallResetsSincePrev, String playedSincePrev, Long RTASincePrev, Long breakRTASincePrev, Long wallTimeSincePrev,
                       String sessionMarker, String RTADistribution) {
        this.dateTime = dateTime;
        this.ironSource = ironSource;
        this.enterType = enterType;
        this.spawnBiome = spawnBiome;
        this.RTA = RTA;
        this.wood = wood;
        this.ironPickaxe = ironPickaxe;
        this.nether = nether;
        this.bastion = bastion;
        this.fortress = fortress;
        this.netherExit = netherExit;
        this.stronghold = stronghold;
        this.end = end;
        this.RTT = RTT;
        this.IGT = IGT;
        this.blazeRods = blazeRods;
        this.blazesKilled = blazesKilled;
        this.iron = iron;
        this.wallResetsSincePrev = wallResetsSincePrev;
        this.playedSincePrev = playedSincePrev;
        this.RTASincePrev = RTASincePrev;
        this.breakRTASincePrev = breakRTASincePrev;
        this.wallTimeSincePrev = wallTimeSincePrev;
        this.sessionMarker = sessionMarker;
        this.RTADistribution = RTADistribution;
    }

    public String dateTime() {
        return dateTime;
    }

    public String ironSource() {
        return ironSource;
    }

    public String enterType() {
        return enterType;
    }

    public String spawnBiome() {
        return spawnBiome;
    }

    public Long RTA() {
        return RTA;
    }

    public Long wood() {
        return wood;
    }

    public Long ironPickaxe() {
        return ironPickaxe;
    }

    public Long nether() {
        return nether;
    }

    public Long bastion() {
        return bastion;
    }

    public Long fortress() {
        return fortress;
    }

    public Long netherExit() {
        return netherExit;
    }

    public Long stronghold() {
        return stronghold;
    }

    public Long end() {
        return end;
    }

    public Long RTT() {
        return RTT;
    }

    public Long IGT() {
        return IGT;
    }

    public String blazeRods() {
        return blazeRods;
    }

    public String blazesKilled() {
        return blazesKilled;
    }

    public Long iron() {
        return iron;
    }

    public String wallResetsSincePrev() {
        return wallResetsSincePrev;
    }

    public String playedSincePrev() {
        return playedSincePrev;
    }

    public Long RTASincePrev() {
        return RTASincePrev;
    }

    public Long breakRTASincePrev() {
        return breakRTASincePrev;
    }

    public Long wallTimeSincePrev() {
        return wallTimeSincePrev;
    }

    public String sessionMarker() {
        return sessionMarker;
    }

    public String RTADistribution() {
        return RTADistribution;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        StatsRecord that = (StatsRecord) obj;
        return Objects.equals(this.dateTime, that.dateTime) &&
                Objects.equals(this.ironSource, that.ironSource) &&
                Objects.equals(this.enterType, that.enterType) &&
                Objects.equals(this.spawnBiome, that.spawnBiome) &&
                Objects.equals(this.RTA, that.RTA) &&
                Objects.equals(this.wood, that.wood) &&
                Objects.equals(this.ironPickaxe, that.ironPickaxe) &&
                Objects.equals(this.nether, that.nether) &&
                Objects.equals(this.bastion, that.bastion) &&
                Objects.equals(this.fortress, that.fortress) &&
                Objects.equals(this.netherExit, that.netherExit) &&
                Objects.equals(this.stronghold, that.stronghold) &&
                Objects.equals(this.end, that.end) &&
                Objects.equals(this.RTT, that.RTT) &&
                Objects.equals(this.IGT, that.IGT) &&
                Objects.equals(this.blazeRods, that.blazeRods) &&
                Objects.equals(this.blazesKilled, that.blazesKilled) &&
                Objects.equals(this.iron, that.iron) &&
                Objects.equals(this.wallResetsSincePrev, that.wallResetsSincePrev) &&
                Objects.equals(this.playedSincePrev, that.playedSincePrev) &&
                Objects.equals(this.RTASincePrev, that.RTASincePrev) &&
                Objects.equals(this.breakRTASincePrev, that.breakRTASincePrev) &&
                Objects.equals(this.wallTimeSincePrev, that.wallTimeSincePrev) &&
                Objects.equals(this.sessionMarker, that.sessionMarker) &&
                Objects.equals(this.RTADistribution, that.RTADistribution);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dateTime, ironSource, enterType, spawnBiome, RTA, wood, ironPickaxe, nether, bastion, fortress, netherExit, stronghold, end, RTT, IGT, blazeRods, blazesKilled, iron, wallResetsSincePrev, playedSincePrev, RTASincePrev, breakRTASincePrev, wallTimeSincePrev, sessionMarker, RTADistribution);
    }

    @Override
    public String toString() {
        return "StatsRecord[" +
                "dateTime=" + dateTime + ", " +
                "ironSource=" + ironSource + ", " +
                "enterType=" + enterType + ", " +
                "spawnBiome=" + spawnBiome + ", " +
                "RTA=" + RTA + ", " +
                "wood=" + wood + ", " +
                "ironPickaxe=" + ironPickaxe + ", " +
                "nether=" + nether + ", " +
                "bastion=" + bastion + ", " +
                "fortress=" + fortress + ", " +
                "netherExit=" + netherExit + ", " +
                "stronghold=" + stronghold + ", " +
                "end=" + end + ", " +
                "RTT=" + RTT + ", " +
                "IGT=" + IGT + ", " +
                "blazeRods=" + blazeRods + ", " +
                "blazesKilled=" + blazesKilled + ", " +
                "iron=" + iron + ", " +
                "wallResetsSincePrev=" + wallResetsSincePrev + ", " +
                "playedSincePrev=" + playedSincePrev + ", " +
                "RTASincePrev=" + RTASincePrev + ", " +
                "breakRTASincePrev=" + breakRTASincePrev + ", " +
                "wallTimeSincePrev=" + wallTimeSincePrev + ", " +
                "sessionMarker=" + sessionMarker + ", " +
                "RTADistribution=" + RTADistribution + ']';
    }


    public String toCSVLine() {
        return String.format("%s,%s,%s,None,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,,%s,%s,,,,,,,%s,%s,%s,%s,%s,%s,%s,%s",
                dateTime, ironSource, enterType, spawnBiome,
                formatTime(RTA), formatTime(wood), formatTime(ironPickaxe), formatTime(nether), formatTime(bastion), formatTime(fortress), formatTime(netherExit), formatTime(stronghold), formatTime(end),
                formatTime(RTT), formatTime(IGT), blazeRods, blazesKilled,
                formatTime(iron), wallResetsSincePrev, playedSincePrev, formatTime(RTASincePrev), formatTime(breakRTASincePrev), formatTime(wallTimeSincePrev),
                sessionMarker, RTADistribution);
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
        if (number != null)
            cell.setUserEnteredValue(new ExtendedValue().setNumberValue((double) Long.parseLong(number)));
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
        if (StringUtils.isBlank(s)) return null;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

}
