package com.optimizeiseasy.core.modules;

import com.optimizeiseasy.core.OptimizeIsEasyPlugin;
import com.optimizeiseasy.core.objects.AbstractModule;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

public class ConsoleFilterModule extends AbstractModule implements Filter {
    private final List<Pattern> patterns = new ArrayList<>();
    private boolean filterEnabled;
    private Filter oldFilter;

    public ConsoleFilterModule(OptimizeIsEasyPlugin plugin) { super(plugin, "ConsoleFilter"); }

    @Override
    public boolean isLoggable(LogRecord record) {
        if (!filterEnabled) return true;
        String msg = record.getMessage();
        if (msg == null) return true;
        for (Pattern p : patterns) if (p.matcher(msg).find()) return false;
        return true;
    }

    @Override
    public void load() {
        if (filterEnabled && !patterns.isEmpty()) {
            oldFilter = Bukkit.getLogger().getFilter();
            Bukkit.getLogger().setFilter(this);
        }
    }

    @Override
    public boolean loadConfig() {
        filterEnabled = getSection().getBoolean("filter.enabled", false);
        patterns.clear();
        for (String s : getSection().getStringList("filter.patterns")) {
            try { patterns.add(Pattern.compile(s)); } catch (Exception ignored) {}
        }
        return true;
    }

    @Override
    public void disable() {
        if (oldFilter != null) Bukkit.getLogger().setFilter(oldFilter);
        else Bukkit.getLogger().setFilter(null);
    }
}
