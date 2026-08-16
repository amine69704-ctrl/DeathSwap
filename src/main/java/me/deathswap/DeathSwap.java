package me.deathswap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeathSwap extends JavaPlugin implements Listener, TabExecutor {

    private final Set<String> team1 = new HashSet<>();
    private final Set<String> team2 = new HashSet<>();

    private final Set<String> hiddenScoreboards = new HashSet<>();

    private final Map<String, Location> lastLocations = new HashMap<>();

    private final Map<String, Integer> deathsThisRound = new HashMap<>();

    private BukkitRunnable gameTask;
    private BukkitRunnable scoreboardTask;

    private boolean gameRunning = false;

    private int remainingSeconds = 300;

    private int team1Score = 0;
    private int team2Score = 0;

    private int roundNumber = 0;

    private boolean swapJustHappened = false;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        Bukkit.getPluginManager().registerEvents(this, this);

        if (getCommand("deathswap") != null) {
            getCommand("deathswap").setExecutor(this);
            getCommand("deathswap").setTabCompleter(this);
        }

        remainingSeconds = getConfig().getInt("swap-time", 300);

        getLogger().info("DeathSwap enabled.");
    }

    @Override
    public void onDisable() {
        cancelTasks();
        removeScoreboards();
    }

    // =========================================================
    // COMMANDS
    // =========================================================

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!command.getName().equalsIgnoreCase("deathswap")) {
            return false;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        // =====================================================
        // OP ONLY COMMANDS
        // =====================================================

        if (sub.equals("team1")) {

            if (!sender.isOp()) {
                sender.sendMessage(color("&cYou must be an operator to use this command."));
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(color("&cUsage: /deathswap team1 <player>"));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);

            if (target == null) {
                sender.sendMessage(color("&cPlayer is not online."));
                return true;
            }

            String name = target.getName().toLowerCase();

            team2.remove(name);
            team1.add(name);

            sender.sendMessage(
                    color("&a" + target.getName() + " &7was added to &bTeam 1&a.")
            );

            return true;
        }

        if (sub.equals("team2")) {

            if (!sender.isOp()) {
                sender.sendMessage(color("&cYou must be an operator to use this command."));
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(color("&cUsage: /deathswap team2 <player>"));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);

            if (target == null) {
                sender.sendMessage(color("&cPlayer is not online."));
                return true;
            }

            String name = target.getName().toLowerCase();

            team1.remove(name);
            team2.add(name);

            sender.sendMessage(
                    color("&a" + target.getName() + " &7was added to &cTeam 2&a.")
            );

            return true;
        }

        if (sub.equals("start")) {

            if (!sender.isOp()) {
                sender.sendMessage(color("&cYou must be an operator to use this command."));
                return true;
            }

            startGame(sender);
            return true;
        }

        if (sub.equals("stop")) {

            if (!sender.isOp()) {
                sender.sendMessage(color("&cYou must be an operator to use this command."));
                return true;
            }

            stopGame(sender);
            return true;
        }

        if (sub.equals("reset")) {

            if (!sender.isOp()) {
                sender.sendMessage(color("&cYou must be an operator to use this command."));
                return true;
            }

            resetGame(sender);
            return true;
        }

        // =====================================================
        // NORMAL PLAYER COMMANDS
        // =====================================================

        if (sub.equals("timer")) {

            sender.sendMessage(
                    color("&6&lDEATH SWAP &7» &fTime remaining: &e"
                            + formatTime(remainingSeconds))
            );

            return true;
        }

        if (sub.equals("score")) {

            sender.sendMessage(color("&6&lDEATH SWAP &7» &bTeam 1: &f"
                    + team1Score
                    + " &7| &cTeam 2: &f"
                    + team2Score));

            return true;
        }

        if (sub.equals("teams")) {

            sender.sendMessage(color("&6&lDEATH SWAP &7» &bTeam 1:"));

            if (team1.isEmpty()) {
                sender.sendMessage(color("&7  No players"));
            } else {
                for (String name : team1) {
                    sender.sendMessage(color("&f  - " + name));
                }
            }

            sender.sendMessage(color("&6&lDEATH SWAP &7» &cTeam 2:"));

            if (team2.isEmpty()) {
                sender.sendMessage(color("&7  No players"));
            } else {
                for (String name : team2) {
                    sender.sendMessage(color("&f  - " + name));
                }
            }

            return true;
        }

        if (sub.equals("scoreboard")) {

            if (args.length < 2) {
                sender.sendMessage(
                        color("&cUsage: /deathswap scoreboard <show|hide>")
                );
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage(color("&cThis command can only be used by a player."));
                return true;
            }

            Player player = (Player) sender;

            if (args[1].equalsIgnoreCase("hide")) {

                hiddenScoreboards.add(player.getName().toLowerCase());

                if (Bukkit.getScoreboardManager() != null) {
                    player.setScoreboard(
                            Bukkit.getScoreboardManager().getMainScoreboard()
                    );
                }

                sender.sendMessage(color("&aScoreboard hidden."));
                return true;
            }

            if (args[1].equalsIgnoreCase("show")) {

                hiddenScoreboards.remove(player.getName().toLowerCase());

                sender.sendMessage(color("&aScoreboard shown."));
                updateScoreboard(player);

                return true;
            }

            sender.sendMessage(
                    color("&cUsage: /deathswap scoreboard <show|hide>")
            );

            return true;
        }

        sendHelp(sender);
        return true;
    }

    // =========================================================
    // START
    // =========================================================

    private void startGame(CommandSender sender) {

        if (gameRunning) {
            sender.sendMessage(color("&cDeathSwap is already running."));
            return;
        }

        if (team1.isEmpty() || team2.isEmpty()) {
            sender.sendMessage(
                    color("&cBoth teams must have at least one player.")
            );
            return;
        }

        cancelTasks();

        gameRunning = true;
        roundNumber = 0;

        remainingSeconds = getConfig().getInt("swap-time", 300);

        deathsThisRound.clear();

        swapJustHappened = false;

        Bukkit.broadcastMessage(
                color("&6&lDEATH SWAP &7» &aThe DeathSwap has started!")
        );

        Bukkit.broadcastMessage(
                color("&7First swap in &e" + formatTime(remainingSeconds))
        );

        startGameTimer();
        startScoreboard();
    }

    // =========================================================
    // TIMER
    // =========================================================

    private void startGameTimer() {

        gameTask = new BukkitRunnable() {

            @Override
            public void run() {

                if (!gameRunning) {
                    cancel();
                    gameTask = null;
                    return;
                }

                if (remainingSeconds <= 0) {

                    performSwap();

                    remainingSeconds =
                            getConfig().getInt("swap-time", 300);

                    return;
                }

                if (remainingSeconds <= 10) {

                    Bukkit.broadcastMessage(
                            color("&c&l" + remainingSeconds)
                    );
                }

                remainingSeconds--;
            }

        }.runTaskTimer(this, 20L, 20L);
    }

    // =========================================================
    // SWAP
    // =========================================================

    private void performSwap() {

        roundNumber++;

        Bukkit.broadcastMessage(
                color("&c&lDEATH SWAP &7» &eSWAP!")
        );

        playSwapSound();

        // Save current locations BEFORE teleporting.
        saveAllLocations();

        // Score deaths from the previous swap round.
        calculateRoundScore();

        deathsThisRound.clear();

        swapJustHappened = true;

        List<Player> players1 = getOnlinePlayers(team1);
        List<Player> players2 = getOnlinePlayers(team2);

        if (players1.isEmpty() || players2.isEmpty()) {

            Bukkit.broadcastMessage(
                    color("&cNot enough online players to perform a normal swap.")
            );

            return;
        }

        /*
         * For equal teams, players are swapped by position.
         */
        int pairs = Math.min(players1.size(), players2.size());

        for (int i = 0; i < pairs; i++) {

            Player p1 = players1.get(i);
            Player p2 = players2.get(i);

            Location l1 = p1.getLocation().clone();
            Location l2 = p2.getLocation().clone();

            p1.teleport(l2);
            p2.teleport(l1);
        }

        /*
         * If one team has extra players, keep their locations.
         * This prevents random teleports or null-location errors.
         */

        Bukkit.broadcastMessage(
                color("&aPlayers have been swapped!")
        );
    }

    // =========================================================
    // SCORE SYSTEM
    // =========================================================

    private void calculateRoundScore() {

        if (!swapJustHappened && roundNumber <= 1) {
            return;
        }

        int team1Deaths = 0;
        int team2Deaths = 0;

        for (String name : team1) {
            team1Deaths += deathsThisRound.getOrDefault(name, 0);
        }

        for (String name : team2) {
            team2Deaths += deathsThisRound.getOrDefault(name, 0);
        }

        if (team1Deaths == 0 && team2Deaths == 0) {
            return;
        }

        /*
         * Both teams have the same number of deaths:
         * no points.
         */
        if (team1Deaths == team2Deaths) {
            Bukkit.broadcastMessage(
                    color("&7Round result: &fNo points.")
            );
            return;
        }

        /*
         * Team 1 has more deaths:
         * Team 2 gets Team 1's number of deaths.
         */
        if (team1Deaths > team2Deaths) {

            team2Score += team1Deaths;

            Bukkit.broadcastMessage(
                    color("&cTeam 1 deaths: &f" + team1Deaths)
            );

            Bukkit.broadcastMessage(
                    color("&aTeam 2 receives &e"
                            + team1Deaths
                            + " &apoint(s)! ")
            );

            return;
        }

        /*
         * Team 2 has more deaths:
         * Team 1 gets Team 2's number of deaths.
         */
        team1Score += team2Deaths;

        Bukkit.broadcastMessage(
                color("&cTeam 2 deaths: &f" + team2Deaths)
        );

        Bukkit.broadcastMessage(
                color("&bTeam 1 receives &e"
                        + team2Deaths
                        + " &bpoint(s)!")
        );
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();

        String name = player.getName().toLowerCase();

        /*
         * IMPORTANT:
         * Deaths before the first swap NEVER give points.
         */
        if (!gameRunning || !swapJustHappened) {
            return;
        }

        /*
         * Only deaths belonging to a DeathSwap team are counted.
         */
        if (!team1.contains(name) && !team2.contains(name)) {
            return;
        }

        deathsThisRound.put(
                name,
                deathsThisRound.getOrDefault(name, 0) + 1
        );
    }

    // =========================================================
    // LOCATION SYSTEM
    // =========================================================

    private void saveAllLocations() {

        for (Player player : Bukkit.getOnlinePlayers()) {

            if (team1.contains(player.getName().toLowerCase())
                    || team2.contains(player.getName().toLowerCase())) {

                lastLocations.put(
                        player.getName().toLowerCase(),
                        player.getLocation().clone()
                );
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();

        String name = player.getName().toLowerCase();

        if (team1.contains(name) || team2.contains(name)) {

            lastLocations.put(
                    name,
                    player.getLocation().clone()
            );
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        String name = player.getName().toLowerCase();

        if (!gameRunning) {
            return;
        }

        /*
         * If the player left during the game, restore their
         * last known DeathSwap location.
         */
        if (lastLocations.containsKey(name)) {

            Location location = lastLocations.get(name);

            if (location != null && location.getWorld() != null) {

                Bukkit.getScheduler().runTaskLater(
                        this,
                        () -> player.teleport(location),
                        1L
                );
            }
        }
    }

    // =========================================================
    // STOP
    // =========================================================

    private void stopGame(CommandSender sender) {

        if (!gameRunning) {
            sender.sendMessage(color("&cDeathSwap is not running."));
            return;
        }

        gameRunning = false;

        cancelTasks();
        removeScoreboards();

        Bukkit.broadcastMessage(
                color("&6&lDEATH SWAP &7» &cThe game has been stopped.")
        );

        sender.sendMessage(color("&aDeathSwap stopped."));
    }

    // =========================================================
    // RESET
    // =========================================================

    private void resetGame(CommandSender sender) {

        gameRunning = false;

        cancelTasks();
        removeScoreboards();

        team1Score = 0;
        team2Score = 0;

        remainingSeconds =
                getConfig().getInt("swap-time", 300);

        roundNumber = 0;

        deathsThisRound.clear();
        lastLocations.clear();

        swapJustHappened = false;

        Bukkit.broadcastMessage(
                color("&6&lDEATH SWAP &7» &eGame reset.")
        );

        sender.sendMessage(
                color("&aDeathSwap has been completely reset.")
        );
    }

    // =========================================================
    // SCOREBOARD
    // =========================================================

    private void startScoreboard() {

        if (scoreboardTask != null) {
            scoreboardTask.cancel();
            scoreboardTask = null;
        }

        scoreboardTask = new BukkitRunnable() {

            @Override
            public void run() {

                if (!gameRunning) {
                    cancel();
                    scoreboardTask = null;
                    return;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateScoreboard(player);
                }
            }

        }.runTaskTimer(this, 0L, 20L);
    }

    private void updateScoreboard(Player player) {

        if (Bukkit.getScoreboardManager() == null) {
            return;
        }

        if (hiddenScoreboards.contains(
                player.getName().toLowerCase()
        )) {
            return;
        }

        Scoreboard board =
                Bukkit.getScoreboardManager().getNewScoreboard();

        Objective objective =
                board.registerNewObjective(
                        "deathswap",
                        "dummy",
                        color("&c&lDEATH SWAP")
                );

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        objective.getScore(
                color("&8----------------")
        ).setScore(6);

        objective.getScore(
                color("&fTime: &e"
                        + formatTime(remainingSeconds))
        ).setScore(5);

        objective.getScore(
                color("&bTeam 1: &f"
                        + team1Score)
        ).setScore(4);

        objective.getScore(
                color("&cTeam 2: &f"
                        + team2Score)
        ).setScore(3);

        objective.getScore(
                color("&7Round: &f"
                        + roundNumber)
        ).setScore(2);

        objective.getScore(
                color("&a/deathswap score")
        ).setScore(1);

        player.setScoreboard(board);
    }

    private void removeScoreboards() {

        if (scoreboardTask != null) {
            scoreboardTask.cancel();
            scoreboardTask = null;
        }

        if (Bukkit.getScoreboardManager() == null) {
            return;
        }

        Scoreboard mainBoard =
                Bukkit.getScoreboardManager().getMainScoreboard();

        for (Player player : Bukkit.getOnlinePlayers()) {
          
