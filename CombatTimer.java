package com.selectcombat.combat;

import com.selectcombat.SelectCombat;
import com.selectcombat.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class CombatTimer {

    private final SelectCombat plugin;
    private final CombatManager combatManager;

    private BukkitTask task;

    public CombatTimer(
            SelectCombat plugin,
            CombatManager combatManager
    ) {
        this.plugin = plugin;
        this.combatManager = combatManager;
    }

    public void start() {

        stop();

        task = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::tick,
                20L,
                20L
        );
    }

    private void tick() {

        Set<UUID> processedSessions = new HashSet<>();

        for (Player player : Bukkit.getOnlinePlayers()) {

            if (!combatManager.isInCombat(player)) {
                continue;
            }

            CombatSession session =
                    combatManager.getSession(player);

            if (session == null) {
                continue;
            }

            UUID sessionId = session.getPlayerOne();

            if (!processedSessions.add(sessionId)) {
                continue;
            }

            int remaining = session.getRemainingSeconds();

            if (remaining <= 0) {

                Player playerOne =
                        Bukkit.getPlayer(session.getPlayerOne());

                Player playerTwo =
                        Bukkit.getPlayer(session.getPlayerTwo());

                if (playerOne != null) {
                    combatManager.endCombat(playerOne);
                } else if (playerTwo != null) {
                    combatManager.endCombat(playerTwo);
                }

                continue;
            }

            /*
             * Show the same timer to both combat players.
             */
            Player playerOne =
                    Bukkit.getPlayer(session.getPlayerOne());

            Player playerTwo =
                    Bukkit.getPlayer(session.getPlayerTwo());

            if (playerOne != null) {
                showTimer(playerOne, remaining);
            }

            if (playerTwo != null) {
                showTimer(playerTwo, remaining);
            }

            session.setRemainingSeconds(remaining - 1);
        }
    }

    private void showTimer(
            Player player,
            int seconds
    ) {

        String combatColorName = plugin.getConfig()
                .getString(
                        "combat-color",
                        "red"
                );

        String timerColorName = plugin.getConfig()
                .getString(
                        "combat-timer-color",
                        "white"
                );

        String display = plugin.getConfig()
                .getString(
                        "combat-display",
                        "&cCombat &f%time%s"
                );

        String combatColor =
                ColorUtil.getColor(
                        combatColorName
                ).toString();

        String timerColor =
                ColorUtil.getColor(
                        timerColorName
                ).toString();

        /*
         * %combat% = Combat color
         * %timer%  = Timer color
         */
        display = display
                .replace(
                        "%combat%",
                        combatColor
                )
                .replace(
                        "%timer%",
                        timerColor
                )
                .replace(
                        "%time%",
                        String.valueOf(seconds)
                );

        player.sendActionBar(
                ColorUtil.translate(display)
        );
    }

    public void stop() {

        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void shutdown() {
        stop();
    }
}