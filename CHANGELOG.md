# Changelog

## Upgrading to 3.1.1

Drop the new jar in and restart. No config changes needed.

**Hopper cap no longer freezes legal chunks.** Transfer cancel now fires only over the limit (`>`), placement deny stays at the limit (`>=`). A chunk with exactly 24 hoppers works again. Deny path recounts the chunk first, so explosion/piston breaks cannot false-trigger "limit reached".

**EntityLimiter counts via world events.** Async recount task dropped; Paper `EntityAddToWorldEvent`/`EntityRemoveFromWorldEvent` keep cached counts sync (fixes stuck item counts too). Cache trusted on hit — no scan under the limit. Overflow purge skips protected mobs (armor stand/tamed/leashed/ridden).

**LagShield fully cached.** `reliable()` reads a field refreshed every 20s — zero `Bukkit.getTPS()` allocs per event. View/sim/tick each hold independently. Originals file written only on new worlds.

---

## Upgrading to 3.1.0

Drop the new jar in and restart. Config files are merged in place, so nothing to delete. Optimize + stability only, no new modules:

**Hopper hot path is cheaper.** Packed long keys instead of per-event strings, `getHolder(false)` / `getTileEntities(false)` (no snapshots), validate uses `isChunkLoaded` + `getType()` (no chunk loads, no block snapshots). Chunk cap now denies placement of extras instead of starving the whole chunk on transfer.

**LagShield caches and holds.** One MSPT-first reading per second shared by all events (no more 2x `getTPS()` per redstone/hopper/spawn), `hysteresis.min_hold_seconds: 60` stops 20s chunk-resend churn, originals persist to `lagshield-originals.yml` so a crash while throttled cannot become the next boot's baseline.

**EntityLimiter stops loading chunks.** `isChunkLoaded(x>>4,z>>4)` guard before counting, cached per-chunk counts (O(1) per spawn), `ItemSpawnEvent` covered alongside drops. Overflow purge stays off on Folia with a warning.

**Cleaners are safer.** WorldCleaner skips armor stands, tamed, leashed and ridden mobs by default (`creatures.protect.*`), timed purge stays off on Folia, `items.blacklist` no longer leaks across reloads. Dead keys `creatures.stacked` / `ignore_models` removed. Invalid Material/EntityType names now warn.

**Failures are louder.** GUI/command/hook init failures and scheduler fallbacks log at WARNING. Join update notice reuses `isNewer` (no false "update available" on newer builds), tag strips one leading `v` only.

---

## Upgrading to 3.0.0

Drop the new jar in and restart. Config files are merged in place, so nothing to delete. One thing breaks, one thing widens:

**Breaking: the build moved to the repository root.** `1.21.x/` is gone. If you had a local build, clone or `git pull` and run `./gradlew clean build` from the top of the repo; the jar lands in `build/libs/`. Nothing about your server's `plugins/OptimizeIsEasy/` folder changes.

**One jar now covers Paper 1.21.1 through 26.x.** It is compiled against the oldest supported API and CI compiles the same sources against the newest, so no new API call can leak in and break an older server. `api-version` stays `1.21`, which every version in that range accepts.

One tuner key was added for Leaf 26.x, which renamed its virtual-thread options: `leaf.performance.use-virtual-thread`. Older Leaf versions ignore it, because the tuner only writes keys your server file already has.

---

## Upgrading to 2.1.2

Drop the new jar in and restart. Config files are merged in place, so nothing to delete. Three things changed that you will notice:

**A failed backup now skips the file instead of writing it.** If `plugins/OptimizeIsEasy/backups/` cannot be written, ServerTuner and ExploitDB count that file as failed and tell you which one, rather than patching without a safety copy.

**Server-file writes are atomic.** YAML and `server.properties` are written to a temp file and moved over the original, so a crash can never leave a half-written server file behind.

**Failures are noisier.** Ten previously silent failure paths (conflict scan, restart alert, proxy detection, AI surgery and others) now log at WARNING; noisy per-event paths log at debug. See the console instead of guessing.

---

## Upgrading to 2.1.1

Drop the new jar in and restart. Config files are merged in place, so nothing to delete. Five things changed that you will notice:

