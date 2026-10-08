package com.optimizeiseasy.core.modules.border;

import java.util.Arrays;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BorderGUI {
    public Inventory createMainGUI() {
        Inventory gui = Bukkit.createInventory(null, 27, "" + ChatColor.GOLD + ChatColor.BOLD + "Configure World Border");
        ItemStack filler = this.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < gui.getSize(); i++) gui.setItem(i, filler);
        gui.setItem(11, this.createItem(Material.BARRIER, "Configure Size", ChatColor.GRAY + ">> Set the maximum diameter", ChatColor.GRAY + "   of the world border"));
        gui.setItem(12, this.createItem(Material.COMPASS, "Configure Center", ChatColor.GRAY + ">> Reposition the center", ChatColor.GRAY + "   to your current location"));
        gui.setItem(13, this.createItem(Material.CLOCK, "Configure Shrink", ChatColor.GRAY + ">> Enable timed shrinking", ChatColor.GRAY + "   or expansion of the border"));
        gui.setItem(14, this.createItem(Material.LAVA_BUCKET, "Configure Damage", ChatColor.GRAY + ">> Set how much damage players", ChatColor.GRAY + "   take outside the border"));
        gui.setItem(15, this.createItem(Material.BELL, "Configure Warning", ChatColor.GRAY + ">> Adjust warning distance", ChatColor.GRAY + "   before reaching the border"));
        return gui;
    }

    public Inventory createQuickConfigGUI() {
        Inventory gui = Bukkit.createInventory(null, 27, "" + ChatColor.GOLD + ChatColor.BOLD + "Quick Border Config");
        ItemStack filler = this.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < gui.getSize(); i++) gui.setItem(i, filler);
        gui.setItem(10, this.createColoredBlock(Material.RED_WOOL, ChatColor.YELLOW + "1,000,000", ChatColor.GRAY + ">> Set border size to 1,000,000"));
        gui.setItem(11, this.createColoredBlock(Material.ORANGE_WOOL, ChatColor.YELLOW + "100,000", ChatColor.GRAY + ">> Set border size to 100,000"));
        gui.setItem(12, this.createColoredBlock(Material.YELLOW_WOOL, ChatColor.YELLOW + "50,000", ChatColor.GRAY + ">> Set border size to 50,000"));
        gui.setItem(13, this.createColoredBlock(Material.LIME_WOOL, ChatColor.YELLOW + "10,000", ChatColor.GRAY + ">> Set border size to 10,000"));
        gui.setItem(14, this.createColoredBlock(Material.CYAN_WOOL, ChatColor.YELLOW + "5,000", ChatColor.GRAY + ">> Set border size to 5,000"));
        gui.setItem(15, this.createColoredBlock(Material.BLUE_WOOL, ChatColor.YELLOW + "1,000", ChatColor.GRAY + ">> Set border size to 1,000"));
        gui.setItem(16, this.createColoredBlock(Material.PURPLE_WOOL, ChatColor.YELLOW + "500", ChatColor.GRAY + ">> Set border size to 500"));
        gui.setItem(22, this.createItem(Material.BARRIER, "Back", ChatColor.GRAY + ">> Return to Size Menu"));
        return gui;
    }

    private ItemStack createColoredBlock(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RESET + name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    public Inventory createSizeOptionGUI() {
        Inventory gui = Bukkit.createInventory(null, 27, "" + ChatColor.GOLD + ChatColor.BOLD + "Configure Size Options");
        ItemStack filler = this.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < gui.getSize(); i++) gui.setItem(i, filler);
        gui.setItem(11, this.createItem(Material.NETHER_STAR, "Quick Config", ChatColor.GRAY + ">> Quick border size presets"));
        gui.setItem(15, this.createItem(Material.REPEATER, "Custom", ChatColor.GRAY + ">> Manual size adjustment"));
        gui.setItem(22, this.createItem(Material.BARRIER, "Back", ChatColor.GRAY + ">> Return to main menu"));
        return gui;
    }

    public Inventory createSizeGUI(double currentSize) {
        Inventory gui = Bukkit.createInventory(null, 27, "" + ChatColor.GOLD + ChatColor.BOLD + "Border Size");
        gui.setItem(9, this.createItem(Material.RED_STAINED_GLASS_PANE, "- 100", ChatColor.GRAY + ">> Decrease size by 100"));
        gui.setItem(10, this.createItem(Material.RED_STAINED_GLASS_PANE, "- 10", ChatColor.GRAY + ">> Decrease size by 10"));
        gui.setItem(11, this.createItem(Material.RED_STAINED_GLASS_PANE, "- 1", ChatColor.GRAY + ">> Decrease size by 1"));
        gui.setItem(15, this.createItem(Material.LIME_STAINED_GLASS_PANE, "+ 1", ChatColor.GRAY + ">> Increase size by 1"));
        gui.setItem(16, this.createItem(Material.LIME_STAINED_GLASS_PANE, "+ 10", ChatColor.GRAY + ">> Increase size by 10"));
        gui.setItem(17, this.createItem(Material.LIME_STAINED_GLASS_PANE, "+ 100", ChatColor.GRAY + ">> Increase size by 100"));
        gui.setItem(13, this.createItem(Material.MAP, "Current Size : " + Math.round(currentSize)));
        gui.setItem(22, this.createItem(Material.BARRIER, "Back", ChatColor.GRAY + ">> Return to main menu"));
        return gui;
    }

    public Inventory createDamageGUI(double currentDamage) {
        Inventory gui = Bukkit.createInventory(null, 27, "" + ChatColor.GOLD + ChatColor.BOLD + "Border Damage");
        gui.setItem(10, this.createItem(Material.RED_STAINED_GLASS_PANE, "- 1.0", ChatColor.GRAY + ">> Decrease damage by 1.0"));
        gui.setItem(11, this.createItem(Material.RED_STAINED_GLASS_PANE, "- 0.1", ChatColor.GRAY + ">> Decrease damage by 0.1"));
        gui.setItem(15, this.createItem(Material.LIME_STAINED_GLASS_PANE, "+ 0.1", ChatColor.GRAY + ">> Increase damage by 0.1"));
        gui.setItem(16, this.createItem(Material.LIME_STAINED_GLASS_PANE, "+ 1.0", ChatColor.GRAY + ">> Increase damage by 1.0"));
        gui.setItem(13, this.createItem(Material.MAP, "Current Damage : " + currentDamage));
        gui.setItem(22, this.createItem(Material.BARRIER, "Back", ChatColor.GRAY + ">> Return to main menu"));
        return gui;
    }

    public Inventory createWarningGUI(int currentWarning) {
        Inventory gui = Bukkit.createInventory(null, 27, "" + ChatColor.GOLD + ChatColor.BOLD + "Warning Distance");
        gui.setItem(9, this.createItem(Material.RED_STAINED_GLASS_PANE, "- 100", ChatColor.GRAY + ">> Decrease warning by 100"));
        gui.setItem(10, this.createItem(Material.RED_STAINED_GLASS_PANE, "- 10", ChatColor.GRAY + ">> Decrease warning by 10"));
        gui.setItem(11, this.createItem(Material.RED_STAINED_GLASS_PANE, "- 1", ChatColor.GRAY + ">> Decrease warning by 1"));
        gui.setItem(15, this.createItem(Material.LIME_STAINED_GLASS_PANE, "+ 1", ChatColor.GRAY + ">> Increase warning by 1"));
        gui.setItem(16, this.createItem(Material.LIME_STAINED_GLASS_PANE, "+ 10", ChatColor.GRAY + ">> Increase warning by 10"));
        gui.setItem(17, this.createItem(Material.LIME_STAINED_GLASS_PANE, "+ 100", ChatColor.GRAY + ">> Increase warning by 100"));
        gui.setItem(13, this.createItem(Material.MAP, "Current Warning : " + currentWarning));
        gui.setItem(22, this.createItem(Material.BARRIER, "Back", ChatColor.GRAY + ">> Return to main menu"));
        return gui;
    }

    public Inventory createShrinkGUI(boolean isShrinking, double targetSize, long duration) {
        Inventory gui = Bukkit.createInventory(null, 45, "" + ChatColor.GOLD + ChatColor.BOLD + "Border Shrink");
        ItemStack filler = this.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < gui.getSize(); i++) gui.setItem(i, filler);
        gui.setItem(10, this.createItem(Material.RED_STAINED_GLASS_PANE, "- 100"));
        gui.setItem(11, this.createItem(Material.RED_STAINED_GLASS_PANE, "- 10"));
        gui.setItem(12, this.createItem(Material.RED_STAINED_GLASS_PANE, "- 1"));
        gui.setItem(13, this.createItem(Material.MAP, "Target Size : " + Math.round(targetSize)));
        gui.setItem(14, this.createItem(Material.LIME_STAINED_GLASS_PANE, "+ 1"));
        gui.setItem(15, this.createItem(Material.LIME_STAINED_GLASS_PANE, "+ 10"));
        gui.setItem(16, this.createItem(Material.LIME_STAINED_GLASS_PANE, "+ 100"));
        gui.setItem(19, this.createItem(Material.RED_STAINED_GLASS_PANE, "- 100"));
        gui.setItem(20, this.createItem(Material.RED_STAINED_GLASS_PANE, "- 10"));
        gui.setItem(21, this.createItem(Material.RED_STAINED_GLASS_PANE, "- 1"));
        gui.setItem(22, this.createItem(Material.CLOCK, "Duration : " + duration + "s"));
        gui.setItem(23, this.createItem(Material.LIME_STAINED_GLASS_PANE, "+ 1"));
        gui.setItem(24, this.createItem(Material.LIME_STAINED_GLASS_PANE, "+ 10"));
        gui.setItem(25, this.createItem(Material.LIME_STAINED_GLASS_PANE, "+ 100"));
        gui.setItem(40, this.createItem(Material.BARRIER, "Back", ChatColor.GRAY + ">> Return to main menu"));
        gui.setItem(44, this.createItem(Material.GREEN_WOOL, "Start", ChatColor.GRAY + ">> Begin the border transition"));
        return gui;
    }

    public ItemStack createItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + name);
            if (loreLines.length > 0) meta.setLore(Arrays.asList(loreLines));
            item.setItemMeta(meta);
        }
        return item;
    }
}
