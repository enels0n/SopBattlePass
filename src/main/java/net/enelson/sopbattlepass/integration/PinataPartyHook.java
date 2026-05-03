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

public final class PinataPartyHook implements Listener {

    private final SopBattlePass plugin;
    private final MissionService missionService;
    private boolean registered;

    public PinataPartyHook(SopBattlePass plugin, MissionService missionService) {
        this.plugin = plugin;
        this.missionService = missionService;
    }

    public boolean register() {
        if (!plugin.getConfig().getBoolean("integrations.pinata-party.enabled", true)) {
            return false;
        }
        Plugin pinataParty = Bukkit.getPluginManager().getPlugin("PinataParty");
        if (pinataParty == null || !pinataParty.isEnabled()) {
            return false;
        }

        ClassLoader classLoader = pinataParty.getClass().getClassLoader();
        boolean any = false;
        any |= registerEvent(classLoader, "me.hexedhero.pp.api.PinataHitEvent", new ReflectiveEventConsumer() {
            @Override
            public void accept(Event event) throws Exception {
                Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);
                if (player != null) {
                    missionService.recordCustomProgress(player, "PINATAPARTY_HIT", 1);
                }
            }
        });
        any |= registerEvent(classLoader, "me.hexedhero.pp.api.PinataDieEvent", new ReflectiveEventConsumer() {
            @Override
            public void accept(Event event) throws Exception {
                Object pinata = event.getClass().getMethod("getPinata").invoke(event);
                if (pinata == null) {
                    return;
                }
                Method getLastHitter = pinata.getClass().getMethod("getLastHitter");
                Player player = (Player) getLastHitter.invoke(pinata);
                if (player != null) {
                    missionService.recordCustomProgress(player, "PINATAPARTY_KILL", 1);
                }
            }
        });
        any |= registerEvent(classLoader, "me.hexedhero.pp.api.PinataPoolDepositEvent", new ReflectiveEventConsumer() {
            @Override
            public void accept(Event event) throws Exception {
                Method getPlayer = event.getClass().getMethod("getPlayer");
                Method getMoney = event.getClass().getMethod("getMoney");
                Player player = (Player) getPlayer.invoke(event);
                Number money = (Number) getMoney.invoke(event);
                if (player != null && money != null) {
                    long value = Math.max(0L, money.longValue());
                    if (value > 0L) {
                        int amount = value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
                        missionService.recordCustomProgress(player, "PINATAPARTY_POOL_DEPOSIT", amount);
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

    private interface ReflectiveEventConsumer {
        void accept(Event event) throws Exception;
    }
}