**LagShield redstone throttling works independently now.** Disabling `entity_spawn` no longer silently kills the `redstone` check, and all throttles skip cleanly when the server exposes no usable TPS source (Folia) instead of acting on a fake 20.0.

**RedstoneLimiter counts per chunk with one timer.** The per-event delayed task is gone, counting is thread-safe, and the dead `click_cooldown` key was removed.

**Corrupt server files are never overwritten.** A YAML file that fails to parse aborts the write (and logs it) instead of being replaced with a near-empty document. `server.properties` edits now preserve comments and key order.

**Dynamic view/simulation/tick changes are restored on disable.** LagShield snapshots per-world originals before its first change. The dead `mobai` threshold was removed.

**Folia is partial, not supported.** Scheduling goes through the region scheduler, but TPS-based throttling stays off on Folia. The badge and install notes say so until a live Folia test passes.

---

## Upgrading to 2.1.0

Drop the new jar in and restart. Config files are merged in place, so nothing to delete. Four things changed that you will notice:

**MobAiReducer actually reduces AI now.** The old module only logged spawns in debug. It now strips expensive goals through the Paper Goal API, swaps vanilla tempt/breed for cooldown-gated versions, and rescans loaded chunks on enable. Farm breeding still works, villagers and tamed mobs are untouched by default. If a farm behaves oddly after the update, narrow `entities.*` in `modules/MobAiReducer.yml` or add the type to `list`.

**Four new KOS profiles.** `HighEnd` (powerful box, view 10), `VanillaPlus` (near-vanilla SMP), `LobbyGames` (lobbies and minigames), `Anarchy` (tightest caps). The Krypton GUI fits all eight in one row. Your on-disk profiles are never overwritten, so the new files appear on the next restart.

**Conflict warnings explain themselves.** `/optimize report` and the startup log now print *why* a plugin is flagged and what to use instead, and the detector also covers `RoseStacker` and `UltimateStacker`. See Recommended stack above.

**Version is 2.1.0 everywhere.** `/optimize version`, the report header and the built jar all read it from the build, so there is nothing to edit by hand.

---

## Upgrading to 2.0.0

Drop the new jar in and restart. Config files are merged in place, so nothing to delete. Six things changed that you will notice:

**Bare `/optimize` and `/exploitfix` now open a GUI.** For players only, console still gets the text output. Set `gui.auto-open: false` in `config.yml` if you want the old usage text back. `/exploitfix` is also reachable as `/exploit`.

**Module clicks moved in the GUI.** In the module list, `ServerTuner`, `ExploitDB` and `BorderControl` now open their own menu on **left-click**; **right-click** is the toggle. Every other module still toggles on left-click. If you were left-clicking ServerTuner to switch it off, use right-click now.

**ExploitDB went from 12 to 22 entries, and two are no longer patched by default.** `EDB-12` (forces `online-mode=true`) and `EDB-18` (throttles plugin messaging) are on `patch-denylist`, so `patch all` skips them. Both are still listed and auditable. Remove one from `patch-denylist` in `modules/ExploitDB.yml` if you want it applied. This means a direct-join server that relied on `patch all` for EDB-12 should either drop it from the denylist or run `/exploitfix patch EDB-12` after editing.

**Patching is stricter and safer.** A config key that is missing from your server files is now skipped and logged by name instead of being written — the old code would create the key and then report it as fixed. EDB also copies your files to `plugins/OptimizeIsEasy/backups/` before writing; in 1.0.0 it wrote with no backup at all.

**`modules/ServerTuner.yml` wins over `config.yml`.** `default-profile`, `allow-override-pregenerated` and the new `world-is-pregenerated` are read from the module file first. The old `kos.*` keys in `config.yml` still work as a fallback, so existing installs keep their values. If you had edited `default-profile` in both places, the module file is the one that counts now.

**The KOS profiles cover more, and explain themselves.** All eight shipped `.kos` files are now commented key by key, and they gained `paper-global`, `gale` and `leaf` sections on top of the new bukkit, spigot and Purpur keys. Leaf and Gale writes are strict: a key your version does not have is skipped, and the run tells you how many. Existing `profiles/*.kos` on disk are never overwritten, so delete the one you want refreshed or copy the section across.
