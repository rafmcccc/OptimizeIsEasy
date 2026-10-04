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
One command applies a full optimisation profile to `server.properties`, `bukkit.yml`, `spigot.yml`, the Paper global and world configs, Purpur, Pufferfish, Gale and Leaf. Originals are backed up first, and a restart applies everything.

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
Applies curated optimisation profiles to your server files. Eight profiles ship with the jar: `YouHaveTrouble.kos`, `FarmFriendly.kos`, `Balanced.kos`, `LowEnd.kos`, `HighEnd.kos`, `VanillaPlus.kos`, `LobbyGames.kos`, `Anarchy.kos`. Every write is type-checked, skipped keys are reported, and originals are backed up to `plugins/OptimizeIsEasy/backups/`. Pick a profile in the Krypton GUI and hit apply, or run `dry-run` first to see the diff without writing. Hide profiles you never want with `denied-profiles`.

A `.kos` file is one commented YAML document. Each top-level section maps to exactly one server file, and the whole section is skipped when that file does not exist, so the same profile is safe on Paper, Purpur, Pufferfish, Gale and Leaf.

| Section in the profile | Server file |
|---|---|
| `server` | `server.properties` |
| `craftbukkit` | `bukkit.yml` |
| `spigot` | `spigot.yml` |
| `paper` | `config/paper-world-defaults.yml` |
| `paper-global` | `config/paper-global.yml` |
| `purpur` | `purpur.yml` |
| `pufferfish` | `pufferfish.yml` |
| `gale` | `config/gale-global.yml` (Leaf ships this too) |
| `leaf` | `config/leaf-global.yml` |

Every profile is commented key by key, so open the file before you run it. Which one to pick:

- `YouHaveTrouble.kos` — default, solid all-rounder from the YouHaveTrouble guide.
- `Balanced.kos` — middle ground between aggressive and permissive.
- `HighEnd.kos` — powerful dedicated box, view 10, fast farms, generous caps.
- `VanillaPlus.kos` — near-vanilla SMP, safe wins only, mechanics untouched.
- `FarmFriendly.kos` — permissive, farms and redstone keep full speed.
- `LowEnd.kos` — weak hardware or busy server, tight view and spawn caps.
- `LobbyGames.kos` — lobbies, creative plots, minigames. Fast cleanup, tiny mobs.
- `Anarchy.kos` — tightest caps we ship, for high-population/grief-heavy servers.

Three rules worth knowing:

- **A missing key is never touched.** Leave it out of the profile and your server file keeps its own value.
- **`default` means "leave it alone".** Handy for per-range values you do not want to override.
- **A key your server version does not have is skipped, never invented.** Forks rename options between Minecraft versions, so the run reports how many keys were absent instead of writing something the server ignores.

Shipped profiles are only copied into `plugins/OptimizeIsEasy/profiles/` when that file does not exist yet, so **your edited profiles are never overwritten**. If you want the new keys after an update, delete the profile you want refreshed (it is re-created on the next restart) or copy the section you need out of a fresh jar.

### ExploitDB (EDB)
Checks and patches **22** known server-file exploits, from armour-stand lag machines to creative-slot NBT injection. See the table below. Restart required after patching. Patch straight from the GUI with a shift-click.

### BorderControl
Full world-border manager with an in-game GUI. Size presets, center-to-player, timed shrink, damage and warning distance. Per world. Open with `/border`.

### And more
MobAiReducer, InstantLeafDecay, ConsoleFilter, TrashDisposal, VehicleMotionReducer, AbilityLimiter for elytra and trident spam. All toggleable. All with their own `modules/*.yml`.

You get 17 modules. Enable what you need. Disable what you do not.

---

## GUIs

Three native Bukkit menus, no inventory library in the way.

