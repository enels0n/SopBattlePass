package net.enelson.sopbattlepass.mission;

import net.enelson.sopbattlepass.SopBattlePass;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public final class MissionPlaytimeTask {

    private final SopBattlePass plugin;
    private final MissionService missionService;
    private BukkitTask task;

    public MissionPlaytimeTask(SopBattlePass plugin, MissionService missionService) {
        this.plugin = plugin;
        this.missionService = missionService;
    }

    public void start() {
        stop();
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    missionService.recordSimpleProgress(player, MissionTriggerType.PLAY_TIME, "", 60);
                }
            }
        }, 20L * 60L, 20L * 60L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
