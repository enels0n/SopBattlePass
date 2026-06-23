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

/**
 * Единый рефлективный хук для собственных событий экосистемы Sop*.
 * Каждый плагин публикует своё событие (см. контракт в README), а здесь мы
 * мягко (через рефлексию) подписываемся на него и пишем прогресс миссий.
 * Хук не имеет жёстких зависимостей: если плагина/события нет — подписка просто пропускается.
 */
public final class SopEcosystemHook implements Listener {

    private final SopBattlePass plugin;
    private final MissionService missionService;
    private boolean registered;

    public SopEcosystemHook(SopBattlePass plugin, MissionService missionService) {
        this.plugin = plugin;
        this.missionService = missionService;
    }

    public boolean register() {
        boolean any = false;
        any |= registerSopAFKZone();
        any |= registerSopAfterworld();
        any |= registerSopAnimals();
        any |= registerSopCamera();
        any |= registerSopCrates();
        any |= registerSopCustomTNT();
        any |= registerSopElevators();
        any |= registerSopExpCanning();
        any |= registerSopMachines();
        any |= registerSopMeals();
        any |= registerSopPlants();
        any |= registerSopSafe();
        any |= registerSopScrolls();
        any |= registerSopTNTRun();
        any |= registerSopUpgradablePickaxe();
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

    // ===================== SopAFKZone =====================
    private boolean registerSopAFKZone() {
        ClassLoader cl = enabledLoader("sopafkzone", "SopAFKZone");
        if (cl == null) {
            return false;
        }
        return registerEvent(cl, "net.enelson.sopafkzone.event.AfkZoneCompleteEvent", event -> {
            Player player = player(event);
            if (player != null) {
                missionService.recordCustomProgress(player, "SOPAFKZONE_COMPLETE", 1);
            }
        });
    }

    // ===================== SopAfterworld =====================
    private boolean registerSopAfterworld() {
        ClassLoader cl = enabledLoader("sopafterworld", "SopAfterworld");
        if (cl == null) {
            return false;
        }
        return registerEvent(cl, "net.enelson.sopafterworld.event.AfterworldEnterEvent", event -> {
            Player player = player(event);
            if (player != null) {
                missionService.recordCustomProgress(player, "SOPAFTERWORLD_ENTER", 1);
            }
        });
    }

    // ===================== SopAnimals =====================
    private boolean registerSopAnimals() {
        ClassLoader cl = enabledLoader("sopanimals", "SopAnimals");
        if (cl == null) {
            return false;
        }
        boolean any = false;
        any |= registerEvent(cl, "net.enelson.sopanimals.event.AnimalTameEvent", event -> {
            Player player = player(event);
            if (player == null) {
                return;
            }
            missionService.recordCustomProgress(player, "SOPANIMALS_TAME", 1);
            recordTyped(player, "SOPANIMALS_TAME", optionalString(event, "getAnimalType"));
        });
        any |= registerEvent(cl, "net.enelson.sopanimals.event.AnimalFeedEvent", event -> {
            Player player = player(event);
            if (player == null) {
                return;
            }
            missionService.recordCustomProgress(player, "SOPANIMALS_FEED", 1);
            recordTyped(player, "SOPANIMALS_FEED", optionalString(event, "getAnimalType"));
        });
        any |= registerEvent(cl, "net.enelson.sopanimals.event.AnimalBirthEvent", event -> {
            Player owner = playerFrom(event, "getOwner");
            if (owner == null) {
                return;
            }
            missionService.recordCustomProgress(owner, "SOPANIMALS_BIRTH", 1);
            recordTyped(owner, "SOPANIMALS_BIRTH", optionalString(event, "getAnimalType"));
        });
        any |= registerEvent(cl, "net.enelson.sopanimals.event.AnimalDeathEvent", event -> {
            Player owner = playerFrom(event, "getOwner");
            if (owner == null) {
                return;
            }
            missionService.recordCustomProgress(owner, "SOPANIMALS_DEATH", 1);
            String cause = optionalString(event, "getCause");
            if (cause != null && !cause.isEmpty()) {
                missionService.recordCustomProgress(owner, "SOPANIMALS_DEATH_" + normalizeKey(cause), 1);
            }
        });
        any |= registerEvent(cl, "net.enelson.sopanimals.event.AnimalKillEvent", event -> {
            Player player = player(event);
            if (player == null) {
                return;
            }
            missionService.recordCustomProgress(player, "SOPANIMALS_KILL", 1);
            recordTyped(player, "SOPANIMALS_KILL", optionalString(event, "getAnimalType"));
        });
        return any;
    }

    // ===================== SopCamera =====================
    private boolean registerSopCamera() {
        ClassLoader cl = enabledLoader("sopcamera", "SopCamera");
        if (cl == null) {
            return false;
        }
        return registerEvent(cl, "net.enelson.sopcamera.event.PhotoTakeEvent", event -> {
            Player player = player(event);
            if (player != null) {
                missionService.recordCustomProgress(player, "SOPCAMERA_PHOTO", 1);
            }
        });
    }

    // ===================== SopCrates =====================
    private boolean registerSopCrates() {
        ClassLoader cl = enabledLoader("sopcrates", "SopCrates");
        if (cl == null) {
            return false;
        }
        return registerEvent(cl, "net.enelson.sopcrates.event.CrateOpenEvent", event -> {
            Player player = player(event);
            if (player == null) {
                return;
            }
            missionService.recordCustomProgress(player, "SOPCRATES_OPEN", 1);
            recordTyped(player, "SOPCRATES_OPEN", optionalString(event, "getCrateId"));
        });
    }

    // ===================== SopCustomTNT =====================
    private boolean registerSopCustomTNT() {
        ClassLoader cl = enabledLoader("sopcustomtnt", "SopCustomTNT");
        if (cl == null) {
            return false;
        }
        return registerEvent(cl, "net.enelson.sopcustomtnt.event.CustomTntExplodeEvent", event -> {
            Player player = player(event);
            if (player == null) {
                return;
            }
            missionService.recordCustomProgress(player, "SOPCUSTOMTNT_EXPLODE", 1);
            recordTyped(player, "SOPCUSTOMTNT_EXPLODE", optionalString(event, "getTntId"));
        });
    }

    // ===================== SopElevators =====================
    private boolean registerSopElevators() {
        ClassLoader cl = enabledLoader("sopelevators", "SopElevators");
        if (cl == null) {
            return false;
        }
        return registerEvent(cl, "net.enelson.sopelevators.event.ElevatorMoveEvent", event -> {
            Player player = player(event);
            if (player == null) {
                return;
            }
            missionService.recordCustomProgress(player, "SOPELEVATORS_MOVE", 1);
            String direction = optionalString(event, "getDirection");
            if (direction != null && !direction.isEmpty()) {
                missionService.recordCustomProgress(player, "SOPELEVATORS_" + normalizeKey(direction), 1);
            }
        });
    }

    // ===================== SopExpCanning =====================
    private boolean registerSopExpCanning() {
        ClassLoader cl = enabledLoader("sopexpcanning", "SopExpCanning");
        if (cl == null) {
            return false;
        }
        return registerEvent(cl, "net.enelson.sopexpcanning.event.ExpCanEvent", event -> {
            Player player = player(event);
            if (player != null) {
                missionService.recordCustomProgress(player, "SOPEXPCANNING_CAN", 1);
            }
        });
    }

    // ===================== SopMachines =====================
    private boolean registerSopMachines() {
        ClassLoader cl = enabledLoader("sopmachines", "SopMachines");
        if (cl == null) {
            return false;
        }
        boolean any = false;
        any |= registerEvent(cl, "net.enelson.sopmachines.event.MachineCraftEvent", event -> {
            Player player = player(event);
            if (player == null) {
                return;
            }
            missionService.recordCustomProgress(player, "SOPMACHINES_CRAFT", 1);
            recordTyped(player, "SOPMACHINES_CRAFT", optionalString(event, "getMachineType"));
        });
        any |= registerEvent(cl, "net.enelson.sopmachines.event.MachineRuinEvent", event -> {
            Player player = player(event);
            if (player == null) {
                return;
            }
            missionService.recordCustomProgress(player, "SOPMACHINES_RUIN", 1);
            recordTyped(player, "SOPMACHINES_RUIN", optionalString(event, "getMachineType"));
        });
        return any;
    }

    // ===================== SopMeals =====================
    private boolean registerSopMeals() {
        ClassLoader cl = enabledLoader("sopmeals", "SopMeals");
        if (cl == null) {
            return false;
        }
        return registerEvent(cl, "net.enelson.sopmeals.event.MealComboEvent", event -> {
            Player player = player(event);
            if (player == null) {
                return;
            }
            missionService.recordCustomProgress(player, "SOPMEALS_COMBO", 1);
            recordTyped(player, "SOPMEALS_COMBO", optionalString(event, "getComboId"));
        });
    }

    // ===================== SopPlants =====================
    private boolean registerSopPlants() {
        ClassLoader cl = enabledLoader("sopplants", "SopPlants");
        if (cl == null) {
            return false;
        }
        return registerEvent(cl, "net.enelson.sopplants.event.PlantActionEvent", event -> {
            Player player = player(event);
            if (player == null) {
                return;
            }
            String action = optionalString(event, "getAction");
            if (action == null || action.isEmpty()) {
                return;
            }
            String actionKey = normalizeKey(action);
            missionService.recordCustomProgress(player, "SOPPLANTS_" + actionKey, 1);
            String plantId = optionalString(event, "getPlantId");
            if (plantId != null && !plantId.isEmpty()) {
                missionService.recordCustomProgress(player, "SOPPLANTS_" + actionKey + "_" + normalizeKey(plantId), 1);
            }
        });
    }

    // ===================== SopSafe =====================
    private boolean registerSopSafe() {
        ClassLoader cl = enabledLoader("sopsafe", "SopSafe");
        if (cl == null) {
            return false;
        }
        return registerEvent(cl, "net.enelson.sopsafe.event.SafeCreateEvent", event -> {
            Player player = player(event);
            if (player != null) {
                missionService.recordCustomProgress(player, "SOPSAFE_CREATE", 1);
            }
        });
    }

    // ===================== SopScrolls =====================
    private boolean registerSopScrolls() {
        ClassLoader cl = enabledLoader("sopscrolls", "SopScrolls");
        if (cl == null) {
            return false;
        }
        return registerEvent(cl, "net.enelson.sopscrolls.event.ScrollUseEvent", event -> {
            Player player = player(event);
            if (player == null) {
                return;
            }
            missionService.recordCustomProgress(player, "SOPSCROLLS_USE", 1);
            recordTyped(player, "SOPSCROLLS_USE", optionalString(event, "getScrollId"));
        });
    }

    // ===================== SopTNTRun =====================
    private boolean registerSopTNTRun() {
        ClassLoader cl = enabledLoader("soptntrun", "SopTNTRun");
        if (cl == null) {
            return false;
        }
        return registerEvent(cl, "net.enelson.soptntrun.event.TntRunFinishEvent", event -> {
            Player player = player(event);
            if (player == null) {
                return;
            }
            Object winner = optional(event, "isWinner");
            boolean isWinner = winner instanceof Boolean && (Boolean) winner;
            missionService.recordCustomProgress(player, isWinner ? "SOPTNTRUN_WIN" : "SOPTNTRUN_LOSE", 1);
        });
    }

    // ===================== SopUpgradablePickaxe =====================
    private boolean registerSopUpgradablePickaxe() {
        ClassLoader cl = enabledLoader("sopupgradablepickaxe", "SopUpgradablePickaxe");
        if (cl == null) {
            return false;
        }
        return registerEvent(cl, "net.enelson.sopupgradablepickaxe.event.PickaxeUpgradeEvent", event -> {
            Player player = player(event);
            if (player != null) {
                missionService.recordCustomProgress(player, "SOPUPGRADABLEPICKAXE_UPGRADE", 1);
            }
        });
    }

    // ===================== Общие помощники =====================

    /** Возвращает classloader плагина, если интеграция включена в конфиге и плагин активен; иначе null. */
    private ClassLoader enabledLoader(String configKey, String pluginName) {
        if (!plugin.getConfig().getBoolean("integrations." + configKey + ".enabled", true)) {
            return null;
        }
        Plugin target = Bukkit.getPluginManager().getPlugin(pluginName);
        if (target == null || !target.isEnabled()) {
            return null;
        }
        return target.getClass().getClassLoader();
    }

    private void recordTyped(Player player, String base, String type) {
        if (type != null && !type.trim().isEmpty()) {
            missionService.recordCustomProgress(player, base + "_" + normalizeKey(type), 1);
        }
    }

    private Player player(Event event) {
        return playerFrom(event, "getPlayer");
    }

    private Player playerFrom(Event event, String methodName) {
        try {
            Object result = event.getClass().getMethod(methodName).invoke(event);
            return result instanceof Player ? (Player) result : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String optionalString(Event event, String methodName) {
        Object value = optional(event, methodName);
        return value == null ? null : String.valueOf(value);
    }

    private Object optional(Event event, String methodName) {
        try {
            return event.getClass().getMethod(methodName).invoke(event);
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
            // Событие отсутствует (старая версия плагина) — это нормально, просто пропускаем.
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