| Menu | Open it with | What it does |
|------|--------------|--------------|
| **Modules** | `/optimize` or `/optimize gui` | Every module as a toggle. Bottom row holds Krypton, ExploitDB, Border, Report and version info. |
| **Krypton (KOS)** | `/optimize krypton`, `/optimize kos gui`, or the pickaxe in the module list | Eight profiles, a pre-generated yes/no picker, and an apply button. Profiles on `denied-profiles` are not listed. |
| **ExploitDB** | `/exploitfix` (or `/exploit`), `/exploitfix gui`, or the shield in the module list | All 22 exploits, two pages, with re-check, patch all, restart flag and back. |
| **Border** | `/border`, `/optimize border` | Size, center, shrink, damage, warning distance. |

**Module list clicks.** Left-click toggles. For `ServerTuner`, `ExploitDB` and `BorderControl`, left-click opens that module's GUI instead and right-click toggles it.

**ExploitDB clicks.** Left-click prints the details for that exploit in chat and changes nothing. **Shift-click is the only thing that writes to your server files.** Four item states: green already patched, red exploitable, grey not applicable to your server software, barrier disabled in config. EDB-12 stays locked while a BungeeCord or Velocity proxy is detected. EDB-12 and EDB-18 are on `patch-denylist` out of the box, so `patch all` will not touch either until you remove them from that list.

`gui.auto-open: false` in config.yml makes bare `/optimize` and `/exploitfix` print usage text instead. Console always gets text, never a menu.

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
| EDB-8 | Projectile suspension | Per-chunk save limit 8 for all 17 projectile types |
| EDB-9 | Recipe book spam | Packet limiter DROP 4.0s / 5.0 rate |
| EDB-10 | Nether roof access | Nether ceiling void damage at y=127 |
| EDB-11 | Xray | Enables anti-xray, clears exempt blocks, requires hidden blocks |
| EDB-12 | Impersonation | Forces `online-mode=true` — **never patch while behind a BungeeCord/Velocity proxy** (refused, and on the patch denylist by default) |
| EDB-13 | Chat spam | Packet limiter DROP 0.5s / 5.0 rate |
| EDB-14 | Chat command spam | Packet limiter DROP 1.0s / 5.0 rate |
| EDB-15 | Block entity NBT query spam | Packet limiter DROP 2.0s / 3.0 rate |
| EDB-16 | Entity NBT query spam | Packet limiter DROP 2.0s / 3.0 rate |
| EDB-17 | Jigsaw generation spam | Packet limiter DROP 5.0s / 3.0 rate |
| EDB-18 | Plugin messaging flood | Packet limiter DROP 1.0s / 200.0 rate — **on the patch denylist by default**, many plugins rely on this channel |
| EDB-19 | Creative slot NBT injection | Packet limiter DROP 1.0s / 10.0 rate — blocks NBT-injecting clients |
| EDB-20 | Book edit spam | Packet limiter DROP 1.0s / 5.0 rate, complements EDB-2 |
| EDB-21 | Vanilla rate limiter disabled | `rate-limit: 0` → 400 (vanilla ships 0, which means off) |
| EDB-22 | Proxy bypass via direct connections | `prevent-proxy-connections: true` — only offered when a proxy is detected |

Run `/exploitfix check` to audit, `/exploitfix patch <id|all>` to fix, or open the GUI and shift-click.

**A note on duplication.** Item duplication is not a config problem and is not in this table. Rail, carpet, TNT and gravity dupers are mechanic-level, and detecting a dupe needs item-level tracking. If you need that, use a dedicated anti-dupe plugin. What EDB *does* cover on that front is the NBT injection route (EDB-19), the oversized-book NBT bomb (EDB-2, EDB-20) and the dupe-symptoms half of the story: entity and item flooding, capped by `EntityLimiter` and the projectile save limits in EDB-8.

**Renamed keys are never invented.** If a config key a patch needs is absent from your server files, the patch is skipped and logged by name instead of writing a bogus key that would then report itself as fixed. That is what you see as *not applicable* rather than *vulnerable*.

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
/optimize
  Opens the module GUI for players. Console gets the usage line.

