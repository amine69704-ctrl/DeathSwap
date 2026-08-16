package com.selectcombat.listener;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WandListener implements Listener {

    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWandUse(PlayerInteractEvent event) {

        Action action = event.getAction();

        if (action != Action.LEFT_CLICK_BLOCK
                && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (!isCombatWand(event.getItem())) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (event.getClickedBlock() == null) {
            return;
        }

        Location location = event.getClickedBlock().getLocation().clone();
        UUID uuid = player.getUniqueId();

        if (action == Action.LEFT_CLICK_BLOCK) {

            pos1.put(uuid, location);

            player.sendMessage(
                    ChatColor.GREEN + "Position 1 selected: "
                            + ChatColor.WHITE
                            + formatLocation(location)
            );

        } else {

            pos2.put(uuid, location);

            player.sendMessage(
                    ChatColor.GREEN + "Position 2 selected: "
                            + ChatColor.WHITE
                            + formatLocation(location)
            );
        }
    }

    private boolean isCombatWand(ItemStack item) {

        if (item == null || item.getType() != Material.WOODEN_AXE) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }

        String name = ChatColor.stripColor(meta.getDisplayName());

        return "SCOMBAT WAND".equalsIgnoreCase(name);
    }

    private String formatLocation(Location location) {
        return "X=" + location.getBlockX()
                + " Y=" + location.getBlockY()
                + " Z=" + location.getBlockZ();
    }

    public Location getPos1(Player player) {

        if (player == null) {
            return null;
        }

        Location location = pos1.get(player.getUniqueId());

        return location == null ? null : location.clone();
    }

    public Location getPos2(Player player) {

        if (player == null) {
            return null;
        }

        Location location = pos2.get(player.getUniqueId());

        return location == null ? null : location.clone();
    }

    public boolean hasCompleteSelection(Player player) {
        return getPos1(player) != null && getPos2(player) != null;
    }

    public void clearSelection(Player player) {

        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();

        pos1.remove(uuid);
        pos2.remove(uuid);
    }
}