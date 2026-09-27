package com.optimizeiseasy.core.utils;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public final class ServerFileUtil {
    private ServerFileUtil() {}

    public enum WriteResult {
        WROTE,
        MISSING_KEY,
        NO_FILE,
        FAILED
    }

    public static boolean exists(String path) {
        try {
            return new File(path).isFile();
        } catch (Exception e) {
            return false;
        }
    }

    public static YamlConfiguration loadYaml(String path) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            File f = new File(path);
            if (!f.exists()) return cfg;
            cfg.load(f);
        } catch (Exception ignored) {}
        return cfg;
    }

    public static boolean saveYaml(String path, YamlConfiguration cfg) {
        try {
            cfg.save(new File(path));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getYamlString(String path, String key) {
        try {
            YamlConfiguration cfg = loadYaml(path);
            Object o = cfg.get(key);
            return o == null ? null : o.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean setYaml(String path, String key, Object value) {
        try {
            YamlConfiguration cfg = loadYaml(path);
            cfg.set(key, value);
            return saveYaml(path, cfg);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean hasKey(String path, String key) {
        try {
            if (!new File(path).isFile()) return false;
            return loadYaml(path).contains(key);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean hasProperty(String file, String key) {
        try {
            if (!new File(file).isFile()) return false;
            return getProperty(file, key) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static WriteResult setYamlStrict(String path, String key, Object value) {
        if (!new File(path).isFile()) return WriteResult.NO_FILE;
        YamlConfiguration cfg = loadYaml(path);
        if (!cfg.contains(key)) return WriteResult.MISSING_KEY;
        cfg.set(key, value);
        return saveYaml(path, cfg) ? WriteResult.WROTE : WriteResult.FAILED;
    }

    public static WriteResult setPropertyStrict(String file, String key, String value) {
        if (!new File(file).isFile()) return WriteResult.NO_FILE;
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
        } catch (Exception e) {
            return WriteResult.FAILED;
        }
        if (props.getProperty(key) == null) return WriteResult.MISSING_KEY;
        props.setProperty(key, value);
        try (FileOutputStream out = new FileOutputStream(file)) {
            props.store(out, "Patched by OptimizeIsEasy");
        } catch (Exception e) {
            return WriteResult.FAILED;
        }
        return WriteResult.WROTE;
    }

    public static String getProperty(String file, String key) {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
        } catch (Exception e) {
            return null;
        }
        return props.getProperty(key);
    }

    public static boolean setProperty(String file, String key, String value) {
        Properties props = new Properties();
        File f = new File(file);
        try {
            if (f.exists()) {
                try (FileInputStream in = new FileInputStream(f)) {
                    props.load(in);
                }
            }
            props.setProperty(key, value);
            try (FileOutputStream out = new FileOutputStream(f)) {
                props.store(out, "Patched by OptimizeIsEasy");
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean equalsYaml(String path, String key, String expected) {
        String cur = getYamlString(path, key);
        if (cur == null || expected == null) return cur == null && expected == null;
        return cur.equals(expected);
    }
}