/optimize status
  TPS, MSPT, entities, hibernation, modules, pending EDB failures

/optimize now
  Manual purge using WorldCleaner rules. Runs GC if enabled.

/optimize toggle
  Toggles hibernation. Will not freeze if players are online.

/optimize toggle <module>
  Toggles one module, like WorldCleaner or LagShield. Saves to modules/<module>.yml

/optimize krypton
  Opens the Krypton GUI. Same as /optimize kos gui.

/optimize kos [profile] [true|false]
  Applies a ServerTuner profile to your server files. Backs up originals.
  Without args runs kos.default-profile when kos.world-is-pregenerated is set (1=yes, 2=no).

/optimize edb <check|patch <id|all>|gui>
  Same checks and patches as /exploitfix. gui opens the ExploitDB GUI.

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
/exploitfix
  Opens the ExploitDB GUI for players. Console gets the usage line. Also aliased /exploit.

/exploitfix gui
  Opens the ExploitDB GUI.

/exploitfix status
  Active protections, hibernation, EDB summary, restart flag

/exploitfix list
  The full EDB-1…EDB-22 table with live pass/fail state

/exploitfix toggle <name>
  Enable or disable one module. Saves to file.

/exploitfix check
  Audit all 22 exploits

/exploitfix patch <id|all>
  Patch and flag restart required. Honours enabled-exploits,
  patch-allowlist and patch-denylist from modules/ExploitDB.yml
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

gui:
  auto-open: true  # bare /optimize and /exploitfix open the GUI
