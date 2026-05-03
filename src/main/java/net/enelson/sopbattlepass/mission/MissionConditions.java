package net.enelson.sopbattlepass.mission;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class MissionConditions {

    private final boolean any;
    private final List<MissionCondition> checks;

    public MissionConditions(boolean any, List<MissionCondition> checks) {
        this.any = any;
        this.checks = checks == null ? Collections.<MissionCondition>emptyList() : new ArrayList<MissionCondition>(checks);
    }

    public static MissionConditions alwaysTrue() {
        return new MissionConditions(false, Collections.<MissionCondition>emptyList());
    }

    public boolean test(MissionService service, Player player) {
        if (checks.isEmpty()) {
            return true;
        }
        if (any) {
            for (MissionCondition check : checks) {
                if (check.test(service, player)) {
                    return true;
                }
            }
            return false;
        }
        for (MissionCondition check : checks) {
            if (!check.test(service, player)) {
                return false;
            }
        }
        return true;
    }

    public static MissionConditions fromSection(ConfigurationSection section) {
        if (section == null) {
            return alwaysTrue();
        }
        boolean any = "any".equalsIgnoreCase(section.getString("type", "all"));
        List<Map<?, ?>> rawChecks = section.getMapList("checks");
        List<MissionCondition> checks = new ArrayList<MissionCondition>();
        for (Map<?, ?> raw : rawChecks) {
            if (raw == null) {
                continue;
            }
            Object typeValue = raw.get("type");
            MissionConditionType type = MissionConditionType.fromString(typeValue == null ? null : String.valueOf(typeValue));
            if (type == null) {
                continue;
            }
            Object inputValue = raw.get("input");
            Object outputValue = raw.get("output");
            checks.add(new MissionCondition(
                    type,
                    typeValue == null ? "" : String.valueOf(typeValue),
                    inputValue == null ? "" : String.valueOf(inputValue),
                    outputValue == null ? "" : String.valueOf(outputValue)
            ));
        }
        if (checks.isEmpty()) {
            return alwaysTrue();
        }
        return new MissionConditions(any, checks);
    }
}
