package net.enelson.sopbattlepass.reward;

import java.util.Collections;
import java.util.List;

public final class RewardBundle {

    private final List<String> commands;

    public RewardBundle(List<String> commands) {
        this.commands = Collections.unmodifiableList(commands);
    }

    public List<String> getCommands() {
        return commands;
    }
}
