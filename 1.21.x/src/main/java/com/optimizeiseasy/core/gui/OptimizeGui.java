package com.optimizeiseasy.core.gui;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.modules.HibernateModule;
import com.optimizeiseasy.core.objects.AbstractModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class OptimizeGui implements Listener {
    private final OptimizeIsEasyPlugin plugin;

    public OptimizeGui(OptimizeIsEasyPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "OptimizeIsEasy - Modules");
        // Hibernate status at slot 4
        HibernateModule hm = plugin.getModuleManager().get(HibernateModule.class);
        boolean frozen = hm != null && hm.isFrozen();
        ItemStack hItem = new ItemStack(frozen ? Material.ICE : Material.FIRE_CHARGE);
        ItemMeta hmMeta = hItem.getItemMeta();
        if (hmMeta != null) {
            hmMeta.setDisplayName(frozen ? "§cHibernation: Frozen" : "§aHibernation: Running");
            List<String> lore = new ArrayList<>();
            lore.add("§7Click to toggle hibernation");
            lore.add(frozen ? "§cFrozen (0 TPS)" : "§aRunning");
            hmMeta.setLore(lore);
            hItem.setItemMeta(hmMeta);
        }
        inv.setItem(4, hItem);

        // Modules from slot 9
        int slot = 9;
        for (AbstractModule m : plugin.getModuleManager().getModules()) {
            if (slot >= 45) break;
            if (m.getName().equalsIgnoreCase("Hibernate")) continue;
            Material mat;
            if (m.getName().equalsIgnoreCase("ServerTuner")) mat = m.isLoaded() ? Material.NETHERITE_PICKAXE : Material.WOODEN_PICKAXE;
            else if (m.getName().equalsIgnoreCase("ExploitDB")) mat = m.isLoaded() ? Material.SHIELD : Material.ELYTRA;
            else if (m.getName().equalsIgnoreCase("BorderControl")) mat = Material.FILLED_MAP;
            else mat = m.isLoaded() ? Material.LIME_WOOL : Material.RED_WOOL;
            ItemStack is = new ItemStack(mat);
            ItemMeta meta = is.getItemMeta();
            if (meta != null) {
                meta.setDisplayName((m.isLoaded() ? "§a" : "§c") + m.getName());
                List<String> lore = new ArrayList<>();
                lore.add(m.isLoaded() ? "§aEnabled" : "§cDisabled");
                if (m.getName().equalsIgnoreCase("BorderControl")) {
                    lore.add("§eLeft-click: open Border GUI");
                    lore.add("§7Right-click: toggle module");
                } else if (m.getName().equalsIgnoreCase("ServerTuner")) {
                    lore.add("§7KOS profiles: /optimize kos");
                    lore.add("§7Click to toggle");
                } else if (m.getName().equalsIgnoreCase("ExploitDB")) {
                    lore.add("§7EDB check: /exploitfix check");
                    lore.add("§7Click to toggle");
                } else {
                    lore.add("§7Click to toggle");
                }
                meta.setLore(lore);
                is.setItemMeta(meta);
            }
            inv.setItem(slot++, is);
        }
        ItemStack border = new ItemStack(Material.COMPASS);
        ItemMeta bm = border.getItemMeta();
        if (bm != null) {
            bm.setDisplayName("§eBorder Manager");
            bm.setLore(List.of("§7Open WorldBorder GUI", "§7/border or /optimize border"));
            border.setItemMeta(bm);
        }
        inv.setItem(47, border);
        ItemStack report = new ItemStack(Material.PAPER);
        ItemMeta rm = report.getItemMeta();
        if (rm != null) {
            rm.setDisplayName("§eServer Report");
            rm.setLore(List.of("§7Run /optimize report"));
            report.setItemMeta(rm);
        }
        inv.setItem(48, report);
        // Info at slot 49
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta im = info.getItemMeta();
        if (im != null) {
            im.setDisplayName("§eOptimizeIsEasy " + plugin.getDescription().getVersion());
            List<String> lore = new ArrayList<>();
            lore.add("§7Paper " + Bukkit.getVersion());
            im.setLore(lore);
            info.setItemMeta(im);
        }
        inv.setItem(49, info);

        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals("OptimizeIsEasy - Modules")) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (!p.hasPermission("optimizeiseasy.optimize") && !p.isOp()) {
            p.sendMessage("§cNo permission.");
            return;
        }
        // Hibernate toggle
        if (e.getSlot() == 4) {
            p.performCommand("optimize toggle");
            Bukkit.getScheduler().runTaskLater(plugin, () -> open(p), 5L);
            return;
        }
        if (e.getSlot() == 47) {
            p.closeInventory();
            p.performCommand("optimize border");
            return;
        }
        if (e.getSlot() == 48) {
            p.closeInventory();
            p.performCommand("optimize report");
            return;
        }
        // Module toggle
        if (e.getSlot() >= 9 && e.getSlot() < 45) {
            if (clicked.getItemMeta() == null) return;
            String name = clicked.getItemMeta().getDisplayName().replace("§a", "").replace("§c", "");
            if (name.isBlank()) return;
            if (name.equalsIgnoreCase("BorderControl") && !e.isRightClick()) {
                p.closeInventory();
                p.performCommand("optimize border");
                return;
            }
            p.performCommand("optimize toggle " + name);
            Bukkit.getScheduler().runTaskLater(plugin, () -> open(p), 5L);
        }
    }
}
