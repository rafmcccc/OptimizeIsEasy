<div align="center">

# OptimizeIsEasy

**One jar. Zero waste. Your server just breathes.**

*Hibernate when empty. Clean when messy. Smart when stressed.*

[![Paper 1.21+](https://img.shields.io/badge/Paper-1.21%2B-blue?style=flat-square)](https://papermc.io)
[![Purpur](https://img.shields.io/badge/Purpur-supported-blue?style=flat-square)](#)
[![Folia](https://img.shields.io/badge/Folia-supported-blue?style=flat-square)](#)
[![Java 21](https://img.shields.io/badge/Java-21-orange?style=flat-square)](https://adoptium.net)
[![Size 72 KB](https://img.shields.io/badge/Size-72%20KB-green?style=flat-square)](#)
[![Modules 14](https://img.shields.io/badge/Modules-14-purple?style=flat-square)](#features)
[![License MIT](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](#license)

</div>

> One plugin that does both, and does it cleaner. Your server sleeps when empty and stays fast when full. No extra setup, no spam.

---

## Why you will love it

**It sleeps when nobody is there.**  
Empty server means zero ticks per second. No entities ticking. No daylight cycle. Just your server idling, using almost no CPU. A player joins and it wakes up instantly. Your host bill will notice.

**It cleans without you asking.**  
Ground items, stray mobs, projectile spam. They pile up and your TPS drops. OptimizeIsEasy sweeps them on schedule, and you can force a sweep with one command when you need it now.

**It stays smart when TPS drops.**  
When your server starts to choke, it does not just watch. It throttles hoppers, caps redstone, trims explosions, and lowers view distance just enough to keep you above 19 TPS. No manual panic.

---

## Features

### Hibernate
Freeze ticks when empty. Checks every 60 seconds if someone unfroze it manually. Optional GC on freeze to reclaim memory. No extra config needed.

### WorldCleaner
Removes items, creatures and projectiles based on your lists. Interval, blacklist, time lived all configurable. Quiet by default, no chat spam.

### EntityLimiter
Hard limits per chunk. No more 500 chickens in one chunk killing everyone nearby. Whitelist your villagers and dragons.

### HopperOptimizer
Smart throttling. Empty hoppers sleep. Full hoppers wait. Chunk hopper caps stop lag machines.

### LagShield
Your emergency brake. TPS thresholds disable spawns, hoppers, redstone, projectiles, leaf decay and more when TPS dips. Dynamic view, simulation and tick speed adjust automatically.

### RedstoneLimiter
1100 redstone ticks per chunk per second is enough. Pistons capped too. Optional break on abuse.

### ExplosionOptimizer
Caps TNT, creeper, crystal and fireball yield. Stops chain reactions from nuking your world and your TPS.

### AFKOptimizer
Detects AFK by position or rotation. Kick, teleport to an AFK world, or just hide entities to save bandwidth. You choose.

### And more
MobAiReducer, InstantLeafDecay, ConsoleFilter, TrashDisposal, VehicleMotionReducer, AbilityLimiter for elytra and trident spam. All toggleable. All with their own `modules/*.yml`.

You get 14 modules. Enable what you need. Disable what you do not.

---

## Performance

We kept it tiny on purpose.

* **72 KB jar.** No shadowed megabytes, no NMS bundles, no heavy deps.
* **Compile only Paper API.** No runtime oshi or adventure shade required.
* **No headless hacks.** The old map monitor needed `java.awt.headless` and X11. Gone.
* **Two log lines.** One on enable, one on disable. Everything else is `debug: false` by default. Set it true only when you need it.

The old approach shaded everything and logged a wall of text. This does not.

---

## Commands

Only two. That is the point.

### /optimize - performance

Permission `optimizeiseasy.optimize` (alias `rafmc.optimize`, default op)

```
/optimize status
  Shows TPS, MSPT, entity count, hibernation state, enabled modules

/optimize now
  Runs a manual purge right now. Removes items, creatures and projectiles
  using WorldCleaner rules. Runs GC if enabled. Tells you how many it removed.

/optimize toggle
  Toggles hibernation. Frozen to running, or running to frozen
  Will not freeze if players are online

/optimize toggle <module>
  Toggles one module, like WorldCleaner or LagShield
  Saves to modules/<module>.yml
```

Tab completes subcommands and module names.

### /exploitfix - protections

Permission `optimizeiseasy.exploitfix` (alias `rafmc.exploitfix`, default op)

```
/exploitfix status
  How many protections are active, frozen players count

/exploitfix list
  Lists every patch that is active. EntityLimiter, RedstoneLimiter,
  AbilityLimiter, LagShield and more. Honest list, no fake CVEs

/exploitfix toggle <name>
  Enable or disable one patch. Saves to file.

/exploitfix freeze <player>
  Freeze a player for investigation. Stops movement and most commands
  Run again to unfreeze. Tab completes online players
```

Tab completes patch names and player names.

---

## Permissions

```
optimizeiseasy.optimize  - use /optimize  (default op)
optimizeiseasy.exploitfix - use /exploitfix (default op)
rafmc.optimize           - alias, same as above
rafmc.exploitfix         - alias, same as above
```

Old permission nodes are gone. Use the two above.

---

## Config

One main file, 13 module files. No surprises.

**config.yml**

```yaml
debug: false

hibernate:
  gc-on-freeze: true
  check-interval-seconds: 60
  messages:
    frozen-on-empty: "Last player disconnected. Server is now frozen."

main:
  prefix: "&8[&a✓&8] "
  prefix_hover: false
  config_formatter: true
  bStats: true
```

More settings live in `plugins/OptimizeIsEasy/modules/*.yml`. Each module has `enabled` and `worlds`. Edit what you need, leave the rest.

---

## Installation

1. Drop `OptimizeIsEasy-1.0.0.jar` into `plugins/`
2. Restart your Paper 1.21+ server. Works on Paper, Purpur and Folia plus any Paper fork
3. Run `/optimize status` to see it is alive

That is it. No extra setup.

---

## Building from source

```
git clone <your repo>
cd OptimizeIsEasy
./gradlew clean build
```

Jar goes to `build/libs/OptimizeIsEasy-1.0.0.jar`.

Requires Java 21, Paper API 1.21.11. Runs on Paper, Purpur, Folia and Paper forks. No Paperweight, no NMS toolchain.

---

## FAQ

**Is this two plugins in one?**  
Yes, but cleaned. Same features, less spam, one config folder, two commands.

**My server still lags, will this fix it?**  
It helps a lot, but it will not fix a bad plugin that leaks memory or spawns 10k entities every second. Fix that plugin first.

**Can I keep only hibernate and disable everything else?**  
Yes. Set every module `enabled: false` except Hibernate, or just run `/optimize toggle WorldCleaner` etc.

**Where is /abyss and /trash?**  
Removed to keep only two commands. Abyss storage still has a config but no command. Set `WorldCleaner.items.abyss.enabled: false` if you do not need it.

---

## License

MIT License. See [LICENSE](LICENSE).

You can use, modify and sell it. Just keep the copyright notice.

---

## Credits

Built by rafmcccc for Paper 1.21 and Paper forks. Designed for Paper, Purpur and Folia.

---

<details>
<summary>Technical notes for reviewers</summary>

This jar merges performance and hibernation into one, same Paper and Java version, only reorganized, stronger implementation kept where needed.

* **Paper API 1.21.11** with Folia support. Uses scheduler abstraction for Paper, Purpur and Folia. Bukkit only stubs keep the jar lean and buildable without Paperweight.
* **GC on freeze** built in and folded into `/optimize now`.
* **Command aliases** `lf, antilag, lag, hibernate, abyss, trash, benchmark, map, menu, ping` removed. Only `optimize` and `exploitfix` remain.
* **Console** now one line on enable and one on disable. `debug: false` gates everything else to `FINE`.
* **Build** `WorkFlow/OptimizeIsEasy` `settings.gradle.kts` `rootProject.name = "OptimizeIsEasy"` `group com.optimizeiseasy` `archiveBaseName OptimizeIsEasy` `BUILD SUCCESSFUL`.

If you need the old multi NMS build, check the git history before the merge.

</details>
