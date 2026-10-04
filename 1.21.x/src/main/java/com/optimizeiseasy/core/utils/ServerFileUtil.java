package com.optimizeiseasy.core.utils;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public final class ServerFileUtil {
    private ServerFileUtil() {}

    private static final Logger LOG = Logger.getLogger(ServerFileUtil.class.getName());

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
        } catch (Exception e) {
            LOG.warning("Could not parse " + path + ", treating as empty: " + e.getMessage());
        }
        return cfg;
    }

    /**
     * Loads a YAML file, failing loudly on parse errors instead of returning a
     * near-empty config that a later save would write over the original.
     */
    private static YamlConfiguration loadYamlStrict(String path) throws IOException, InvalidConfigurationException {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.load(new File(path));
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
            if (!new File(path).isFile()) return null;
            YamlConfiguration cfg = loadYamlStrict(path);
            Object o = cfg.get(key);
            return o == null ? null : o.toString();
        } catch (Exception e) {
            LOG.warning("Could not read " + path + ": " + e.getMessage());
            return null;
        }
    }

    public static boolean setYaml(String path, String key, Object value) {
        try {
            File f = new File(path);
            YamlConfiguration cfg = new YamlConfiguration();
            if (f.isFile()) {
                // Never save over a file we failed to parse: that would replace
                // e.g. spigot.yml with a near-empty document.
                try {
                    cfg = loadYamlStrict(path);
                } catch (Exception e) {
                    LOG.warning("Refusing to write " + path + ": existing file could not be parsed: " + e.getMessage());
                    return false;
                }
            }
            cfg.set(key, value);
            return saveYaml(path, cfg);
        } catch (Exception e) {
            LOG.warning("Could not write " + path + ": " + e.getMessage());
            return false;
        }
    }

    public static boolean hasKey(String path, String key) {
        try {
            if (!new File(path).isFile()) return false;
            return loadYamlStrict(path).contains(key);
        } catch (Exception e) {
            LOG.warning("Could not read " + path + ": " + e.getMessage());
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
        YamlConfiguration cfg;
        try {
            cfg = loadYamlStrict(path);
        } catch (Exception e) {
            LOG.warning("Refusing to write " + path + ": existing file could not be parsed: " + e.getMessage());
            return WriteResult.FAILED;
        }
        if (!cfg.contains(key)) return WriteResult.MISSING_KEY;
        cfg.set(key, value);
        return saveYaml(path, cfg) ? WriteResult.WROTE : WriteResult.FAILED;
    }

    public static WriteResult setPropertyStrict(String file, String key, String value) {
        if (!new File(file).isFile()) return WriteResult.NO_FILE;
        return writePropertyLine(file, key, value, true);
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
        return writePropertyLine(file, key, value, false) == WriteResult.WROTE;
    }

    /**
     * Edits a {@code .properties} file in place so comments, blank lines and
     * key order survive. {@link Properties#store} would rewrite the whole file
     * and drop every comment in {@code server.properties}.
     */
    private static WriteResult writePropertyLine(String file, String key, String value, boolean strict) {
        File f = new File(file);
        if (strict && !f.isFile()) return WriteResult.NO_FILE;
        List<String> lines = new ArrayList<>();
        if (f.isFile()) {
            try {
                lines = new ArrayList<>(Files.readAllLines(f.toPath(), StandardCharsets.ISO_8859_1));
            } catch (Exception e) {
                LOG.warning("Refusing to write " + file + ": existing file could not be read: " + e.getMessage());
                return WriteResult.FAILED;
            }
        }
        boolean found = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.stripLeading();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) continue;
            int sep = separatorIndex(trimmed);
            if (sep < 0) continue;
            if (unescape(trimmed.substring(0, sep).strip()).equals(key)) {
                lines.set(i, key + "=" + value);
                found = true;
                break;
            }
        }
        if (!found) {
            if (strict) return WriteResult.MISSING_KEY;
            if (!lines.isEmpty() && !lines.get(lines.size() - 1).isEmpty()) lines.add("");
            lines.add(key + "=" + value);
        }
        try {
            Files.write(f.toPath(), lines, StandardCharsets.ISO_8859_1);
        } catch (Exception e) {
            LOG.warning("Could not write " + file + ": " + e.getMessage());
            return WriteResult.FAILED;
        }
        return WriteResult.WROTE;
    }

    /** Index of the first unescaped {@code =}, {@code :} or whitespace separator. */
    private static int separatorIndex(String line) {
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '=' || c == ':' || c == ' ' || c == '\t' || c == '\f') return i;
        }
        return -1;
    }

    private static String unescape(String key) {
        if (key.indexOf('\\') < 0) return key;
        StringBuilder out = new StringBuilder(key.length());
        boolean escaped = false;
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (escaped) {
                out.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                out.append(c);
            }
        }
        if (escaped) out.append('\\');
        return out.toString();
    }

    public static boolean equalsYaml(String path, String key, String expected) {
        String cur = getYamlString(path, key);
        if (cur == null || expected == null) return cur == null && expected == null;
        return cur.equals(expected);
    }
}
