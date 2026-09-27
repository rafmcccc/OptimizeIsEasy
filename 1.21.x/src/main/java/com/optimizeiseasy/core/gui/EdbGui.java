package com.optimizeiseasy.core.gui;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.modules.ExploitDBModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EdbGui implements Listener {
    public static final String TITLE = "OptimizeIsEasy - ExploitDB";

    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23};
    private static final int SLOT_SUMMARY = 4;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_RESTART = 47;
    private static final int SLOT_RECHECK = 48;
    private static final int SLOT_PAGE = 49;
    private static final int SLOT_PATCH_ALL = 50;
    private static final int SLOT_BACK = 52;
    private static final int SLOT_NEXT = 53;

    private final OptimizeIsEasyPlugin plugin;
    private final Map<UUID, Integer> pages = new HashMap<>();

    public EdbGui(OptimizeIsEasyPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private ExploitDBModule edb() {
        return plugin.getModuleManager().get(ExploitDBModule.class);
    }

    private boolean hasPerm(Player p) {
        return p.hasPermission("optimizeiseasy.exploitfix") || p.hasPermission("rafmc.exploitfix") || p.isOp();
    }

    public void open(Player p) {
        ExploitDBModule mod = edb();
        if (mod == null || !mod.isLoaded()) {
            p.sendMessage("§cExploitDB module is disabled. Enable it with /optimize toggle ExploitDB.");
            return;
        }
        List<String> ids = mod.allIds();
        int perPage = SLOTS.length;
        int pageCount = Math.max(1, (ids.size() + perPage - 1) / perPage);
        int page = Math.min(Math.max(pages.getOrDefault(p.getUniqueId(), 0), 0), pageCount - 1);
        pages.put(p.getUniqueId(), page);

        Map<String, ExploitDBModule.CheckResult> res = mod.checkDetailed();
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        int from = page * perPage;
        for (int i = 0; i < perPage; i++) {
            int index = from + i;
            if (index >= ids.size()) break;
            inv.setItem(SLOTS[i], exploitItem(mod, res, ids.get(index)));
        }

        inv.setItem(SLOT_SUMMARY, summaryItem(mod, res, page, pageCount));
        if (pageCount > 1) {
            inv.setItem(SLOT_PAGE, item(Material.PAPER, "§ePage " + (page + 1) + " of " + pageCount,
                    "§7" + ids.size() + " exploits tracked", "§8Left-click an item for details"));
        }
        if (page > 0) inv.setItem(SLOT_PREV, item(Material.ARROW, "§ePrevious page", "§7Page " + page + " of " + pageCount));
        if (page < pageCount - 1) inv.setItem(SLOT_NEXT, item(Material.ARROW, "§eNext page", "§7Page " + (page + 2) + " of " + pageCount));
        if (plugin.isRestartRequired()) {
            inv.setItem(SLOT_RESTART, item(Material.REDSTONE_TORCH, "§cRestart required",
                    "§7KOS / EDB changes need a restart", "§7to take effect"));
        }
        inv.setItem(SLOT_RECHECK, item(Material.SPYGLASS, "§eRe-check all",
                "§7Re-reads every server config", "§7Nothing is written to disk"));
        inv.setItem(SLOT_PATCH_ALL, patchAllItem(mod, res));
        inv.setItem(SLOT_BACK, item(Material.BARRIER, "§cBack", "§7Return to the module list"));
        p.openInventory(inv);
    }

    private ItemStack exploitItem(ExploitDBModule mod, Map<String, ExploitDBModule.CheckResult> res, String id) {
        boolean enabled = mod.isEnabled(id);
        ExploitDBModule.CheckResult state = res.get(id);
        List<String> lore = new ArrayList<>();
        Material mat;
        String name = "§f" + id + " §7- " + mod.describe(id);
        lore.add("§7Config: §8" + mod.target(id));

        if (!enabled) {
            mat = Material.BARRIER;
            lore.add("§8Disabled in modules/ExploitDB.yml");
        } else if (state == ExploitDBModule.CheckResult.UNSUPPORTED) {
            mat = Material.GRAY_STAINED_GLASS_PANE;
            if (id.equals("EDB-12") && mod.isProxied()) {
                lore.add("§cProxied server - do not patch");
                lore.add("§8Forcing online-mode=true breaks");
                lore.add("§8logins through BungeeCord/Velocity");
            } else if (id.equals("EDB-22") && !mod.isProxied()) {
                lore.add("§8Only relevant behind a proxy");
            } else {
                lore.add("§8Not applicable on your software");
            }
        } else if (state == ExploitDBModule.CheckResult.SAFE) {
            mat = Material.LIME_WOOL;
            lore.add("§aPatched");
        } else {
            mat = Material.RED_WOOL;
            lore.add("§cVulnerable");
            lore.add("§eLeft-click: details");
            lore.add("§eShift-click: §fpatch §e(backs up first)");
        }
        if (mod.isDryRun()) {
            lore.add("§6Dry run on - patch writes nothing");
        }
        lore.add("§8 /exploitfix patch " + id);
        return item(mat, name, lore);
    }

    private ItemStack summaryItem(ExploitDBModule mod, Map<String, ExploitDBModule.CheckResult> res, int page, int pages) {
        long safe = 0, vulnerable = 0, unsupported = 0, disabled = 0;
        for (Map.Entry<String, ExploitDBModule.CheckResult> en : res.entrySet()) {
            if (!mod.isEnabled(en.getKey())) {
                disabled++;
                continue;
            }
            switch (en.getValue()) {
                case SAFE -> safe++;
                case VULNERABLE -> vulnerable++;
                case UNSUPPORTED -> unsupported++;
            }
        }
        List<String> lore = new ArrayList<>();
        lore.add("§a" + safe + " patched");
        lore.add(vulnerable > 0 ? "§c" + vulnerable + " vulnerable" : "§a0 vulnerable");
        if (unsupported > 0) lore.add("§8" + unsupported + " not applicable here");
        if (disabled > 0) lore.add("§8" + disabled + " disabled in config");
        if (pages > 1) lore.add("§8Page " + (page + 1) + "/" + pages);
        if (plugin.getSoftwareDetector() != null) lore.add("§8" + plugin.getSoftwareDetector().describe());
        if (mod.isProxied()) lore.add("§cProxy detected - EDB-12 locked");
        if (mod.isDryRun()) lore.add("§6Dry run on");
        return item(Material.BOOK, "§eExploitDB " + safe + "/" + (safe + vulnerable), lore);
    }

    private ItemStack patchAllItem(ExploitDBModule mod, Map<String, ExploitDBModule.CheckResult> res) {
        long pending = res.entrySet().stream()
                .filter(en -> mod.canPatch(en.getKey()) && en.getValue() == ExploitDBModule.CheckResult.VULNERABLE).count();
        List<String> lore = new ArrayList<>();
        lore.add("§7Patches every exploit that is");
        lore.add("§7enabled and allowed to patch");
        if (mod.isBackupBeforePatch()) lore.add("§7Backs up originals first");
        if (mod.isDryRun()) lore.add("§6Dry run - nothing will be written");
        else lore.add("§cRestart required");
        lore.add(pending == 0 ? "§8Nothing to patch" : "§e" + pending + " waiting");
        return item(pending == 0 ? Material.GRAY_DYE : Material.EMERALD_BLOCK, "§aPatch all", lore);
    }

    private ItemStack item(Material mat, String name, String first, String... rest) {
        List<String> lore = new ArrayList<>();
        lore.add(first);
        lore.addAll(List.of(rest));
        return item(mat, name, lore);
    }

    private ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack is = new ItemStack(mat);
        ItemMeta meta = is.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            is.setItemMeta(meta);
        }
        return is;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!TITLE.equals(e.getView().getTitle())) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!hasPerm(p)) {
            p.sendMessage("§cNo permission.");
            p.closeInventory();
            return;
        }
        ExploitDBModule mod = edb();
        if (mod == null || !mod.isLoaded()) {
            p.sendMessage("§cExploitDB module is disabled.");
            p.closeInventory();
            return;
        }
        int slot = e.getRawSlot();
        List<String> ids = mod.allIds();
        int perPage = SLOTS.length;
        int page = pages.getOrDefault(p.getUniqueId(), 0);
        int from = page * perPage;

        for (int i = 0; i < perPage; i++) {
            if (SLOTS[i] != slot) continue;
            int index = from + i;
            if (index >= ids.size()) return;
            String id = ids.get(index);
            if (e.isShiftClick()) patch(mod, p, id);
            else inspect(mod, p, id);
            return;
        }

        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.0F);
        switch (slot) {
            case SLOT_SUMMARY -> {
                p.closeInventory();
                p.performCommand("exploitfix list");
            }
            case SLOT_PREV -> {
                pages.put(p.getUniqueId(), Math.max(0, page - 1));
                open(p);
            }
            case SLOT_NEXT -> {
                pages.put(p.getUniqueId(), page + 1);
                open(p);
            }
            case SLOT_RECHECK -> open(p);
            case SLOT_PATCH_ALL -> {
                mod.patchAll(p);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F);
                refreshLater(p);
            }
            case SLOT_BACK -> {
                p.closeInventory();
                if (plugin.getGui() != null) Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (p.isOnline()) plugin.getGui().open(p);
                }, 2L);
            }
            default -> {
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        pages.remove(e.getPlayer().getUniqueId());
    }

    private void inspect(ExploitDBModule mod, Player p, String id) {
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.0F);
        ExploitDBModule.CheckResult state = mod.checkResult(id);
        p.sendMessage("§8§m    §r §c§l" + id + " §7" + mod.describe(id) + " §8§m    ");
        p.sendMessage(" §8• §fConfig: §7" + mod.target(id));
        if (!mod.isEnabled(id)) {
            p.sendMessage(" §8• §7Disabled in modules/ExploitDB.yml (enabled-exploits).");
        } else if (state == ExploitDBModule.CheckResult.UNSUPPORTED) {
            p.sendMessage(" §8• §cNot applicable on your server software.");
        } else if (state == ExploitDBModule.CheckResult.SAFE) {
            p.sendMessage(" §8• §aPatched");
        } else {
            p.sendMessage(" §8• §cVulnerable - shift-click in the GUI to patch.");
        }
        String blocked = mod.patchBlockedReason(id);
        if (blocked != null) p.sendMessage(" §8• §7Cannot patch: " + blocked + ".");
        if (id.equals("EDB-12") && mod.isProxied()) {
            p.sendMessage(" §8• §cDo not patch: forcing online-mode=true would break proxy logins.");
        }
    }

    private void patch(ExploitDBModule mod, Player p, String id) {
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.0F);
        if (!mod.patch(id, p)) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 1.0F);
            return;
        }
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F);
        refreshLater(p);
    }

    private void refreshLater(Player p) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) open(p);
        }, 5L);
    }
}
