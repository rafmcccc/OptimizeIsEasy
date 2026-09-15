<div align="center">

# OptimizeIsEasy

**One jar. Zero waste. Your server just breathes.**

*Hibernate when empty. Clean when messy. Smart when stressed. Tuned at the config level.*

[![Paper 1.21.x](https://img.shields.io/badge/Paper-1.21.x-blue?style=flat-square)](https://papermc.io)
[![Purpur](https://img.shields.io/badge/Purpur-supported-blue?style=flat-square)](#)
[![Folia](https://img.shields.io/badge/Folia-supported-blue?style=flat-square)](#)
[![Java 21](https://img.shields.io/badge/Java-21-orange?style=flat-square)](https://adoptium.net)
[![Modules 17](https://img.shields.io/badge/Modules-17-purple?style=flat-square)](#features)
[![License MIT](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](#license)

</div>

> Runtime lag defense, server-file tuning, exploit patching and world-border control in one plugin. Your server sleeps when empty, stays fast when full, and ships with hardened configs out of the box.

---

## Why you will love it

**It sleeps when nobody is there.**  
Empty server means frozen ticks. No entities ticking. No daylight cycle. Just your server idling, using almost no CPU. A player joins and it wakes up instantly. Your host bill will notice.

**It cleans without you asking.**  
Ground items, stray mobs, projectile spam. They pile up and your TPS drops. OptimizeIsEasy sweeps them on schedule, and you can force a sweep with one command when you need it now.

**It stays smart when TPS drops.**  
When your server starts to choke, it does not just watch. It throttles hoppers, caps redstone, trims explosions, and lowers view distance just enough to keep you above 19 TPS. No manual panic.

**It tunes your server files.**  
One command applies a full optimisation profile to `server.properties`, `bukkit.yml`, `spigot.yml`, Paper, Purpur and Pufferfish configs. Originals are backed up first, and a restart applies everything.

---

## Features

### Hibernate
Freeze ticks when empty. Checks every 60 seconds if someone unfroze it manually. Optional GC on freeze to reclaim memory. No extra config needed.

### WorldCleaner
Removes items, creatures and projectiles based on your lists. Interval, blacklist, time lived all configurable. Quiet by default, no chat spam.

### EntityLimiter
Hard limits per chunk. No more 500 chickens in one chunk killing everyone nearby. Whitelist your villagers and dragons.

### HopperOptimizer
Location-tracked throttling. Full hoppers wait. Chunk hopper caps stop lag machines. Survives reloads by rescanning loaded chunks.

### LagShield
Your emergency brake. TPS thresholds disable spawns, hoppers, redstone, projectiles, leaf decay and more when TPS dips. Dynamic view, simulation and tick speed adjust automatically.

### RedstoneLimiter
1100 redstone ticks per chunk per second is enough. Pistons capped too. Optional break on abuse.

### ExplosionOptimizer
Caps TNT, creeper, crystal and fireball yield. Stops chain reactions from nuking your world and your TPS.

### AFKOptimizer
Detects AFK by position or rotation. Kick, teleport to an AFK world, or just hide entities to save bandwidth. You choose.

### ServerTuner (KOS)
Applies curated optimisation profiles to your server files. Four profiles ship with the jar: `YouHaveTrouble.kos`, `FarmFriendly.kos`, `Balanced.kos`, `LowEnd.kos`. Every write is type-checked, skipped keys are reported, and originals are backed up to `plugins/OptimizeIsEasy/backups/`.

### ExploitDB (EDB)
Checks and patches 12 known server-file exploits, from armour-stand lag machines to impersonation. See the table below. Restart required after patching.

### BorderControl
Full world-border manager with an in-game GUI. Size presets, center-to-player, timed shrink, damage and warning distance. Per world. Open with `/border`.

### And more
MobAiReducer, InstantLeafDecay, ConsoleFilter, TrashDisposal, VehicleMotionReducer, AbilityLimiter for elytra and trident spam. All toggleable. All with their own `modules/*.yml`.

You get 17 modules. Enable what you need. Disable what you do not.

---

## Exploit database

| ID | Exploit | Fix |
|----|---------|-----|
| EDB-1 | Armour stand lag machines | Disables armour-stand ticking and collision lookups |
| EDB-2 | Book exploits | Caps book page size at 1024 |
| EDB-3 | Collision lag machines | `max-entity-collisions: 2` + climbing/cramming fix |
| EDB-4 | Command suggestion packet spam | Packet limiter DROP 1.0s / 15.0 rate |
| EDB-5 | Command spam | Clears spigot spam exclusions |
| EDB-6 | Join spam | `max-joins-per-tick: 3` |
| EDB-7 | Neighbor update lag machines | `max-chained-neighbor-updates: 10000` |
| EDB-8 | Projectile suspension | Per-chunk save limit 8 for projectiles |
| EDB-9 | Recipe book spam | Packet limiter DROP 4.0s / 5.0 rate |
| EDB-10 | Nether roof access | Nether ceiling void damage at y=127 |
| EDB-11 | Xray | Enables anti-xray |
| EDB-12 | Impersonation | Forces `online-mode=true` — **never patch while behind a BungeeCord/Velocity proxy** (the plugin refuses and tells you why) |

Run `/exploitfix check` to audit, `/exploitfix patch <id|all>` to fix.

---

## Performance

We kept it lean on purpose.

* **No heavy deps.** Paper API only at compile time. The old Foundry/inventorygui/FoliaLib stack is gone; GUIs are native Bukkit.
* **Compile only Paper API.** No runtime shade required.
* **Two log lines.** One on enable, one on disable. Everything else is `debug: false` by default. Set it true only when you need it.

---

## Commands

### /optimize - performance and tuning

Permission `optimizeiseasy.optimize` (alias `rafmc.optimize`, default op)

```
/optimize status
  TPS, MSPT, entities, hibernation, modules, pending EDB failures

/optimize now
  Manual purge using WorldCleaner rules. Runs GC if enabled.

/optimize toggle
  Toggles hibernation. Will not freeze if players are online.

/optimize toggle <module>
  Toggles one module, like WorldCleaner or LagShield. Saves to modules/<module>.yml

/optimize kos [profile] [true|false]
  Applies a ServerTuner profile to your server files. Backs up originals.
  Without args runs kos.default-profile when kos.world-is-pregenerated is set (1=yes, 2=no).

/optimize edb <check|patch <id|all>>
  Same checks and patches as /exploitfix.

/optimize border
  Opens the BorderControl GUI for your current world.

/optimize report
  Full diagnostic: TPS, modules, software configs, EDB state, conflicting plugins.

/optimize reload | gui | version
```

Tab completes subcommands, module names, profile names and EDB ids.

### /exploitfix - protections

Permission `optimizeiseasy.exploitfix` (alias `rafmc.exploitfix`, default op)

```
/exploitfix status
  Active protections, hibernation, EDB summary, restart flag

/exploitfix list
  The full EDB-1…EDB-12 table with live pass/fail state

/exploitfix toggle <name>
  Enable or disable one module. Saves to file.

/exploitfix check
  Audit all 12 exploits

/exploitfix patch <id|all>
  Patch and flag restart required
```

### /border - world border

Permission `optimizeiseasy.border` (alias `border.admin`, default op). Opens the border GUI. Same as `/optimize border`.

---

## Permissions

```
optimizeiseasy.optimize   - use /optimize  (default op)
optimizeiseasy.exploitfix - use /exploitfix (default op)
optimizeiseasy.border     - use /border    (default op)
border.admin              - alias for optimizeiseasy.border
rafmc.optimize            - alias, same as above
rafmc.exploitfix          - alias, same as above
```

---

## Config

**config.yml**

```yaml
debug: false

hibernate:
  gc-on-freeze: true
  check-interval-seconds: 60
  messages:
    frozen-on-empty: "Last player disconnected. Server is now frozen."

kos:
  override-pregenerated-world-protections: false
  using-tcpshield: false
  world-is-pregenerated: 0  # 1=yes, 2=no, 0=ask every time
  default-profile: YouHaveTrouble.kos

main:
  prefix: "&8[&a&l✓&8] "
  prefix_hover: false
  config_formatter: true
  monitor:
    resource:
      enabled: true
      interval: 5
    worlds:
      enabled: true
      interval: 30
    map:
      enabled: false
      interval: 3
  errors_reporter: false
  updater: false
  warnings: false
```

More settings live in `plugins/OptimizeIsEasy/modules/*.yml` (17 files). KOS profiles live in `plugins/OptimizeIsEasy/profiles/*.kos`. Server-file backups land in `plugins/OptimizeIsEasy/backups/`.

---

## Installation

1. Drop the jar into `plugins/`
2. Restart your Paper 1.21.x server. Works on Paper, Purpur and Folia plus any Paper fork
3. Run `/optimize status` and `/exploitfix check` to see where you stand

That is it. No extra setup.

---

## Building from source

```
git clone <your repo>
cd OptimizeIsEasy/1.21.x
./gradlew clean build
```

Jar goes to `1.21.x/build/libs/OptimizeIsEasy-1.0.0.jar`.

Requires Java 21, Paper API 1.21.11. Runs on Paper, Purpur, Folia and Paper forks. No Paperweight, no NMS toolchain.

---

## FAQ

**My server still lags, will this fix it?**  
It helps a lot, but it will not fix a bad plugin that leaks memory or spawns 10k entities every second. Fix that plugin first. `/optimize report` flags known conflicting plugins.

**Can I keep only hibernate and disable everything else?**  
Yes. Set every module `enabled: false` except Hibernate, or just run `/optimize toggle WorldCleaner` etc.

**Where is /abyss and /trash?**  
Removed to keep the command surface small. Abyss storage still has a config but no command. Set `WorldCleaner.items.abyss.enabled: false` if you do not need it.

**I have a stale frozen.yml from an old version.**  
Player freezing was removed. The file is inert and safe to delete.

**Should I patch EDB-12?**  
Only if players join directly, not through BungeeCord/Velocity. The plugin refuses to apply it on proxied setups.

---

## License

MIT License. See [LICENSE](LICENSE).

You can use, modify and sell it. Just keep the copyright notice.

---

## Credits

Built by rafmcccc for Paper 1.21 and Paper forks. Designed for Paper, Purpur and Folia.

Server tuning and exploit checks are ports of [Kryptonite](https://github.com/LewMC/Kryptonite) by LewMC (Apache-2.0). World-border GUI is a port of Border by Gab. Thanks to YouHaveTrouble for the optimisation guide behind the default profile.
