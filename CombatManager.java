package com.selectcombat.combat;

import com.selectcombat.SelectCombat;
import com.selectcombat.util.ColorUtil;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CombatManager {

    private final SelectCombat plugin;

    private final Map<UUID, UUID> opponents = new HashMap<>();
    private final Map<UUID, CombatSession> sessions = new HashMap<>();

    public CombatManager(SelectCombat plugin) {
        this.plugin = plugin;
    }

    public boolean startCombat(Player player, Player opponent) {

        if (player == null || opponent == null
                || player.equals(opponent)) {
            return false;
        }

        if (isInCombat(player) || isInCombat(opponent)) {
            return false;
        }

        int combatTime = Math.max(
                1,
                plugin.getConfig().getInt("combat-time", 15)
        );

        UUID playerId = player.getUniqueId();
        UUID opponentId = opponent.getUniqueId();

        CombatSession session = new CombatSession(
                playerId,
                opponentId,
                combatTime
        );

        opponents.put(playerId, opponentId);
        opponents.put(opponentId, playerId);

        sessions.put(playerId, session);
        sessions.put(opponentId, session);

        sendCombatStart(player);
        sendCombatStart(opponent);

        return true;
    }

    public boolean refreshCombat(Player player, Player opponent) {

        if (!isOpponent(player, opponent)) {
            return false;
        }

        CombatSession session = sessions.get(player.getUniqueId());

        if (session == null) {
            return false;
        }

        int combatTime = Math.max(
                1,
                plugin.getConfig().getInt("combat-time", 15)
        );

        session.refresh(combatTime);

        return true;
    }

    public boolean isInCombat(Player player) {
        return player != null
                && opponents.containsKey(player.getUniqueId());
    }

    public boolean isOpponent(Player first, Player second) {

        if (first == null || second == null) {
            return false;
        }

        UUID opponentId = opponents.get(first.getUniqueId());

        return opponentId != null
                && opponentId.equals(second.getUniqueId());
    }

    public Player getOpponent(Player player) {

        if (player == null) {
            return null;
        }

        UUID opponentId = opponents.get(
                player.getUniqueId()
        );

        if (opponentId == null) {
            return null;
        }

        return plugin.getServer().getPlayer(opponentId);
    }

    public CombatSession getSession(Player player) {

        if (player == null) {
            return null;
        }

        return sessions.get(player.getUniqueId());
    }

    public void endCombat(Player player) {

        if (player == null) {
            return;
        }

        UUID playerId = player.getUniqueId();
        UUID opponentId = opponents.remove(playerId);

        CombatSession session = sessions.remove(playerId);

        if (opponentId != null) {
            opponents.remove(opponentId);
            sessions.remove(opponentId);

            Player opponent = plugin.getServer()
                    .getPlayer(opponentId);

            sendCombatEnd(player);

            if (opponent != null) {
                sendCombatEnd(opponent);
            }

        } else if (session != null) {
            sendCombatEnd(player);
        }
    }

    public void endCombat(UUID playerId) {

        if (playerId == null) {
            return;
        }

        Player player = plugin.getServer()
                .getPlayer(playerId);

        if (player != null) {
            endCombat(player);
            return;
        }

        UUID opponentId = opponents.remove(playerId);

        sessions.remove(playerId);

        if (opponentId != null) {
            opponents.remove(opponentId);
            sessions.remove(opponentId);
        }
    }

    public void endAllCombats() {

        opponents.clear();
        sessions.clear();
    }

    public int getCombatPlayerCount() {
        return opponents.size();
    }

    public int getCombatSessionCount() {
        return sessions.size() / 2;
    }

    private void sendCombatStart(Player player) {

        String colorName = plugin.getConfig()
                .getString("combat-color", "red");

        String message = plugin.getConfig()
                .getString(
                        "messages.combat-start",
                        "NOW YOU ARE COMBAT"
                );

        player.sendMessage(
                ColorUtil.getColor(colorName)
                        + ColorUtil.translate(message)
        );
    }

    private void sendCombatEnd(Player player) {

        if (!player.isOnline()) {
            return;
        }

        String colorName = plugin.getConfig()
                .getString("combat-color", "red");

        String message = plugin.getConfig()
                .getString(
                        "messages.combat-end",
                        "NOW YOU ARE NOT COMBAT"
                );

        player.sendMessage(
                ColorUtil.getColor(colorName)
                        + ColorUtil.translate(message)
        );
    }
}