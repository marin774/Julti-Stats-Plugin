package me.marin.statsplugin.stats;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RunData {

    public static final String IRON = "Iron";
    public static final String IRON_PICKAXE = "Iron Pickaxe";
    public static final String WOOD = "Wood";
    public static final String NETHER_ENTER = "Nether";
    public static final String STRUCTURE_1 = "Structure 1";
    public static final String STRUCTURE_2 = "Structure 2";
    public static final String NETHER_EXIT = "Nether Exit";
    public static final String STRONGHOLD = "Stronghold";
    public static final String END_ENTER = "End";
    public static final List<String> LABELS = List.of(IRON, WOOD, IRON_PICKAXE, NETHER_ENTER, STRUCTURE_1, STRUCTURE_2, NETHER_EXIT, STRONGHOLD, END_ENTER);

    private final Map<String, Long> splitsMap = new LinkedHashMap<>();

    public RunData(Long iron, Long wood, Long ironPickaxe, Long nether, Long bastion, Long fortress, Long netherExit, Long stronghold, Long end) {
        if (iron != null) splitsMap.put(IRON, iron);
        if (wood != null) splitsMap.put(WOOD, wood);
        if (ironPickaxe != null) splitsMap.put(IRON_PICKAXE, ironPickaxe);
        if (nether != null) splitsMap.put(NETHER_ENTER, nether);
        if (bastion != null && fortress != null) {
            splitsMap.put(STRUCTURE_1, Math.min(bastion, fortress));
            splitsMap.put(STRUCTURE_2, Math.max(bastion, fortress));
        } else if (bastion != null) {
            splitsMap.put(STRUCTURE_1, bastion);
        } else if (fortress != null) {
            splitsMap.put(STRUCTURE_1, fortress);
        }
        if (netherExit != null) splitsMap.put(NETHER_EXIT, netherExit);
        if (stronghold != null) splitsMap.put(STRONGHOLD, stronghold);
        if (end != null) splitsMap.put(END_ENTER, end);
    }

    public boolean hasSplit(String splitName) {
        return splitsMap.containsKey(splitName);
    }

    public Long getSplit(String splitName) {
        return splitsMap.get(splitName);
    }

    public Map<String, Long> getSplitsMap() {
        return splitsMap;
    }

    public Long valueAt(int columnIndex) {
        return splitsMap.get(LABELS.get(columnIndex));
    }
}
