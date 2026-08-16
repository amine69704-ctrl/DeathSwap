package com.selectcombat.region;

import com.selectcombat.SelectCombat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RegionManager {

    private final SelectCombat plugin;
    private final Map<String, CombatRegion> regions = new LinkedHashMap<>();

    private File file;
    private FileConfiguration config;

    public RegionManager(SelectCombat plugin) {
        this.plugin = plugin;
        load();
    }

    public boolean createRegion(String name, Location pos1, Location pos2) {
        if (name == null || name.isBlank() || pos1 == null || pos2 == null) {
            return false;
        }

        if (pos1.getWorld() == null || pos2.getWorld() == null) {
            return false;
        }

        if (!pos1.getWorld().equals(pos2.getWorld())) {
            return false;
        }

        String key = name.toLowerCase();

        if (regions.containsKey(key)) {
            return false;
        }

        CombatRegion region = new CombatRegion(name, pos1, pos2);
        regions.put(key, region);

        save();
        return true;
    }

    public CombatRegion getRegion(String name) {
        if (name == null) {
            return null;
        }

        return regions.get(name.toLowerCase());
    }

    public Collection<CombatRegion> getRegions() {
        return regions.values();
    }

    public CombatRegion getRegionAt(Location location) {
        if (location == null) {
            return null;
        }

        for (CombatRegion region : regions.values()) {
            if (region.contains(location)) {
                return region;
            }
        }

        return null;
    }

    public boolean deleteRegion(String name) {
        if (name == null) {
            return false;
        }

        CombatRegion removed = regions.remove(name.toLowerCase());

        if (removed == null) {
            return false;
        }

        save();
        return true;
    }

    private void load() {

        file = new File(plugin.getDataFolder(), "regions.yml");

        if (!file.exists()) {
            if (!plugin.getDataFolder().exists()
                    && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning(
                        "Could not create plugin data folder."
                );
            }

            try {
                if (!file.createNewFile()) {
                    plugin.getLogger().warning(
                            "Could not create regions.yml."
                    );
                }
            } catch (IOException exception) {
                plugin.getLogger().severe(
                        "Could not create regions.yml: "
                                + exception.getMessage()
                );
                return;
            }
        }

        config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection section = config.getConfigurationSection("regions");

        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {

            String path = "regions." + key;

            String worldName = config.getString(path + ".world");

            if (worldName == null) {
                continue;
            }

            World world = Bukkit.getWorld(worldName);

            if (world == null) {
                plugin.getLogger().warning(
                        "World '" + worldName
                                + "' for region '" + key
                                + "' is not loaded."
                );
                continue;
            }

            Location pos1 = readLocation(config, path + ".pos1", world);
            Location pos2 = readLocation(config, path + ".pos2", world);

            if (pos1 == null || pos2 == null) {
                plugin.getLogger().warning(
                        "Invalid region data: " + key
                );
                continue;
            }

            String displayName = config.getString(
                    path + ".name",
                    key
            );

            regions.put(
                    key.toLowerCase(),
                    new CombatRegion(displayName, pos1, pos2)
            );
        }
    }

    public void save() {

        if (config == null) {
            config = new YamlConfiguration();
        }

        config.set("regions", null);

        for (Map.Entry<String, CombatRegion> entry : regions.entrySet()) {

            String path = "regions." + entry.getKey();
            CombatRegion region = entry.getValue();

            Location pos1 = region.getPos1();
            Location pos2 = region.getPos2();

            config.set(path + ".name", region.getName());
            config.set(path + ".world", pos1.getWorld().getName());

            writeLocation(config, path + ".pos1", pos1);
            writeLocation(config, path + ".pos2", pos2);
        }

        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe(
                    "Could not save regions.yml: "
                            + exception.getMessage()
            );
        }
    }

    private void writeLocation(
            FileConfiguration config,
            String path,
            Location location
    ) {
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
    }

    private Location readLocation(
            FileConfiguration config,
            String path,
            World world
    ) {
        if (!config.contains(path + ".x")
                || !config.contains(path + ".y")
                || !config.contains(path + ".z")) {
            return null;
        }

        double x = config.getDouble(path + ".x");
        double y = config.getDouble(path + ".y");
        double z = config.getDouble(path + ".z");

        return new Location(world, x, y, z);
    }
}