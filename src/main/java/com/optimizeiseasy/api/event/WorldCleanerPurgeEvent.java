package com.optimizeiseasy.api.event;

import org.bukkit.World;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class WorldCleanerPurgeEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;
    private final World world;
    private int removed;

    public WorldCleanerPurgeEvent(World world, int removed) { this.world = world; this.removed = removed; }
    public World getWorld() { return world; }
    public int getRemoved() { return removed; }
    public void setRemoved(int removed) { this.removed = removed; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
