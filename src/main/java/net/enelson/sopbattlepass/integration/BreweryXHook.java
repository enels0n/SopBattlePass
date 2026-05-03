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

public final class BreweryXHook implements Listener {

    private final SopBattlePass plugin;
    private final MissionService missionService;
    private boolean registered;

    public BreweryXHook(SopBattlePass plugin, MissionService missionService) {
        this.plugin = plugin;
        this.missionService = missionService;
    }

    public boolean register() {
        if (!plugin.getConfig().getBoolean("integrations.breweryx.enabled", true)) {
            return false;
        }
        Plugin brewery = Bukkit.getPluginManager().getPlugin("BreweryX");
        if (brewery == null || !brewery.isEnabled()) {
            return false;
        }

        ClassLoader classLoader = brewery.getClass().getClassLoader();
        boolean any = false;
        any |= registerEvent(classLoader, "com.dre.brewery.api.events.brew.BrewDrinkEvent", new ReflectiveEventConsumer() {
            @Override
            public void accept(Event event) throws Exception {
                Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);
                if (player == null) {
                    return;
                }
                missionService.recordCustomProgress(player, "BREWERYX_DRINK", 1);
                Object brew = event.getClass().getMethod("getBrew").invoke(event);
                String recipeId = extractRecipeId(brew);
                if (!recipeId.isEmpty()) {
                    missionService.recordCustomProgress(player, "BREWERYX_DRINK_" + recipeId, 1);
                }
            }
        });
        any |= registerEvent(classLoader, "com.dre.brewery.api.events.brew.BrewModifyEvent", new ReflectiveEventConsumer() {
            @Override
            public void accept(Event event) throws Exception {
                Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);
                if (player == null) {
                    return;
                }
                Object type = event.getClass().getMethod("getType").invoke(event);
                if (type != null) {
                    missionService.recordCustomProgress(player, "BREWERYX_MODIFY_" + normalizeKey(type.toString()), 1);
                }
            }
        });
        any |= registerEvent(classLoader, "com.dre.brewery.api.events.IngedientAddEvent", new ReflectiveEventConsumer() {
            @Override
            public void accept(Event event) throws Exception {
                Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);
                if (player == null) {
                    return;
                }
                missionService.recordCustomProgress(player, "BREWERYX_INGREDIENT", 1);
                Object recipeItem = event.getClass().getMethod("getRecipeItem").invoke(event);
                if (recipeItem != null) {
                    Object configId = recipeItem.getClass().getMethod("getConfigId").invoke(recipeItem);
                    if (configId != null && !String.valueOf(configId).trim().isEmpty()) {
                        missionService.recordCustomProgress(player, "BREWERYX_INGREDIENT_" + normalizeKey(String.valueOf(configId)), 1);
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

    private String extractRecipeId(Object brew) {
        if (brew == null) {
            return "";
        }
        try {
            Object recipe = brew.getClass().getMethod("getCurrentRecipe").invoke(brew);
            if (recipe == null) {
                return "";
            }
            Object id = recipe.getClass().getMethod("getId").invoke(recipe);
            return id == null ? "" : normalizeKey(String.valueOf(id));
        } catch (Exception ignored) {
            return "";
        }
    }

    private String normalizeKey(String input) {
        return input == null ? "" : input.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private interface ReflectiveEventConsumer {
        void accept(Event event) throws Exception;
    }
}
