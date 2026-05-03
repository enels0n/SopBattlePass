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
- reflective soft hook for AxEnvoy custom progress
- reflective soft hook for CustomFishing custom progress
- reflective soft hook for PinataParty custom progress

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

Current AxEnvoy hook targets:
- `AXENVOY_COLLECT`
- `AXENVOY_COLLECT_<ENVOY_NAME>`
- `AXENVOY_CRATE_<CRATE_NAME>`
- `AXENVOY_REWARD_<REWARD_NAME>`
