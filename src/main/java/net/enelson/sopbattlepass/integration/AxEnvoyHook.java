package net.enelson.sopbattlepass.integration;

import net.enelson.sopbattlepass.SopBattlePass;
import net.enelson.sopbattlepass.mission.MissionService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

public final class AxEnvoyHook implements Listener {

    private final SopBattlePass plugin;
    private final MissionService missionService;
    private boolean registered;

    public AxEnvoyHook(SopBattlePass plugin, MissionService missionService) {
        this.plugin = plugin;
        this.missionService = missionService;
    }

    public boolean register() {
        if (!plugin.getConfig().getBoolean("integrations.axenvoy.enabled", true)) {
            return false;
        }
        Plugin axEnvoy = Bukkit.getPluginManager().getPlugin("AxEnvoy");
        if (axEnvoy == null || !axEnvoy.isEnabled()) {
            return false;
        }

        ClassLoader classLoader = axEnvoy.getClass().getClassLoader();
        boolean any = false;
        any |= registerEvent(classLoader, "com.artillexstudios.axenvoy.event.EnvoyCrateCollectEvent", new ReflectiveEventConsumer() {
            @Override
            public void accept(Event event) throws Exception {
                Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);
                if (player == null) {
                    return;
                }
                missionService.recordCustomProgress(player, "AXENVOY_COLLECT", 1);

                Object envoy = event.getClass().getMethod("getEnvoy").invoke(event);
                if (envoy != null) {
                    Object envoyName = envoy.getClass().getMethod("getName").invoke(envoy);
                    if (envoyName != null) {
                        missionService.recordCustomProgress(player, "AXENVOY_COLLECT_" + normalizeKey(String.valueOf(envoyName)), 1);
                    }
                }

                Object crate = event.getClass().getMethod("getCrate").invoke(event);
                if (crate != null) {
                    Object crateType = crate.getClass().getMethod("getHandle").invoke(crate);
                    if (crateType != null) {
                        Object crateName = crateType.getClass().getMethod("getName").invoke(crateType);
                        if (crateName != null) {
                            missionService.recordCustomProgress(player, "AXENVOY_CRATE_" + normalizeKey(String.valueOf(crateName)), 1);
                        }
                    }
                }

                Object reward = event.getClass().getMethod("getReward").invoke(event);
                if (reward != null) {
                    Object rewardName = reward.getClass().getMethod("name").invoke(reward);
                    if (rewardName != null && !String.valueOf(rewardName).trim().isEmpty()) {
                        missionService.recordCustomProgress(player, "AXENVOY_REWARD_" + normalizeKey(String.valueOf(rewardName)), 1);
                    }
                }
            }
        });

        this.registered = any;
        return any;
    }

    public void unregister() {
        if (!registered) {
            return;
        }
        HandlerList.unregisterAll(this);
        registered = false;
    }

    @SuppressWarnings("unchecked")
    private boolean registerEvent(ClassLoader classLoader, String className, ReflectiveEventConsumer consumer) {
        try {
            final Class<? extends Event> eventClass = (Class<? extends Event>) Class.forName(className, true, classLoader);
            Bukkit.getPluginManager().registerEvent(eventClass, this, EventPriority.MONITOR, new EventExecutor() {
                @Override
                public void execute(Listener listener, Event event) throws EventException {
                    if (!eventClass.isInstance(event)) {
                        return;
                    }
                    try {
                        consumer.accept(event);
                    } catch (Exception exception) {
                        throw new EventException(exception);
                    }
                }
            }, plugin, true);
            return true;
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Could not hook " + className + ": " + throwable.getMessage());
            return false;
        }
    }

    private String normalizeKey(String input) {
        return input == null ? "" : input.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private interface ReflectiveEventConsumer {
        void accept(Event event) throws Exception;
    }
}
