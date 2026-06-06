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

public final class SopEnvoyHook implements Listener {

    private final SopBattlePass plugin;
    private final MissionService missionService;
    private boolean registered;

    public SopEnvoyHook(SopBattlePass plugin, MissionService missionService) {
        this.plugin = plugin;
        this.missionService = missionService;
    }

    public boolean register() {
        if (!plugin.getConfig().getBoolean("integrations.sopenvoy.enabled",
                plugin.getConfig().getBoolean("integrations.axenvoy.enabled", true))) {
            return false;
        }

        Plugin sopEnvoy = Bukkit.getPluginManager().getPlugin("SopEnvoy");
        if (sopEnvoy == null || !sopEnvoy.isEnabled()) {
            return false;
        }

        ClassLoader classLoader = sopEnvoy.getClass().getClassLoader();
        boolean any = false;
        any |= registerEvent(classLoader, "net.enelson.sopenvoy.event.EnvoyCrateOpenEvent", new ReflectiveEventConsumer() {
            @Override
            public void accept(Event event) throws Exception {
                Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);
                if (player == null) {
                    return;
                }

                missionService.recordCustomProgress(player, "SOPENVOY_OPEN", 1);

                Object crate = event.getClass().getMethod("getCrate").invoke(event);
                if (crate != null) {
                    Object typeId = crate.getClass().getMethod("getTypeId").invoke(crate);
                    if (typeId != null && !String.valueOf(typeId).trim().isEmpty()) {
                        missionService.recordCustomProgress(player, "SOPENVOY_OPEN_" + normalizeKey(String.valueOf(typeId)), 1);
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
