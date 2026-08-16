package com.selectcombat.listener;

import com.selectcombat.SelectCombat;
import com.selectcombat.combat.CombatManager;
import com.selectcombat.region.RegionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class CombatListener implements Listener {

    private final SelectCombat plugin;
    private final CombatManager combatManager;
    private final RegionManager regionManager;

    public CombatListener(
            SelectCombat plugin,
            CombatManager combatManager,
            RegionManager regionManager
    ) {
        this.plugin = plugin;
        this.combatManager = combatManager;
        this.regionManager = regionManager;
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onPlayerDamage(EntityDamageByEntityEvent event) {

        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        /*
         * Both players must be inside the same
         * SELECT-COMBAT region.
         */
        var attackerRegion =
                regionManager.getRegionAt(
                        attacker.getLocation()
                );

        var victimRegion =
                regionManager.getRegionAt(
                        victim.getLocation()
                );

        if (attackerRegion == null
                || victimRegion == null) {

            return;
        }

        if (!attackerRegion.getName()
                .equalsIgnoreCase(
                        victimRegion.getName()
                )) {

            event.setCancelled(true);
            return;
        }

        /*
         * If victim is already fighting somebody else,
         * third-party damage is blocked.
         */
        if (combatManager.isInCombat(victim)
                && !combatManager.isOpponent(
                        victim,
                        attacker
                )) {

            event.setCancelled(true);
            return;
        }

        /*
         * If attacker is already fighting somebody else,
         * they cannot attack another player.
         */
        if (combatManager.isInCombat(attacker)
                && !combatManager.isOpponent(
                        attacker,
                        victim
                )) {

            event.setCancelled(true);
            return;
        }

        /*
         * Existing opponents can continue fighting.
         */
        if (combatManager.isOpponent(
                attacker,
                victim
        )) {

            if (plugin.getConfig().getBoolean(
                    "combat.refresh-timer-on-hit",
                    true
            )) {
                combatManager.refreshCombat(
                        attacker,
                        victim
                );
            }

            return;
        }

        /*
         * Both players are free.
         * Start a new 1v1 combat session.
         */
        combatManager.startCombat(
                attacker,
                victim
        );
    }
}