# SopBattlePass

Seasonal battle pass plugin scaffold for the Sop ecosystem.

Current foundation:
- shared network-aware player progress
- SQLite or MySQL via SopLib
- season loading from resource files
- player GUI entry point
- PlaceholderAPI expansion
- admin commands for reload, xp, premium, and random rerolls
- daily, weekly, and global mission pools with survival-first starter content
- progress tracking for gameplay missions, playtime, and smelting
- reward claims per level and backend server id
- placeholder-based mission conditions (`all/any` checks)
- GUI item creation through SopLib with CustomModelData-ready config
- admin-configurable forced daily slots
- admin custom progress bridge through command input
- reflective soft hook for BreweryX custom progress
- reflective soft hook for SopEnvoy custom progress
- reflective soft hook for SopRegionCore custom progress
- reflective soft hook for CustomFishing custom progress
- reflective soft hook for PinataParty custom progress
- reflective soft hook for the wider Sop* ecosystem (SopAFKZone, SopAfterworld, SopAnimals, SopCamera, SopCrates, SopCustomTNT, SopElevators, SopExpCanning, SopMachines, SopMeals, SopPlants, SopSafe, SopScrolls, SopTNTRun, SopUpgradablePickaxe)

This first pass intentionally focuses on the durable core:
- config model
- storage model
- season/xp model
- extensible mission and reward structure

Next implementation layers:
- more trigger types such as smelting and custom external progress hooks
- plugin-facing API for external custom progress publishers
- deeper GUI configuration coverage for all menu surfaces
- richer reward previews and claimed-state visuals
- production testing on multi-server MySQL setups

Current PinataParty hook targets:
- `PINATAPARTY_HIT`
- `PINATAPARTY_KILL`
- `PINATAPARTY_POOL_DEPOSIT`

Current CustomFishing hook targets:
- `CUSTOMFISHING_CAST`
- `CUSTOMFISHING_RESULT`
- `CUSTOMFISHING_RESULT_SUCCESS`
- `CUSTOMFISHING_RESULT_FAILURE`
- `CUSTOMFISHING_LOOT`
- `CUSTOMFISHING_LOOT_<LOOT_ID>`

Current BreweryX hook targets:
- `BREWERYX_DRINK`
- `BREWERYX_DRINK_<RECIPE_ID>`
- `BREWERYX_MODIFY_<TYPE>`
- `BREWERYX_INGREDIENT`
- `BREWERYX_INGREDIENT_<CONFIG_ID>`

Current SopEnvoy hook targets:
- `SOPENVOY_OPEN`
- `SOPENVOY_OPEN_<CRATE_TYPE>`

Current SopRegionCore hook targets:
- `SOPREGIONCORE_PLACE`
- `SOPREGIONCORE_START`
- `SOPREGIONCORE_START_<CORE_TYPE>`
- `SOPREGIONCORE_MEMBER_ADD`
- `SOPREGIONCORE_STATE_<STATE>`

Current Sop* ecosystem hook targets (managed by `SopEcosystemHook`, each toggle under `integrations.<plugin>.enabled`):
- SopAFKZone: `SOPAFKZONE_COMPLETE`
- SopAfterworld: `SOPAFTERWORLD_ENTER`
- SopAnimals: `SOPANIMALS_TAME`, `SOPANIMALS_TAME_<TYPE>`, `SOPANIMALS_FEED`, `SOPANIMALS_FEED_<TYPE>`, `SOPANIMALS_BIRTH`, `SOPANIMALS_BIRTH_<TYPE>`, `SOPANIMALS_DEATH`, `SOPANIMALS_DEATH_HUNGER`, `SOPANIMALS_DEATH_OLD_AGE`
- SopCamera: `SOPCAMERA_PHOTO`
- SopCrates: `SOPCRATES_OPEN`, `SOPCRATES_OPEN_<CRATE_ID>`
- SopCustomTNT: `SOPCUSTOMTNT_EXPLODE`, `SOPCUSTOMTNT_EXPLODE_<TNT_ID>` (requires an igniter player; currently fired with no player, so progress is only credited if the plugin later tracks the responsible player)
- SopElevators: `SOPELEVATORS_MOVE`, `SOPELEVATORS_UP`, `SOPELEVATORS_DOWN`
- SopExpCanning: `SOPEXPCANNING_CAN`
- SopMachines: `SOPMACHINES_CRAFT`, `SOPMACHINES_CRAFT_<TYPE>`, `SOPMACHINES_RUIN`, `SOPMACHINES_RUIN_<TYPE>`
- SopMeals: `SOPMEALS_COMBO`, `SOPMEALS_COMBO_<COMBO_ID>`
- SopPlants: `SOPPLANTS_PLANT`, `SOPPLANTS_WATER`, `SOPPLANTS_HARVEST`, `SOPPLANTS_BREAK` (each also with a `_<PLANT>` suffixed variant)
- SopSafe: `SOPSAFE_CREATE`
- SopScrolls: `SOPSCROLLS_USE`, `SOPSCROLLS_USE_<SCROLL_ID>`
- SopTNTRun: `SOPTNTRUN_WIN`, `SOPTNTRUN_LOSE`
- SopUpgradablePickaxe: `SOPUPGRADABLEPICKAXE_UPGRADE`

Event contract: each plugin publishes a Bukkit event under `net.enelson.<plugin>.event.*` exposing `getPlayer()` (or `getOwner()` for SopAnimals birth/death) plus the type/id getters used above. `SopEcosystemHook` subscribes reflectively, so missing plugins/events are simply skipped.
