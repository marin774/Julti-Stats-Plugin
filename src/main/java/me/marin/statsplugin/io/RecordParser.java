package me.marin.statsplugin.io;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;

public class RecordParser {

    private final JsonObject record;

    private final JsonObject adv;
    private final Set<String> advKeys;
    private final JsonObject stats;
    private final Set<String> statsKeys;
    private final JsonObject crafted;
    private final Set<String> craftedKeys;
    private final JsonObject mined;
    private final Set<String> minedKeys;
    private final JsonObject killed;
    private final Set<String> killedKeys;
    private final JsonObject used;
    private final Set<String> usedKeys;
    private final JsonObject custom;
    private final Set<String> customKeys;
    private final JsonObject pickedUp;
    private final Set<String> pickedUpKeys;

    public RecordParser(JsonObject recordJson) {
        this.record = recordJson;
        this.adv = this.record.keySet().contains("advancements") ? this.record.get("advancements").getAsJsonObject() : null;
        this.advKeys = adv != null ? adv.keySet() : Collections.emptySet();

        JsonObject stats = this.record.keySet().contains("stats") ? this.record.get("stats").getAsJsonObject() : null;
        if (stats != null && !stats.keySet().isEmpty()) {
            String uuid = stats.keySet().iterator().next();
            stats = stats.get(uuid).getAsJsonObject().get("stats").getAsJsonObject();
        }
        this.stats = stats;
        this.statsKeys = this.stats != null ? this.stats.keySet() : Collections.emptySet();

        this.crafted = stats != null && statsKeys.contains("minecraft:crafted") ? stats.get("minecraft:crafted").getAsJsonObject() : null;
        this.craftedKeys = crafted != null ? crafted.keySet() : Collections.emptySet();
        this.mined = stats != null && statsKeys.contains("minecraft:mined") ? stats.get("minecraft:mined").getAsJsonObject() : null;
        this.minedKeys = mined != null ? mined.keySet() : Collections.emptySet();
        this.killed = stats != null && statsKeys.contains("minecraft:killed") ? stats.get("minecraft:killed").getAsJsonObject() : null;
        this.killedKeys = killed != null ? killed.keySet() : Collections.emptySet();
        this.used = stats != null && statsKeys.contains("minecraft:used") ? stats.get("minecraft:used").getAsJsonObject() : null;
        this.usedKeys = used != null ? used.keySet() : Collections.emptySet();
        this.custom = stats != null && statsKeys.contains("minecraft:custom") ? stats.get("minecraft:custom").getAsJsonObject() : null;
        this.customKeys = custom != null ? custom.keySet() : Collections.emptySet();
        this.pickedUp = stats != null && statsKeys.contains("minecraft:picked_up") ? stats.get("minecraft:picked_up").getAsJsonObject() : null;
        this.pickedUpKeys = pickedUp != null ? pickedUp.keySet() : Collections.emptySet();
    }

    public boolean validateRSG() {
        if (!record.get("run_type").getAsString().equals("random_seed")) {
            return false;
        }
        if (!record.get("world_name").getAsString().startsWith("Random Speedrun #")) {
            return false;
        }
        return true;
    }

    public long getDate() {
        return record.get("date").getAsLong();
    }

    public int getBlazesKilled() {
        if (killedKeys.contains("minecraft:blaze")) {
            return killed.get("minecraft:blaze").getAsInt();
        }
        return 0;
    }

    public int getBlazeRodsPickedUp() {
        if (pickedUpKeys.contains("minecraft:blaze_rod")) {
            return pickedUp.get("minecraft:blaze_rod").getAsInt();
        }
        return 0;
    }

    public boolean isCompleted() {
        return record.get("is_completed").getAsBoolean();
    }

    public boolean hasObtainedWood() {
        if (!advKeys.contains("minecraft:recipes/misc/charcoal")) {
            return false;
        }
        JsonObject criteria = getNestedJsonObject(adv, "minecraft:recipes/misc/charcoal", "criteria");
        if (criteria == null) {
            return false;
        }
        return criteria.keySet().contains("has_log");
    }

    public Long getWoodObtainedTime() {
        if (!hasObtainedWood()) {
            return null;
        }
        JsonObject hasLog = getNestedJsonObject(adv, "minecraft:recipes/misc/charcoal", "criteria", "has_log");
        if (hasLog == null) {
            return null;
        }
        Long LAN = getOpenLAN();
        long IGT = getLong(hasLog, "igt");
        long RTA = getLong(hasLog, "rta");
        if (LAN != null && LAN <= RTA) {
            return null;
        }
        return IGT;
    }

    public boolean hasObtainedIron() {
        return advKeys.contains("minecraft:story/smelt_iron");
    }

    public Long getIronObtainedTime() {
        if (!hasObtainedIron()) return null;
        JsonObject obtainedIron = getNestedJsonObject(adv, "minecraft:story/smelt_iron");
        if (obtainedIron == null) {
            return null;
        }
        JsonElement IGTelement = obtainedIron.get("igt");
        JsonElement RTAelement = obtainedIron.get("rta");
        if (IGTelement == null || IGTelement.isJsonNull() || RTAelement == null || RTAelement.isJsonNull()) {
            return null;
        }

        Long LAN = getOpenLAN();
        long IGT = getLong(obtainedIron, "igt");
        long RTA = getLong(obtainedIron, "rta");

        if (LAN != null && LAN <= RTA) {
            return null;
        }
        return IGT;
    }