```

More settings live in `plugins/OptimizeIsEasy/modules/*.yml` (17 files). KOS profiles live in `plugins/OptimizeIsEasy/profiles/*.kos`. Server-file backups land in `plugins/OptimizeIsEasy/backups/`.

### modules/ExploitDB.yml — deciding what gets patched

```yaml
values:
  auto-check-on-start: true
  auto-patch-on-start: false      # patch on boot, still gated by the lists below
  dry-run: false                   # report what would change, write nothing
  backup-before-patch: true        # EDB used to write with no backup at all
  patch-cooldown-seconds: 3        # ignore a second "patch all" in this window

  enabled-exploits: ['*']          # '*' or an explicit list of ids
  patch-allowlist: []              # if set, ONLY these may be patched
  patch-denylist: ['EDB-12', 'EDB-18']  # never patched, not even by patch all

  rate-limit-packets: 400          # value for EDB-21

  packet-limits:                   # override the limiter values per exploit
    EDB-13:
      action: DROP                 # DROP, FILTER or DISABLED
      interval: 0.5                # seconds
      max-packet-rate: 5.0         # packets per second
```

`enabled-exploits` decides what exists at all. `patch-allowlist` is the stricter gate on top. `patch-denylist` always wins, so you can keep EDB-12 visible and auditable while making it impossible to apply by accident. All three are honoured identically by `/exploitfix patch`, `/optimize edb patch`, the GUI shift-click and `auto-patch-on-start`.

### modules/ServerTuner.yml — deciding what gets applied

```yaml
values:
  default-profile: YouHaveTrouble.kos
  world-is-pregenerated: 0         # 1 = yes, 2 = no, 0 = ask every time
  allow-override-pregenerated: false
  dry-run: false
  backup-before-apply: true
  denied-profiles: []              # e.g. ['LowEnd'] - hidden everywhere
```

A profile on `denied-profiles` disappears from `/optimize kos`, from tab completion and from the Krypton GUI, and `runProfile` refuses it even if the name is typed by hand. The `.kos` suffix is optional when listing.

These module keys now take precedence over the older `kos.*` keys in `config.yml`, which still work as a fallback so existing installs keep their settings.

---

## Installation

1. Drop the jar into `plugins/`
2. Restart your Paper 1.21.x server. Works on Paper, Purpur and Folia plus any Paper fork
3. Run `/optimize` and `/exploitfix` in-game to open the menus, or `/optimize status` and `/exploitfix check` for the text output

That is it. No extra setup.

---

## Building from source

```
git clone <your repo>
cd OptimizeIsEasy/1.21.x
./gradlew clean build
```

Jar goes to `1.21.x/build/libs/OptimizeIsEasy-2.0.0.jar`.

Requires Java 21, Paper API 1.21.11. Runs on Paper, Purpur, Folia, Leaf, Gale and Paper forks. No Paperweight, no NMS toolchain.

---

## Upgrading to 2.0.0

Drop the new jar in and restart. Config files are merged in place, so nothing to delete. Six things changed that you will notice:

**Bare `/optimize` and `/exploitfix` now open a GUI.** For players only, console still gets the text output. Set `gui.auto-open: false` in `config.yml` if you want the old usage text back. `/exploitfix` is also reachable as `/exploit`.

**Module clicks moved in the GUI.** In the module list, `ServerTuner`, `ExploitDB` and `BorderControl` now open their own menu on **left-click**; **right-click** is the toggle. Every other module still toggles on left-click. If you were left-clicking ServerTuner to switch it off, use right-click now.

**ExploitDB went from 12 to 22 entries, and two are no longer patched by default.** `EDB-12` (forces `online-mode=true`) and `EDB-18` (throttles plugin messaging) are on `patch-denylist`, so `patch all` skips them. Both are still listed and auditable. Remove one from `patch-denylist` in `modules/ExploitDB.yml` if you want it applied. This means a direct-join server that relied on `patch all` for EDB-12 should either drop it from the denylist or run `/exploitfix patch EDB-12` after editing.

**Patching is stricter and safer.** A config key that is missing from your server files is now skipped and logged by name instead of being written — the old code would create the key and then report it as fixed. EDB also copies your files to `plugins/OptimizeIsEasy/backups/` before writing; in 1.0.0 it wrote with no backup at all.

**`modules/ServerTuner.yml` wins over `config.yml`.** `default-profile`, `allow-override-pregenerated` and the new `world-is-pregenerated` are read from the module file first. The old `kos.*` keys in `config.yml` still work as a fallback, so existing installs keep their values. If you had edited `default-profile` in both places, the module file is the one that counts now.

**The KOS profiles cover more, and explain themselves.** All eight shipped `.kos` files are now commented key by key, and they gained `paper-global`, `gale` and `leaf` sections on top of the new bukkit, spigot and Purpur keys. Leaf and Gale writes are strict: a key your version does not have is skipped, and the run tells you how many. Existing `profiles/*.kos` on disk are never overwritten, so delete the one you want refreshed or copy the section across.

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

**Will KOS touch my Leaf or Gale config?**  
Yes, but only through the `leaf` and `gale` sections of the profile, and only for keys your version actually has. Leaf sizes its own thread pools from your CPU cores, so the profiles leave those counts at `0` instead of guessing.

**Why did a profile run say some keys were skipped?**  
Because your server version does not have them. Forks add and rename options between Minecraft versions, and an absent key is skipped rather than written, so nothing your server cannot read is ever added. The count in the summary is the number of those keys.

---

## License

MIT License. See [LICENSE](LICENSE).

You can use, modify and sell it. Just keep the copyright notice.

---

## Credits

Built by rafmcccc for Paper 1.21 and Paper forks. Designed for Paper, Purpur and Folia.

Server tuning and exploit checks are ports of [Kryptonite](https://github.com/LewMC/Kryptonite) by LewMC (Apache-2.0). Mob AI reduction is inspired by [LagFixer](https://github.com/lajczik/lagfixer) by lajczik (GPL-3.0), reimplemented clean-room against the Paper Goal API. World-border GUI is a port of Border by Gab. Thanks to YouHaveTrouble for the optimisation guide behind the default profile.
