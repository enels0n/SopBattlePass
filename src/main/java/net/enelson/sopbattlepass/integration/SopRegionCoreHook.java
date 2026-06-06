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
import java.util.UUID;

public final class SopRegionCoreHook implements Listener {

    private final SopBattlePass plugin;
    private final MissionService missionService;
    private boolean registered;

    public SopRegionCoreHook(SopBattlePass plugin, MissionService missionService) {
        this.plugin = plugin;
        this.missionService = missionService;
    }

    public boolean register() {
        if (!plugin.getConfig().getBoolean("integrations.sopregioncore.enabled", true)) {
            return false;
        }

        Plugin sopRegionCore = Bukkit.getPluginManager().getPlugin("SopRegionCore");
        if (sopRegionCore == null || !sopRegionCore.isEnabled()) {
            return false;
        }

        ClassLoader classLoader = sopRegionCore.getClass().getClassLoader();
        boolean any = false;

        any |= registerEvent(classLoader, "net.enelson.sopregioncore.event.RegionCorePlaceEvent", new ReflectiveEventConsumer() {
            @Override
            public void accept(Event event) throws Exception {
                Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);
                if (player == null) {
                    return;
                }
                missionService.recordCustomProgress(player, "SOPREGIONCORE_PLACE", 1);
            }
        });

        any |= registerEvent(classLoader, "net.enelson.sopregioncore.event.RegionCoreStartEvent", new ReflectiveEventConsumer() {
            @Override
            public void accept(Event event) throws Exception {
                Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);
                if (player == null) {
                    return;
                }
                missionService.recordCustomProgress(player, "SOPREGIONCORE_START", 1);

                Object coreType = event.getClass().getMethod("getCoreType").invoke(event);
                if (coreType != null) {
                    Object typeId = coreType.getClass().getMethod("getId").invoke(coreType);
                    if (typeId != null && !String.valueOf(typeId).trim().isEmpty()) {
                        missionService.recordCustomProgress(player, "SOPREGIONCORE_START_" + normalizeKey(String.valueOf(typeId)), 1);
                    }
                }
            }
        });

        any |= registerEvent(classLoader, "net.enelson.sopregioncore.event.RegionCoreMemberAddEvent", new ReflectiveEventConsumer() {
            @Override
            public void accept(Event event) throws Exception {
                Player actor = (Player) event.getClass().getMethod("getActor").invoke(event);
                if (actor == null) {
                    return;
                }
                missionService.recordCustomProgress(actor, "SOPREGIONCORE_MEMBER_ADD", 1);
            }
        });

        any |= registerEvent(classLoader, "net.enelson.sopregioncore.event.RegionCoreStateChangeEvent", new ReflectiveEventConsumer() {
            @Override
            public void accept(Event event) throws Exception {
                Object core = event.getClass().getMethod("getCore").invoke(event);
                Player owner = resolveCoreOwner(core);
                if (owner == null) {
                    return;
                }
                Object newState = event.getClass().getMethod("getNewState").invoke(event);
                if (newState != null) {
                    missionService.recordCustomProgress(owner, "SOPREGIONCORE_STATE_" + normalizeKey(String.valueOf(newState)), 1);
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

    private Player resolveCoreOwner(Object core) {
        if (core == null) {
            return null;
        }
        try {
            Object ownerUuid = core.getClass().getMethod("getOwnerUuid").invoke(core);
            if (!(ownerUuid instanceof String) || ((String) ownerUuid).trim().isEmpty()) {
                return null;
            }
            UUID uniqueId = UUID.fromString((String) ownerUuid);
            return Bukkit.getPlayer(uniqueId);
        } catch (Throwable ignored) {
            return null;
        }
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
