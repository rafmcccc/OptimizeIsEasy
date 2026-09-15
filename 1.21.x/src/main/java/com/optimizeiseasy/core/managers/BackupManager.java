package com.optimizeiseasy.core.managers;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

public class BackupManager {
    private final Plugin plugin;
    private final int keep = 10;

    public BackupManager(Plugin plugin) { this.plugin = plugin; }

    public void backup(File file) {
        try {
            if (!file.exists()) return;
            File dir = new File(plugin.getDataFolder(), "backups");
            dir.mkdirs();
            String ts = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            File dest = new File(dir, file.getName() + "." + ts + ".bak");
            Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            // rotation
            File[] list = dir.listFiles((d, n) -> n.startsWith(file.getName()));
            if (list != null && list.length > keep) {
                Arrays.sort(list, Comparator.comparingLong(File::lastModified));
                for (int i = 0; i < list.length - keep; i++) list[i].delete();
            }
        } catch (Exception e) { plugin.getLogger().fine("Backup failed for " + file.getName() + ": " + e.getMessage()); }
    }

    public void backupConfigs() {
        backup(new File(plugin.getDataFolder(), "config.yml"));
        File mods = new File(plugin.getDataFolder(), "modules");
        if (mods.exists()) {
            File[] files = mods.listFiles((d, n) -> n.endsWith(".yml"));
            if (files != null) for (File f : files) backup(f);
        }
    }
}
