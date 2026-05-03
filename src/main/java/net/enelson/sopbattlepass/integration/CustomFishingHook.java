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

import java.lang.reflect.Method;
import java.util.Locale;

public final class CustomFishingHook implements Listener {

    private final SopBattlePass plugin;
    private final MissionService missionService;
    private boolean registered;

    public CustomFishingHook(SopBattlePass plugin, MissionService missionService) {
        this.plugin = plugin;
        this.missionService = missionService;
    }

    public boolean register() {
        if (!plugin.getConfig().getBoolean("integrations.custom-fishing.enabled", true)) {
            return false;
        }
        Plugin customFishing = Bukkit.getPluginManager().getPlugin("CustomFishing");
        if (customFishing == null || !customFishing.isEnabled()) {
            return false;
        }

        ClassLoader classLoader = customFishing.getClass().getClassLoader();
        boolean any = false;
        any |= registerEvent(classLoader, "net.momirealms.customfishing.api.event.RodCastEvent", new ReflectiveEventConsumer() {
            @Override
            public void accept(Event event) throws Exception {
                Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);
                if (player != null) {
                    missionService.recordCustomProgress(player, "CUSTOMFISHING_CAST", 1);
                }
            }
        });
        any |= registerEvent(classLoader, "net.momirealms.customfishing.api.event.FishingResultEvent", new ReflectiveEventConsumer() {
            @Override
            public void accept(Event event) throws Exception {
                Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);
                if (player == null) {
                    return;
                }
                Object result = event.getClass().getMethod("getResult").invoke(event);
                int amount = ((Number) event.getClass().getMethod("getAmount").invoke(event)).intValue();
                int safeAmount = Math.max(1, amount);

                missionService.recordCustomProgress(player, "CUSTOMFISHING_RESULT", 1);
                if (result != null) {
                    String resultKey = normalizeKey(result.toString());
                    missionService.recordCustomProgress(player, "CUSTOMFISHING_RESULT_" + resultKey, 1);
                }

                Object loot = event.getClass().getMethod("getLoot").invoke(event);
                if (loot != null) {
                    Method idMethod = loot.getClass().getMethod("id");
                    Object lootId = idMethod.invoke(loot);
                    missionService.recordCustomProgress(player, "CUSTOMFISHING_LOOT", safeAmount);
                    if (lootId != null) {
                        missionService.recordCustomProgress(player, "CUSTOMFISHING_LOOT_" + normalizeKey(String.valueOf(lootId)), safeAmount);
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
