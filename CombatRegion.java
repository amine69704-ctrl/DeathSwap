package com.selectcombat.region;

import org.bukkit.Location;

public final class CombatRegion {

    private final String name;
    private final Location pos1;
    private final Location pos2;

    public CombatRegion(String name, Location pos1, Location pos2) {
        this.name = name;
        this.pos1 = pos1.clone();
        this.pos2 = pos2.clone();
    }

    public String getName() {
        return name;
    }

    public Location getPos1() {
        return pos1.clone();
    }

    public Location getPos2() {
        return pos2.clone();
    }

    public boolean contains(Location location) {

        if (location == null || location.getWorld() == null) {
            return false;
        }

        if (pos1.getWorld() == null
                || !pos1.getWorld().equals(location.getWorld())) {
            return false;
        }

        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());

        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());

        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());

        return location.getX() >= minX
                && location.getX() <= maxX
                && location.getY() >= minY
                && location.getY() <= maxY
                && location.getZ() >= minZ
                && location.getZ() <= maxZ;
    }
}