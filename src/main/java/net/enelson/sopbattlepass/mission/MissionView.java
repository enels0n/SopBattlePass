package net.enelson.sopbattlepass.mission;

import java.util.Collections;
import java.util.List;

public final class MissionView {

    private final ActiveMission activeMission;
    private final int progress;
    private final boolean completed;
    private final List<String> resolvedDescription;

    public MissionView(ActiveMission activeMission, int progress, boolean completed, List<String> resolvedDescription) {
        this.activeMission = activeMission;
        this.progress = progress;
        this.completed = completed;
        this.resolvedDescription = Collections.unmodifiableList(resolvedDescription);
    }

    public ActiveMission getActiveMission() {
        return activeMission;
    }

    public int getProgress() {
        return progress;
    }

    public boolean isCompleted() {
        return completed;
    }

    public List<String> getResolvedDescription() {
        return resolvedDescription;
    }
}
