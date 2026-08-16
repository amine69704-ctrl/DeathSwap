package com.selectcombat.util;

import com.selectcombat.SelectCombat;

public final class ConfigUtil {

    private final SelectCombat plugin;

    public ConfigUtil(SelectCombat plugin) {
        this.plugin = plugin;
    }

    public int getCombatTime() {
        return Math.max(
                1,
                plugin.getConfig().getInt("combat-time", 15)
        );
    }

    public String getCombatColor() {
        return plugin.getConfig().getString(
                "combat-color",
                "red"
        );
    }

    public String getCombatTimerColor() {
        return plugin.getConfig().getString(
                "combat-timer-color",
                "white"
        );
    }

    public String getCombatDisplay() {
        return plugin.getConfig().getString(
                "combat-display",
                "%combat%Combat %timer%%time%s"
        );
    }

    public String getMessage(
            String path,
            String fallback
    ) {
        return plugin.getConfig().getString(
                "messages." + path,
                fallback
        );
    }

    public boolean isThirdPartyDamageBlocked() {
        return plugin.getConfig().getBoolean(
                "combat.block-third-party-damage",
                true
        );
    }

    public boolean shouldRefreshTimerOnHit() {
        return plugin.getConfig().getBoolean(
                "combat.refresh-timer-on-hit",
                true
        );
    }
}