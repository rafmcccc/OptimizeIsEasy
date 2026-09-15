package com.optimizeiseasy.core.modules.border;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class BorderListener implements Listener {
    private final OptimizeIsEasyPlugin plugin;
    private final BorderGUI guiManager = new BorderGUI();
    private final Map<Player, BorderEnum> settingContext = new HashMap<>();
    private final Map<Player, Double> currentSize = new HashMap<>();
    private final Map<Player, Double> currentDamage = new HashMap<>();
    private final Map<Player, Integer> currentWarning = new HashMap<>();
    private final Map<Player, Boolean> shrinking = new HashMap<>();
    private final Map<Player, Double> shrinkTargetSize = new HashMap<>();
    private final Map<Player, Long> shrinkDuration = new HashMap<>();

    public BorderListener(OptimizeIsEasyPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
    }

    public void openMainGUI(Player player) {
        Inventory gui = this.guiManager.createMainGUI();
        player.openInventory(gui);
        this.settingContext.remove(player);
    }

    public void openSizeGUI(Player player) {
        double current = player.getWorld().getWorldBorder().getSize();
        this.currentSize.put(player, current);
        Inventory gui = this.guiManager.createSizeGUI(current);
        player.openInventory(gui);
        this.settingContext.put(player, BorderEnum.SIZE);
    }

    public void openSizeOptionGUI(Player player) {
        Inventory gui = this.guiManager.createSizeOptionGUI();
        player.openInventory(gui);
        this.settingContext.put(player, BorderEnum.SIZE_OPTION);
    }

    public void openDamageGUI(Player player) {
        double current = player.getWorld().getWorldBorder().getDamageAmount();
        double rounded = Math.round(current * 10.0) / 10.0;
        this.currentDamage.put(player, rounded);
        Inventory gui = this.guiManager.createDamageGUI(rounded);
        player.openInventory(gui);
        this.settingContext.put(player, BorderEnum.DAMAGE);
    }

    public void openWarningGUI(Player player) {
        int current = player.getWorld().getWorldBorder().getWarningDistance();
        this.currentWarning.put(player, current);
        Inventory gui = this.guiManager.createWarningGUI(current);
        player.openInventory(gui);
        this.settingContext.put(player, BorderEnum.WARNING);
    }

    public void openShrinkGUI(Player player) {
        double cs = player.getWorld().getWorldBorder().getSize();
        boolean isShrinking = this.shrinking.getOrDefault(player, true);
        double targetSize = this.shrinkTargetSize.getOrDefault(player, cs / 2.0);
        long duration = this.shrinkDuration.getOrDefault(player, 60L);
        Inventory gui = this.guiManager.createShrinkGUI(isShrinking, targetSize, duration);
        player.openInventory(gui);
        this.settingContext.put(player, BorderEnum.SHRINK);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!isBorderGUI(title)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked != null && clicked.getType() != Material.AIR && clicked.hasItemMeta()) {
            String rawName = clicked.getItemMeta().getDisplayName();
            String name = ChatColor.stripColor(rawName);
            event.setCancelled(true);
            Sound clickSound = Sound.UI_BUTTON_CLICK;
            Sound plingSound = Sound.BLOCK_NOTE_BLOCK_PLING;
            if (title.contains("Configure World Border")) {
                switch (clicked.getType()) {
                    case BARRIER -> { player.playSound(player.getLocation(), clickSound, 1.0F, 1.0F); this.openSizeOptionGUI(player); }
                    case COMPASS -> {
                        Location loc = player.getLocation();
                        double cx = Math.round(loc.getX());
                        double cz = Math.round(loc.getZ());
                        player.getWorld().getWorldBorder().setCenter(cx, cz);
                        player.sendMessage(ChatColor.GREEN + "Center set to your current location (" + cx + ", " + cz + ")");
                        player.playSound(player.getLocation(), plingSound, 1.0F, 1.0F);
                    }
                    case CLOCK -> { player.playSound(player.getLocation(), clickSound, 1.0F, 1.0F); this.openShrinkGUI(player); }
                    case LAVA_BUCKET -> { player.playSound(player.getLocation(), clickSound, 1.0F, 1.0F); this.openDamageGUI(player); }
                    case BELL -> { player.playSound(player.getLocation(), clickSound, 1.0F, 1.0F); this.openWarningGUI(player); }
                    default -> {}
                }
            } else if (title.contains("Border Size")) {
                if (clicked.getType() == Material.MAP) {
                    player.playSound(player.getLocation(), clickSound, 1.0F, 1.0F);
                    long rs = Math.round(this.currentSize.getOrDefault(player, 0.0));
                    player.sendMessage(ChatColor.GRAY + "Current value : " + ChatColor.YELLOW + rs);
                    return;
                }
                if (name.equalsIgnoreCase("Back")) {
                    player.playSound(player.getLocation(), clickSound, 1.0F, 1.0F);
                    this.openMainGUI(player);
                } else {
                    player.playSound(player.getLocation(), plingSound, 1.0F, 1.0F);
                    this.handleNumericUpdate(player, name, this.currentSize, "Size updated : ", player.getWorld().getWorldBorder()::setSize);
                    this.openSizeGUI(player);
                }
            } else if (title.contains("Configure Size Options")) {
                if (name.equalsIgnoreCase("Quick Config")) {
                    player.playSound(player.getLocation(), clickSound, 1.0F, 1.0F);
                    player.openInventory(this.guiManager.createQuickConfigGUI());
                    this.settingContext.put(player, BorderEnum.QUICK_CONFIG);
                } else if (name.equalsIgnoreCase("Custom")) {
                    player.playSound(player.getLocation(), clickSound, 1.0F, 1.0F);
                    this.openSizeGUI(player);
                } else if (name.equalsIgnoreCase("Back")) {
                    player.playSound(player.getLocation(), clickSound, 1.0F, 1.0F);
                    this.openMainGUI(player);
                }
            } else if (title.contains("Quick Border Config")) {
                if (name.equalsIgnoreCase("Back")) {
                    player.playSound(player.getLocation(), clickSound, 1.0F, 1.0F);
                    this.openSizeOptionGUI(player);
                    return;
                }
                try {
                    String sanitized = name.replaceAll("[,_]", "");
                    double size = Double.parseDouble(sanitized);
                    player.getWorld().getWorldBorder().setSize(Math.round(size));
                    player.sendMessage(ChatColor.GREEN + "Border size set to " + ChatColor.YELLOW + (int) size);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F);
                } catch (NumberFormatException ignored) {}
            } else if (title.contains("Border Damage")) {
                if (clicked.getType() == Material.MAP) {
                    player.playSound(player.getLocation(), clickSound, 1.0F, 1.0F);
                    player.sendMessage(ChatColor.GRAY + "Current value : " + ChatColor.YELLOW + this.currentDamage.getOrDefault(player, 0.0));
                    return;
                }
                if (name.equalsIgnoreCase("Back")) {
                    player.playSound(player.getLocation(), clickSound, 1.0F, 1.0F);
                    this.openMainGUI(player);
                } else {
                    player.playSound(player.getLocation(), plingSound, 1.0F, 1.0F);
                    this.handleNumericUpdate(player, name, this.currentDamage, "Damage updated : ", player.getWorld().getWorldBorder()::setDamageAmount);
                    this.openDamageGUI(player);
                }
            } else if (title.contains("Warning Distance")) {
                if (clicked.getType() == Material.MAP) {
                    player.playSound(player.getLocation(), clickSound, 1.0F, 1.0F);
                    player.sendMessage(ChatColor.GRAY + "Current value : " + ChatColor.YELLOW + this.currentWarning.getOrDefault(player, 0));
                    return;
                }
                if (name.equalsIgnoreCase("Back")) {
                    player.playSound(player.getLocation(), clickSound, 1.0F, 1.0F);
                    this.openMainGUI(player);
                } else {
                    player.playSound(player.getLocation(), plingSound, 1.0F, 1.0F);
                    this.handleNumericUpdate(player, name, this.currentWarning, "Warning distance updated : ", value -> player.getWorld().getWorldBorder().setWarningDistance(value));
                    this.openWarningGUI(player);
                }
            } else if (title.contains("Border Shrink")) {
                if (clicked.getType() == Material.MAP || clicked.getType() == Material.CLOCK) {
                    player.playSound(player.getLocation(), clickSound, 1.0F, 1.0F);
                    String display = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
                    if (display.startsWith("Target Size") || display.startsWith("Duration")) {
                        String[] parts = display.split(":", 2);
                        if (parts.length == 2) player.sendMessage(ChatColor.GRAY + parts[0] + ": " + ChatColor.YELLOW + parts[1].trim());
                        else player.sendMessage(ChatColor.GRAY + display);
                    } else {
                        player.sendMessage(ChatColor.GRAY + "Current value : " + ChatColor.YELLOW + display);
                    }
                    return;
                }
                if (name.equalsIgnoreCase("Back")) {
                    player.playSound(player.getLocation(), clickSound, 1.0F, 1.0F);
                    this.openMainGUI(player);
                    return;
                }
                boolean changed = false;
                boolean playPling = false;
                double current = player.getWorld().getWorldBorder().getSize();
                double target = this.shrinkTargetSize.getOrDefault(player, current / 2.0);
                long duration = this.shrinkDuration.getOrDefault(player, 60L);
                if (name.matches("[-+]\\s\\d+")) {
                    int delta = Integer.parseInt(name.substring(2));
                    int slot = event.getSlot();
                    if (slot >= 19 && slot <= 25) {
                        long nd = name.startsWith("-") ? Math.max(0L, duration - delta) : duration + delta;
                        if (nd != duration) {
                            player.sendMessage(ChatColor.GREEN + "Duration updated : " + ChatColor.YELLOW + nd + " seconds");
                            duration = nd;
                            changed = true;
                            playPling = true;
                        }
                    } else if (slot >= 10 && slot <= 16) {
                        double minTarget = 1.0;
                        double maxTarget = current - 1.0;
                        if (maxTarget < minTarget) maxTarget = minTarget;
                        double nt = name.startsWith("-") ? Math.max(minTarget, target - delta) : Math.min(maxTarget, target + delta);
                        if (nt != target) {
                            player.sendMessage(ChatColor.GREEN + "Target size updated : " + ChatColor.YELLOW + Math.round(nt));
                            target = nt;
                            changed = true;
                            playPling = true;
                        }
                    }
                } else if (name.equals("Start")) {
                    player.playSound(player.getLocation(), clickSound, 1.0F, 1.0F);
                    double finalTarget = Math.round(Math.min(current, target));
                    player.getWorld().getWorldBorder().setSize(finalTarget, duration);
                    player.sendMessage(ChatColor.GREEN + "World border will shrink to " + ChatColor.YELLOW + Math.round(finalTarget) + ChatColor.GREEN + " blocks in " + ChatColor.YELLOW + duration + ChatColor.GREEN + " seconds");
                    player.closeInventory();
                    return;
                }
                if (changed) {
                    if (playPling) player.playSound(player.getLocation(), plingSound, 1.0F, 1.0F);
                    this.shrinkTargetSize.put(player, target);
                    this.shrinkDuration.put(player, duration);
                    this.openShrinkGUI(player);
                }
            }
        } else {
            event.setCancelled(true);
        }
    }

    private boolean isBorderGUI(String title) {
        return title.contains("Configure World Border")
                || title.contains("Border Size")
                || title.contains("Configure Size Options")
                || title.contains("Quick Border Config")
                || title.contains("Border Damage")
                || title.contains("Warning Distance")
                || title.contains("Border Shrink");
    }

    @SuppressWarnings("unchecked")
    private <T extends Number> void handleNumericUpdate(Player player, String name, Map<Player, T> map, String message, Consumer<T> updater) {
        try {
            double current = map.getOrDefault(player, (T) (Object) 0).doubleValue();
            double delta = (!name.startsWith("- ") && !name.startsWith("+ ")) ? 0.0 : Double.parseDouble(name.substring(2));
            current = name.startsWith("-") ? current - delta : current + delta;
            if (map.get(player) instanceof Integer) {
                int updated = Math.max(0, (int) Math.round(current));
                updater.accept((T) (Object) updated);
                map.put(player, (T) (Object) updated);
                player.sendMessage(ChatColor.GREEN + message + ChatColor.YELLOW + updated);
            } else {
                int updated = (int) Math.max(0L, Math.round(current));
                updater.accept((T) (Object) (double) updated);
                map.put(player, (T) (Object) (double) updated);
                player.sendMessage(ChatColor.GREEN + message + ChatColor.YELLOW + updated);
            }
        } catch (Exception ignored) {}
    }
}
