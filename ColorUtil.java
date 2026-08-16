package com.selectcombat.util;

import org.bukkit.ChatColor;

import java.util.Locale;

public final class ColorUtil {

    private ColorUtil() {
        // Utility class
    }

    public static ChatColor getColor(String colorName) {

        if (colorName == null || colorName.isBlank()) {
            return ChatColor.WHITE;
        }

        return switch (colorName.trim().toLowerCase(Locale.ROOT)) {

            case "black" -> ChatColor.BLACK;
            case "dark_blue", "darkblue" -> ChatColor.DARK_BLUE;
            case "dark_green", "darkgreen" -> ChatColor.DARK_GREEN;
            case "dark_aqua", "darkaqua" -> ChatColor.DARK_AQUA;
            case "dark_red", "darkred" -> ChatColor.DARK_RED;
            case "dark_purple", "darkpurple" -> ChatColor.DARK_PURPLE;
            case "gold" -> ChatColor.GOLD;
            case "gray", "grey" -> ChatColor.GRAY;
            case "dark_gray", "darkgray" -> ChatColor.DARK_GRAY;
            case "blue" -> ChatColor.BLUE;
            case "green" -> ChatColor.GREEN;
            case "aqua", "cyan" -> ChatColor.AQUA;
            case "red" -> ChatColor.RED;
            case "light_purple", "lightpurple", "pink" -> ChatColor.LIGHT_PURPLE;
            case "yellow" -> ChatColor.YELLOW;
            case "white" -> ChatColor.WHITE;

            default -> ChatColor.WHITE;
        };
    }

    public static String colorize(String colorName, String text) {

        if (text == null) {
            return "";
        }

        return getColor(colorName) + text;
    }

    public static String translate(String text) {

        if (text == null) {
            return "";
        }

        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String colorizeAndTranslate(
            String colorName,
            String text
    ) {

        return getColor(colorName) + translate(text);
    }
}