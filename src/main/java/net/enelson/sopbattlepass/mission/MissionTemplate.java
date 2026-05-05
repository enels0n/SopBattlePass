package net.enelson.sopbattlepass.mission;

import net.enelson.sopbattlepass.gui.MenuItemSpec;

import java.util.Collections;
import java.util.List;

public final class MissionTemplate {

    private final String id;
    private final MissionTriggerType triggerType;
    private final String serverGroup;
    private final String target;
    private final String displayName;
    private final List<String> description;
    private final int required;
    private final int xp;
    private final MissionConditions conditions;
    private final MenuItemSpec itemSpec;

    public MissionTemplate(String id, MissionTriggerType triggerType, String serverGroup, String target, String displayName, List<String> description, int required, int xp, MissionConditions conditions, MenuItemSpec itemSpec) {
        this.id = id;
        this.triggerType = triggerType;
        this.serverGroup = serverGroup;
        this.target = target;
        this.displayName = displayName;
        this.description = Collections.unmodifiableList(description);
        this.required = required;
        this.xp = xp;
        this.conditions = conditions == null ? MissionConditions.alwaysTrue() : conditions;
        this.itemSpec = itemSpec;
    }

    public String getId() {
        return id;
    }

    public MissionTriggerType getTriggerType() {
        return triggerType;
    }

    public String getServerGroup() {
        return serverGroup;
    }

    public String getTarget() {
        return target;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getDescription() {
        return description;
    }

    public int getRequired() {
        return required;
    }

    public int getXp() {
        return xp;
    }

    public MissionConditions getConditions() {
        return conditions;
    }

    public MenuItemSpec getItemSpec() {
        return itemSpec;
    }
}