    public boolean hasObtainedPickaxe() {
        if (craftedKeys.contains("minecraft:diamond_pickaxe")) {
            return true;
        }
        JsonObject ironPickaxe = getNestedJsonObject(adv, "minecraft:story/iron_tools", "criteria", "iron_pickaxe");
        return (ironPickaxe != null);
    }

    public Long getPickaxeTime() {
        JsonObject ironPickaxe = getNestedJsonObject(adv, "minecraft:story/iron_tools", "criteria", "iron_pickaxe");
        if (ironPickaxe == null) {
            return null;
        }
        Long LAN = getOpenLAN();
        long IGT = ironPickaxe.get("igt").getAsLong();
        long RTA = ironPickaxe.get("rta").getAsLong();
        if (LAN != null && LAN <= RTA) {
            return null;
        }
        return IGT;
    }


    public String getSpawnBiome() {
        JsonObject criteria = getNestedJsonObject(adv, "minecraft:adventure/adventuring_time", "criteria");
        if (criteria == null) return "Unknown";
        Set<String> visitedBiomes = criteria.keySet();
        for (String visitedBiome : visitedBiomes) {
            if (getAdvancementIGT(criteria, visitedBiome) == 0) {
                return visitedBiome.split(":")[1];
            }
        }
        return "Unknown";
    }

    public String getEnterType() {
        String enterType = "None";
        if (advKeys.contains("minecraft:story/enter_the_nether")) {
            enterType = "Obsidian";
            if (minedKeys.contains("minecraft:magma_block")) {
                if (advKeys.contains("minecraft:story/lava_bucket")) {
                    return "Magma Ravine";
                } else {
                    return "Bucketless";
                }
            } else if (advKeys.contains("minecraft:story/lava_bucket")) {
                return "Lava Pool";
            }
        }
        return enterType;
    }

    public String getIronSource() {
        boolean obtainedIron = hasObtainedIron() || advKeys.contains("minecraft:story/iron_tools") || craftedKeys.contains("minecraft:diamond_pickaxe");

        if (!obtainedIron) return "None";

        String ironSource = "Misc";

        // if iron not obtained before nether enter
        boolean ironInNether = advKeys.contains("minecraft:story/enter_the_nether") && advKeys.contains("minecraft:story/smelt_iron")
                && getAdvancementIGT(adv, "minecraft:story/enter_the_nether") < getAdvancementIGT(adv, "minecraft:story/smelt_iron");
        if (ironInNether) {
            return "Nether";
        }

        // if furnace crafted and iron ore mined
        boolean smeltedIron = craftedKeys.contains("minecraft:furnace") && minedKeys.contains("minecraft:iron_ore");
        if (smeltedIron) {
            return "Structureless";
        }

        // if haybale mined or iron golem killed or iron pickaxe obtained from chest
        boolean village = minedKeys.contains("minecraft:hay_block") || killedKeys.contains("minecraft:iron_golem") ||
                (advKeys.contains("minecraft:story/iron_tools") && !(craftedKeys.contains("minecraft:iron_pickaxe") || craftedKeys.contains("minecraft:diamond_pickaxe")));
        if (village) {
            return "Village";
        }

        // if more than 7 tnt mined
        boolean desertTemple = minedKeys.contains("minecraft:tnt") && mined.get("minecraft:tnt").getAsInt() > 7;
        if (desertTemple) {
            return "Desert Temple";
        }

        // if visited ocean/beach biome in the first 3 minutes
        boolean visitedOceanOrBeach = false;
        if (adv != null) {
            JsonObject criteria = getNestedJsonObject(adv, "minecraft:adventure/adventuring_time", "criteria");
            if (criteria != null) {
                Set<String> visitedBiomes = criteria.keySet();
                for (String visitedBiome : visitedBiomes) {
                    if (visitedBiome.contains("beach") || visitedBiome.contains("ocean") && getAdvancementIGT(criteria, visitedBiome) < 180000) {
                        visitedOceanOrBeach = true;
                        break;
                    }
                }
            }

        }
        if (visitedOceanOrBeach) {

            boolean isFullShipwreck = advKeys.contains("minecraft:recipes/food/baked_potato") ||
                    advKeys.contains("minecraft:recipes/food/bread") ||
                    advKeys.contains("minecraft:recipes/transportation/carrot_on_a_stick") ||
                    usedKeys.contains("minecraft:suspicious_stew") ||
                    usedKeys.contains("minecraft:rotten_flesh");

            boolean explodedTNT = usedKeys.contains("minecraft:tnt");

            // if cooked salmon or cod eaten OR if sand/gravel mined before iron acquired
            boolean buriedTreasure = usedKeys.contains("minecraft:cooked_salmon") || usedKeys.contains("minecraft:cooked_cod") ||
                    (advKeys.contains("minecraft:recipes/building_blocks/magenta_concrete_powder") &&
                            getNestedJsonObject(adv, "minecraft:recipes/building_blocks/magenta_concrete_powder", "criteria", "has_the_recipe").get("igt").getAsLong() < getAdvancementIGT(adv, "minecraft:story/smelt_iron"));

            // if wood obtained before iron
            boolean woodBeforeIron = (advKeys.contains("minecraft:story/smelt_iron") && advKeys.contains("minecraft:recipes/misc/charcoal") &&
                    (getAdvancementIGT(adv, "minecraft:story/smelt_iron") > getAdvancementIGT(adv, "minecraft:recipes/misc/charcoal")))
                    ||
                    (!advKeys.contains("minecraft:story/smelt_iron") && advKeys.contains("minecraft:recipes/misc/charcoal"));

            boolean craftedDiamondPickOrSword = craftedKeys.contains("minecraft:diamond_pickaxe") || craftedKeys.contains("minecraft:diamond_sword");

            // Mined 4 or fewer logs of one type
            boolean minedFewLogs =
                    (minedKeys.contains("minecraft:oak_log") && getLong(mined, "minecraft:oak_log") <= 4) ||
                            (minedKeys.contains("minecraft:dark_oak_log") && getLong(mined, "minecraft:dark_oak_log") <= 4) ||
                            (minedKeys.contains("minecraft:birch_log") && getLong(mined, "minecraft:birch_log") <= 4) ||
                            (minedKeys.contains("minecraft:jungle_log") && getLong(mined, "minecraft:jungle_log") <= 4) ||
                            (minedKeys.contains("minecraft:spruce_log") && getLong(mined, "minecraft:spruce_log") <= 4) ||
                            (minedKeys.contains("minecraft:acacia_log") && getLong(mined, "minecraft:acacia_log") <= 4);


            if (isFullShipwreck) {
                ironSource = "Full Shipwreck";
            } else if (explodedTNT) {
                ironSource = "Buried Treasure w/ tnt";
            } else if (buriedTreasure) {
                ironSource = "Buried Treasure";
            } else if (woodBeforeIron) {
                if ((customKeys.contains("minecraft:open_chest") && custom.get("minecraft:open_chest").getAsInt() == 1) || advKeys.contains("minecraft:nether/find_bastion")) {
                    ironSource = "Half Shipwreck";
                } else {
                    ironSource = "Full Shipwreck";
                }
            } else if (craftedDiamondPickOrSword) {
                ironSource = "Buried Treasure";
            } else if (minedFewLogs) {
                ironSource = "Half Shipwreck";
            } else {
                ironSource = "Full Shipwreck";
            }

        }


        return ironSource;
    }

