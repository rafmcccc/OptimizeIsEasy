package com.optimizeiseasy.core.gui;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.modules.ServerTunerModule;
import com.optimizeiseasy.core.support.Scheduler;
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

public class KryptonGui implements Listener {
    public static final String TITLE = "OptimizeIsEasy - Krypton (KOS)";

    private static final int SLOT_PREGEN = 4;
    private static final int[] PROFILE_SLOTS = {9, 10, 11, 12, 13, 14, 15, 16};
    private static final int SLOT_BACK = 18;
    private static final int SLOT_APPLY = 22;
    private static final int SLOT_INFO = 26;

    private static final class Selection {
        String profile;
        Boolean pregenerated;
    }

    private final OptimizeIsEasyPlugin plugin;
    private final Map<UUID, Selection> selections = new HashMap<>();

    public KryptonGui(OptimizeIsEasyPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private ServerTunerModule tuner() {
        return plugin.getModuleManager().get(ServerTunerModule.class);
    }

    private boolean hasPerm(Player p) {
        return p.hasPermission("optimizeiseasy.optimize") || p.hasPermission("rafmc.optimize") || p.isOp();
    }

    private Selection selection(Player p) {
        return selections.computeIfAbsent(p.getUniqueId(), k -> new Selection());
    }

    private String defaultProfile(ServerTunerModule tuner) {
        return tuner.defaultProfile();
    }

    private Boolean storedPregen(ServerTunerModule tuner) {
        return tuner.storedPregen();
    }

    public void open(Player p) {
        ServerTunerModule tuner = tuner();
        if (tuner == null || !tuner.isLoaded()) {
            p.sendMessage("§cServerTuner module is disabled. Enable it with /optimize toggle ServerTuner.");
            return;
        }
        Selection sel = selection(p);
        if (sel.profile == null) sel.profile = defaultProfile(tuner);
        if (sel.pregenerated == null) sel.pregenerated = storedPregen(tuner);

        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        inv.setItem(SLOT_PREGEN, pregenItem(sel));
        List<String> profiles = tuner.listProfiles();
        for (int i = 0; i < PROFILE_SLOTS.length && i < profiles.size(); i++) {
            inv.setItem(PROFILE_SLOTS[i], profileItem(profiles.get(i), sel.profile, profiles.get(i).equalsIgnoreCase(defaultProfile(tuner))));
        }
        inv.setItem(SLOT_APPLY, applyItem(sel, tuner));
        inv.setItem(SLOT_BACK, item(Material.BARRIER, "§cBack", "§7Return to the module list"));
        inv.setItem(SLOT_INFO, infoItem(tuner));
        p.openInventory(inv);
    }

    private ItemStack pregenItem(Selection sel) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Is your world pre-generated?");
        lore.add("§7Treasure maps and dolphin looting");
        lore.add("§7are only re-enabled when it is.");
        Material mat;
        String name;
        if (sel.pregenerated == null) {
            mat = Material.GRAY_WOOL;
            name = "§ePregenerated: §funknown";
            lore.add("§eLeft-click: §fYes §eRight-click: §fNo");
            lore.add("§8kos.world-is-pregenerated: 0 (ask every time)");
        } else if (sel.pregenerated) {
            mat = Material.LIME_WOOL;
            name = "§aPregenerated: §fYes";
            lore.add("§7Left-click: §fNo");
            lore.add("§7Right-click: §fYes");
        } else {
            mat = Material.RED_WOOL;
            name = "§cPregenerated: §fNo";
            lore.add("§7Left-click: §fYes");
            lore.add("§7Right-click: §fNo");
        }
        return item(mat, name, lore);
    }

    private ItemStack profileItem(String profile, String selected, boolean isDefault) {
        boolean sel = profile.equalsIgnoreCase(selected);
        List<String> lore = new ArrayList<>();
        lore.add("§7Tuning profile: §f" + profile);
        lore.add(sel ? "§aSelected" : "§8Not selected");
        if (isDefault) lore.add("§bDefault in config.yml");
        lore.add("§eClick to select");
        return item(sel ? Material.NETHERITE_INGOT : Material.PAPER, (sel ? "§a" : "§f") + profile, lore);
    }

