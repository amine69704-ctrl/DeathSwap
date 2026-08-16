package com.selectcombat.combat;

import java.util.UUID;

public final class CombatSession {

    private final UUID playerOne;
    private final UUID playerTwo;

    private long lastHitTime;
    private int remainingSeconds;

    public CombatSession(
            UUID playerOne,
            UUID playerTwo,
            int combatTime
    ) {
        this.playerOne = playerOne;
        this.playerTwo = playerTwo;
        this.remainingSeconds = combatTime;
        this.lastHitTime = System.currentTimeMillis();
    }

    public UUID getPlayerOne() {
        return playerOne;
    }

    public UUID getPlayerTwo() {
        return playerTwo;
    }

    public boolean contains(UUID playerId) {
        return playerOne.equals(playerId)
                || playerTwo.equals(playerId);
    }

    public UUID getOpponent(UUID playerId) {
        if (playerOne.equals(playerId)) {
            return playerTwo;
        }

        if (playerTwo.equals(playerId)) {
            return playerOne;
        }

        return null;
    }

    public void refresh(int combatTime) {
        this.remainingSeconds = combatTime;
        this.lastHitTime = System.currentTimeMillis();
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(int remainingSeconds) {
        this.remainingSeconds = Math.max(0, remainingSeconds);
    }

    public long getLastHitTime() {
        return lastHitTime;
    }

    public boolean isExpired() {
        return remainingSeconds <= 0;
    }
}