    public long getRTA() {
        return getLong(record, "final_rta");
    }
    public long getRTT() {
        return getLong(record, "retimed_igt");
    }
    public long getIGT() {
        return getLong(record, "final_igt");
    }
    public Long getOpenLAN() {
        JsonElement element = record.get("open_lan");
        if (element.isJsonNull()) {
            return null;
        }
        return element.getAsLong();
    }

    private static final List<String> TIMELINES_SPLITS;

    static {
        TIMELINES_SPLITS = new ArrayList<>();
        TIMELINES_SPLITS.add("enter_nether");
        TIMELINES_SPLITS.add("enter_bastion");
        TIMELINES_SPLITS.add("enter_fortress");
        TIMELINES_SPLITS.add("nether_travel");
        TIMELINES_SPLITS.add("enter_stronghold");
        TIMELINES_SPLITS.add("enter_end");
    }

    public Map<String, Long> getTimelinesMap() {
        JsonArray timelines = record.get("timelines").getAsJsonArray();
        Map<String, Long> map = new LinkedHashMap<>();
        Long LAN = getOpenLAN();
        for (JsonElement element : timelines) {
            JsonObject obj = element.getAsJsonObject();
            String splitName = obj.get("name").getAsString();
            if (!TIMELINES_SPLITS.contains(splitName)) {
                // unimportant split
                continue;
            }
            long IGT = getLong(obj, "igt");
            long RTA = getLong(obj, "rta");
            if (LAN != null && LAN <= RTA) {
                // Split was cheated
                continue;
            }
            map.put(splitName, IGT);
        }
        return map;
    }

    public boolean hasDoneAnySplit() {
        if (hasObtainedWood() || hasObtainedIron() || hasObtainedPickaxe()) return true;
        return !getTimelinesMap().isEmpty();
    }

    private static long getAdvancementIGT(JsonObject advancements, String advancement) {
        return getLong(advancements.get(advancement).getAsJsonObject(), "igt");
    }

    private static long getLong(JsonObject jsonObject, String s) {
        return jsonObject.get(s).getAsLong();
    }

    private static JsonObject getNestedJsonObject(JsonObject root, String... tree) {
        for (String s : tree) {
            JsonElement element = root.get(s);
            if (element == null) return null;
            root = element.getAsJsonObject();
        }
        return root;
    }

}