    private ItemStack applyItem(Selection sel, ServerTunerModule tuner) {
        List<String> lore = new ArrayList<>();
        Material mat;
        String name;
        if (sel.pregenerated == null) {
            mat = Material.BARRIER;
            name = "§cApply - answer first";
            lore.add("§7Pick pregenerated Yes or No above");
        } else {
            mat = tuner.isDryRun() ? Material.CHEST : Material.EMERALD_BLOCK;
            name = (tuner.isDryRun() ? "§eDry run " : "§aApply ") + sel.profile;
            lore.add("§7Writes server.properties, bukkit.yml,");
            lore.add("§7spigot.yml and Paper / Purpur configs");
            if (tuner.isBackupBeforeApply()) lore.add("§7Backs up originals first");
            if (tuner.isDryRun()) lore.add("§eDry run is on - nothing is written");
            else lore.add("§cRestart required");
        }
        return item(mat, name, lore);
    }

    private ItemStack infoItem(ServerTunerModule tuner) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Backups: §fplugins/OptimizeIsEasy/backups/");
        lore.add("§7Profiles: §fplugins/OptimizeIsEasy/profiles/");
        if (plugin.getSoftwareDetector() != null) {
            lore.add("§7Detected: §f" + plugin.getSoftwareDetector().describe());
        }
        if (!tuner.deniedProfiles().isEmpty()) {
            lore.add("§7Hidden: §8" + String.join(", ", tuner.deniedProfiles()));
        }
        if (tuner.isDryRun()) lore.add("§eDry run on - Apply writes nothing");
        if (plugin.isRestartRequired()) lore.add("§cRestart required - patches pending");
        return item(Material.BOOK, "§eServerTuner", lore);
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
        ServerTunerModule tuner = tuner();
        if (tuner == null || !tuner.isLoaded()) {
            p.sendMessage("§cServerTuner module is disabled.");
            p.closeInventory();
            return;
        }
        Selection sel = selection(p);
        int slot = e.getRawSlot();
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.0F);

        if (slot == SLOT_PREGEN) {
            if (sel.pregenerated == null) {
                sel.pregenerated = e.isRightClick();
            } else {
                sel.pregenerated = !e.isRightClick();
            }
            open(p);
            return;
        }
        for (int i = 0; i < PROFILE_SLOTS.length; i++) {
            if (PROFILE_SLOTS[i] != slot) continue;
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) return;
            String name = clicked.getItemMeta().getDisplayName().replace("§a", "").replace("§f", "");
            if (name.isBlank()) return;
            sel.profile = name;
            open(p);
            return;
        }
        switch (slot) {
            case SLOT_APPLY -> {
                if (sel.profile == null) {
                    p.sendMessage("§cSelect a profile first.");
                    return;
                }
                if (sel.pregenerated == null) {
                    p.sendMessage("§cChoose pregenerated Yes or No first.");
                    return;
                }
                p.closeInventory();
                String profile = sel.profile;
                boolean pregenerated = sel.pregenerated;
                Scheduler.runLaterTicks(plugin, () -> {
                    tuner.runProfile(profile, pregenerated, p);
                    Scheduler.runLaterTicks(plugin, () -> {
                        if (p.isOnline()) open(p);
                    }, 40L);
                }, 1L);
            }
            case SLOT_BACK -> {
                p.closeInventory();
                if (plugin.getGui() != null) Scheduler.runLaterTicks(plugin, () -> {
                    if (p.isOnline()) plugin.getGui().open(p);
                }, 2L);
            }
            case SLOT_INFO -> p.sendMessage("§7Profiles live in §fplugins/OptimizeIsEasy/profiles/§7, backups in §fplugins/OptimizeIsEasy/backups/§7.");
            default -> {
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        selections.remove(e.getPlayer().getUniqueId());
    }
}
