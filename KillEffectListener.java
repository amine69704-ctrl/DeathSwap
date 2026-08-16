package com.selectcombat.listener;

import com.selectcombat.combat.CombatManager;
import com.selectcombat.region.RegionManager;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class KillEffectListener implements Listener {

    private final CombatManager combatManager;
    private final RegionManager regionManager;

    public KillEffectListener(
            CombatManager combatManager,
            RegionManager regionManager
    ) {
        this.combatManager = combatManager;
        this.regionManager = regionManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) {
            return;
        }

        /*
         * Kill effect only works when the fight
         * belongs to a SELECT-COMBAT region.
         */
        if (regionManager.getRegionAt(victim.getLocation()) == null) {
            return;
        }

        if (!combatManager.isOpponent(killer, victim)) {
            return;
        }

        /*
         * Totem-style kill effect.
         */
        killer.getWorld().spawnParticle(
                Particle.TOTEM_OF_UNDYING,
                victim.getLocation().add(0, 1, 0),
                80,
                0.5,
                0.8,
                0.5,
                0.1
        );

        killer.getWorld().playSound(
                killer.getLocation(),
                Sound.ITEM_TOTEM_USE,
                1.0f,
                1.0f
        );
    }
}