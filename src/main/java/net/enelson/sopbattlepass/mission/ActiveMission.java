package net.enelson.sopbattlepass.mission;

import java.util.Collections;
import java.util.List;

public final class ActiveMission {

    private final MissionType type;
    private final String key;
    private final int slot;
    private final String periodKey;
    private final int weekNumber;
    private final MissionTemplate template;
    private final List<String> sharedSlotKeys;

    public ActiveMission(MissionType type,
                         String key,
                         int slot,
                         String periodKey,
                         int weekNumber,
                         MissionTemplate template,
                         List<String> sharedSlotKeys) {
        this.type = type;
        this.key = key;
        this.slot = slot;
        this.periodKey = periodKey;
        this.weekNumber = weekNumber;
        this.template = template;
        this.sharedSlotKeys = Collections.unmodifiableList(sharedSlotKeys);
    }

    public MissionType getType() {
        return type;
    }

    public String getKey() {
        return key;
    }

    public int getSlot() {
        return slot;
    }

    public String getPeriodKey() {
        return periodKey;
    }

    public int getWeekNumber() {
        return weekNumber;
    }

    public MissionTemplate getTemplate() {
        return template;
    }

    public List<String> getSharedSlotKeys() {
        return sharedSlotKeys;
    }
}
