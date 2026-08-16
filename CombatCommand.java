package com.selectcombat.command;

import com.selectcombat.SelectCombat;
import com.selectcombat.listener.WandListener;
import com.selectcombat.region.CombatRegion;
import com.selectcombat.region.RegionManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public final class CombatCommand implements CommandExecutor {

    private final SelectCombat plugin;
    private final RegionManager regionManager;
    private final WandListener wandListener;

    public CombatCommand(
            SelectCombat plugin,
            RegionManager regionManager,
            WandListener wandListener
    ) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        this.wandListener = wandListener;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    ChatColor.RED + "Only players can use this command."
            );
            return true;
        }

        if (!player.hasPermission("selectcombat.admin")) {
            player.sendMessage(
                    ChatColor.RED + "You don't have permission."
            );
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "wand" -> giveWand(player);

            case "create" -> createRegion(player, args);

            case "list" -> listRegions(player);

            case "help" -> sendHelp(player);

            default -> sendHelp(player);
        }

        return true;
    }

    private void giveWand(Player player) {

        ItemStack wand = new ItemStack(Material.WOODEN_AXE);

        ItemMeta meta = wand.getItemMeta();

        if (meta != null) {

            meta.setDisplayName(
                    ChatColor.GOLD + "SCOMBAT WAND"
            );

            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Left Click = Position 1",
                    ChatColor.GRAY + "Right Click = Position 2"
            ));

            wand.setItemMeta(meta);
        }

        player.getInventory().addItem(wand);

        player.sendMessage(
                ChatColor.GREEN
                        + "You received the "
                        + ChatColor.GOLD
                        + "SCOMBAT WAND"
                        + ChatColor.GREEN
                        + "!"
        );
    }

    private void createRegion(
            Player player,
            String[] args
    ) {

        if (args.length < 2) {

            player.sendMessage(
                    ChatColor.RED
                            + "Usage: /scombat create <name>"
            );

            return;
        }

        if (!wandListener.hasCompleteSelection(player)) {

            player.sendMessage(
                    ChatColor.RED
                            + "You must select Position 1 and Position 2 first!"
            );

            return;
        }

        String name = args[1].trim();

        if (name.isEmpty()) {

            player.sendMessage(
                    ChatColor.RED
                            + "Region name cannot be empty."
            );

            return;
        }

        if (regionManager.getRegion(name) != null) {

            player.sendMessage(
                    ChatColor.RED
                            + "A combat region with this name already exists."
            );

            return;
        }

        var pos1 = wandListener.getPos1(player);
        var pos2 = wandListener.getPos2(player);

        if (pos1 == null || pos2 == null) {

            player.sendMessage(
                    ChatColor.RED
                            + "Invalid selection."
            );

            return;
        }

        if (pos1.getWorld() == null
                || pos2.getWorld() == null
                || !pos1.getWorld().equals(pos2.getWorld())) {

            player.sendMessage(
                    ChatColor.RED
                            + "Position 1 and Position 2 must be in the same world."
            );

            return;
        }

        boolean created = regionManager.createRegion(
                name,
                pos1,
                pos2
        );

        if (!created) {

            player.sendMessage(
                    ChatColor.RED
                            + "Could not create the combat region."
            );

            return;
        }

        wandListener.clearSelection(player);

        player.sendMessage(
                ChatColor.GREEN
                        + "Combat region "
                        + ChatColor.YELLOW
                        + name
                        + ChatColor.GREEN
                        + " created successfully!"
        );
    }

    private void listRegions(Player player) {

        if (regionManager.getRegions().isEmpty()) {

            player.sendMessage(
                    ChatColor.RED
                            + "THE COMBAT IS NOT CREATED"
            );

            return;
        }

        player.sendMessage(
                ChatColor.DARK_PURPLE
                        + "===== SELECT-COMBAT REGIONS ====="
        );

        for (CombatRegion region : regionManager.getRegions()) {

            player.sendMessage(
                    ChatColor.LIGHT_PURPLE
                            + "• "
                            + ChatColor.WHITE
                            + region.getName()
            );
        }
    }

    private void sendHelp(Player player) {

        player.sendMessage(
                ChatColor.DARK_PURPLE
                        + "===== SELECT-COMBAT ====="
        );

        player.sendMessage(
                ChatColor.LIGHT_PURPLE
                        + "/scombat wand"
                        + ChatColor.GRAY
                        + " - Get combat wand"
        );

        player.sendMessage(
                ChatColor.LIGHT_PURPLE
                        + "/scombat create <name>"
                        + ChatColor.GRAY
                        + " - Create combat region"
        );

        player.sendMessage(
                ChatColor.LIGHT_PURPLE
                        + "/scombat list"
                        + ChatColor.GRAY
                        + " - List combat regions"
        );

        player.sendMessage(
                ChatColor.LIGHT_PURPLE
                        + "/scombat help"
                        + ChatColor.GRAY
                        + " - Show help"
        );
    }
}