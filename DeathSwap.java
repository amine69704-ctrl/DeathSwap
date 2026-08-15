package me.deathswap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class DeathSwap extends JavaPlugin
        implements CommandExecutor, Listener, TabCompleter {

    private final List<String> team1 = new ArrayList<>();
    private final List<String> team2 = new ArrayList<>();

    private BukkitTask gameTask;
    private BukkitTask scoreboardTask;

    private boolean gameRunning = false;

    private int remainingSeconds = 0;

    private int team1Score = 0;
    private int team2Score = 0;

    private int team1RoundDeaths = 0;
    private int team2RoundDeaths = 0;

    private final Set<String> hiddenScoreboards = new HashSet<>();

    @Override
    public void onEnable() {

        saveDefaultConfig();

        if (getCommand("deathswap") != null) {

            getCommand("deathswap").setExecutor(this);
            getCommand("deathswap").setTabCompleter(this);
        }

        Bukkit.getPluginManager()
                .registerEvents(this, this);

        getLogger().info(
                "DeathSwap enabled!"
        );
    }

    @Override
    public void onDisable() {

        cancelTasks();
        removeScoreboards();

        getLogger().info(
                "DeathSwap disabled!"
        );
    }

    // ==========================================
    // COLOR
    // ==========================================

    private String color(String text) {

        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }

    // ==========================================
    // BROADCAST
    // ==========================================

    private void broadcast(String message) {

        Bukkit.broadcastMessage(
                color(
                        getConfig().getString(
                                "messages.prefix",
                                ""
                        ) + message
                )
        );
    }

    private void send(
            CommandSender sender,
            String message
    ) {

        sender.sendMessage(
                color(
                        getConfig().getString(
                                "messages.prefix",
                                ""
                        ) + message
                )
        );
    }

    // ==========================================
    // COMMAND
    // ==========================================

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!command.getName()
                .equalsIgnoreCase("deathswap")) {

            return true;
        }

        if (args.length == 0) {

            showHelp(sender);

            return true;
        }

        String sub =
                args[0].toLowerCase();

        // --------------------------------------
        // PUBLIC COMMANDS
        // --------------------------------------

        if (sub.equals("timer")) {

            showTimer(sender);

            return true;
        }

        if (sub.equals("score")) {

            showScore(sender);

            return true;
        }

        if (sub.equals("teams")) {

            showTeams(sender);

            return true;
        }

        if (sub.equals("scoreboard")) {

            handleScoreboardCommand(sender, args);

            return true;
        }

        // --------------------------------------
        // ADMIN PERMISSION
        // --------------------------------------

        if (!sender.hasPermission(
                "deathswap.admin")) {

            send(
                    sender,
                    "&cYou don't have permission!"
            );

            return true;
        }

        // --------------------------------------
        // COMMANDS
        // --------------------------------------

        switch (sub) {

            case "team1":

                addPlayersToTeam(
                        sender,
                        args,
                        team1,
                        1
                );

                break;

            case "team2":

                addPlayersToTeam(
                        sender,
                        args,
                        team2,
                        2
                );

                break;

            case "start":

                startGame(sender);

                break;

            case "stop":

                stopGame(sender);

                break;

            case "reset":

                resetGame(sender);

                break;

            default:

                send(
                        sender,
                        "&cUnknown command!"
                );

                break;
        }

        return true;
    }

    // ==========================================
    // HELP
    // ==========================================

    private void showHelp(
            CommandSender sender
    ) {

        send(
                sender,
                "&6&lDeathSwap Commands"
        );

        send(
                sender,
                "&e/deathswap timer"
        );

        send(
                sender,
                "&e/deathswap score"
        );

        send(
                sender,
                "&e/deathswap teams"
        );

        send(
                sender,
                "&e/deathswap team1 <players>"
        );

        send(
                sender,
                "&e/deathswap team2 <players>"
        );

        send(
                sender,
                "&e/deathswap start"
        );

        send(
                sender,
                "&e/deathswap stop"
        );

        send(
                sender,
                "&e/deathswap reset"
        );
    }

    // ==========================================
    // TEAM SYSTEM
    // ==========================================

    private void addPlayersToTeam(
            CommandSender sender,
            String[] args,
            List<String> team,
            int teamNumber
    ) {

        if (gameRunning) {

            send(
                    sender,
                    "&cYou cannot change teams "
                            + "while DeathSwap is running!"
            );

            return;
        }

        if (args.length < 2) {

            send(
                    sender,
                    "&cUsage: /deathswap team"
                            + teamNumber
                            + " <player1> <player2> ..."
            );

            return;
        }

        int added = 0;
        int alreadyInTeam = 0;

        for (int i = 1; i < args.length; i++) {

            String playerName = args[i];

            // Remove player from opposite team
            if (teamNumber == 1) {

                team2.removeIf(
                        name -> name.equalsIgnoreCase(
                                playerName
                        )
                );

            } else {

                team1.removeIf(
                        name -> name.equalsIgnoreCase(
                                playerName
                        )
                );
            }

            // Check duplicate
            boolean exists = false;

            for (String name : team) {

                if (name.equalsIgnoreCase(
                        playerName
                )) {

                    exists = true;

                    break;
                }
            }

            if (exists) {

                alreadyInTeam++;

                continue;
            }

            team.add(playerName);

            added++;
        }

        send(
                sender,
                "&aTeam "
                        + teamNumber
                        + " updated!"
        );

        if (added > 0) {

            send(
                    sender,
                    "&aAdded players: &f"
                            + added
            );
        }

        if (alreadyInTeam > 0) {

            send(
                    sender,
                    "&eAlready in Team "
                            + teamNumber
                            + ": &f"
                            + alreadyInTeam
            );
        }
    }

    // ==========================================
    // START
    // ==========================================

    private void startGame(
            CommandSender sender
    ) {

        if (gameRunning) {

            send(
                    sender,
                    "&cGame is already running!"
            );

            return;
        }

        if (team1.isEmpty()) {

            send(
                    sender,
                    "&cTeam 1 has no players!"
            );

            return;
        }

        if (team2.isEmpty()) {

            send(
                    sender,
                    "&cTeam 2 has no players!"
            );

            return;
        }

        team1Score = 0;
        team2Score = 0;
        team1RoundDeaths = 0;
        team2RoundDeaths = 0;

        gameRunning = true;

        broadcast(
                "&aDeathSwap game is starting!"
        );

        startCountdown();

        startScoreboard();
    }

    // ==========================================
    // START COUNTDOWN
    // ==========================================

    private void startCountdown() {

        new BukkitRunnable() {

            int count =
                    getConfig().getInt(
                            "start-countdown-seconds",
                            3
                    );

            @Override
            public void run() {

                if (!gameRunning) {

                    cancel();

                    return;
                }

                if (count <= 0) {

                    broadcast(
                            "&c&lTHE DEATH SWAP "
                                    + "IS STARTING NOW!"
                    );

                    startTimer();

                    cancel();

                    return;
                }

                broadcast(
                        "&eStarting in &c"
                                + count
                                + "&e..."
                );

                count--;
            }

        }.runTaskTimer(
                this,
                0L,
                20L
        );
    }

    // ==========================================
    // TIMER
    // ==========================================

    private void startTimer() {

        remainingSeconds =
                getConfig().getInt(
                        "swap-time-seconds",
                        300
                );

        gameTask = new BukkitRunnable() {

            @Override
            public void run() {

                if (!gameRunning) {

                    cancel();

                    return;
                }

                // ------------------------------
                // 10 SECOND COUNTDOWN
                // ------------------------------

                if (remainingSeconds <= 10 &&
                        remainingSeconds > 0) {

                    broadcast(
                            "&c&l"
                                    + remainingSeconds
                    );

                    remainingSeconds--;

                    return;
                }

                // ------------------------------
                // SWAP
                // ------------------------------

                if (remainingSeconds <= 0) {

                    resolveRoundDeaths();

                    if (!gameRunning) {
                        cancel();
                        return;
                    }

                    performSwap();

                    remainingSeconds =
                            getConfig().getInt(
                                    "swap-time-seconds",
                                    300
                            );

                    return;
                }

                remainingSeconds--;

                // ------------------------------
                // WARNINGS
                // ------------------------------

                if (remainingSeconds == 60 ||
                        remainingSeconds == 30) {

                    broadcast(
                            "&eSwap in &c"
                                    + remainingSeconds
                                    + " seconds!"
                    );
                }
            }

        }.runTaskTimer(
                this,
                20L,
                20L
        );
    }

    // ==========================================
    // RANDOM SWAP
    // ==========================================

    private void performSwap() {

        broadcast(
                "&b&lDEATH SWAP!"
        );

        List<Player> players1 =
                getOnlinePlayers(team1);

        List<Player> players2 =
                getOnlinePlayers(team2);

        if (players1.isEmpty() ||
                players2.isEmpty()) {

            broadcast(
                    "&cSwap skipped because "
                            + "a team has no online players."
            );

            return;
        }

        Collections.shuffle(players1);
        Collections.shuffle(players2);

        List<Location> locations1 =
                new ArrayList<>();

        List<Location> locations2 =
                new ArrayList<>();

        for (Player player : players1) {

            locations1.add(
                    player.getLocation().clone()
            );
        }

        for (Player player : players2) {

            locations2.add(
                    player.getLocation().clone()
            );
        }

        int size1 = players1.size();
        int size2 = players2.size();

        // --------------------------------------
        // SAME SIZE
        // --------------------------------------

        if (size1 == size2) {

            for (int i = 0; i < size1; i++) {

                Player p1 =
                        players1.get(i);

                Player p2 =
                        players2.get(i);

                p1.teleport(
                        locations2.get(i)
                );

                p2.teleport(
                        locations1.get(i)
                );

                sendSwapMessage(p1);
                sendSwapMessage(p2);
            }
        }

        // --------------------------------------
        // TEAM 1 SMALLER
        // --------------------------------------

        else if (size1 < size2) {

            for (int i = 0; i < size2; i++) {

                Player p2 =
                        players2.get(i);

                Location target =
                        locations1.get(
                                i % size1
                        );

                p2.teleport(target);

                sendSwapMessage(p2);
            }

            for (int i = 0; i < size1; i++) {

                Player p1 =
                        players1.get(i);

                Location target =
                        locations2.get(
                                i % size2
                        );

                p1.teleport(target);

                sendSwapMessage(p1);
            }
        }

        // --------------------------------------
        // TEAM 1 LARGER
        // --------------------------------------

        else {

            for (int i = 0; i < size1; i++) {

                Player p1 =
                        players1.get(i);

                Location target =
                        locations2.get(
                                i % size2
                        );

                p1.teleport(target);

                sendSwapMessage(p1);
            }

            for (int i = 0; i < size2; i++) {

                Player p2 =
                        players2.get(i);

                Location target =
                        locations1.get(
                                i % size1
                        );

                p2.teleport(target);

                sendSwapMessage(p2);
            }
        }

        broadcast(
                "&aRandom team swap completed!"
        );
    }

    private List<Player> getOnlinePlayers(
            List<String> names
    ) {

        List<Player> result =
                new ArrayList<>();

        for (String name : names) {

            Player player =
                    Bukkit.getPlayerExact(name);

            if (player != null &&
                    player.isOnline()) {

                result.add(player);
            }
        }

        return result;
    }

    private void sendSwapMessage(
            Player player
    ) {

        player.sendMessage(
                color(
                        "&b&lDEATH SWAP! "
                                + "&fYou have been swapped!"
                )
        );
    }

    // ==========================================
    // DEATH
    // ==========================================

    @EventHandler
    public void onDeath(
            PlayerDeathEvent event
    ) {

        if (!gameRunning) {

            return;
        }

        String name =
                event.getEntity().getName();

        if (team1.contains(name)) {

            team1RoundDeaths++;

            broadcast(
                    "&c"
                            + name
                            + " &7from Team 1 died!"
            );

        } else if (team2.contains(name)) {

            team2RoundDeaths++;

            broadcast(
                    "&c"
                            + name
                            + " &7from Team 2 died!"
            );
        }

        broadcastScore();
    }

    // ==========================================
    // ROUND DEATH SCORE
    // ==========================================

    private void resolveRoundDeaths() {

        int deaths1 = team1RoundDeaths;
        int deaths2 = team2RoundDeaths;

        if (deaths1 == deaths2) {

            if (deaths1 > 0) {
                broadcast(
                        "&eBoth teams had the same number "
                                + "of deaths. No points awarded."
                );
            }

        } else if (deaths1 > deaths2) {

            team2Score += deaths1;

            broadcast(
                    "&aTeam 2 gets &e"
                            + deaths1
                            + " point(s) &afor Team 1 deaths!"
            );

        } else {

            team1Score += deaths2;

            broadcast(
                    "&aTeam 1 gets &e"
                            + deaths2
                            + " point(s) &afor Team 2 deaths!"
            );
        }

        team1RoundDeaths = 0;
        team2RoundDeaths = 0;

        broadcastScore();

        checkWinner();
    }

    // ==========================================
    // RESPAWN
    // ==========================================

    @EventHandler
    public void onRespawn(
            PlayerRespawnEvent event
    ) {

        if (!gameRunning) {

            return;
        }

        event.getPlayer().sendMessage(
                color(
                        "&eYou are back in DeathSwap!"
                )
        );
    }

    // ==========================================
    // QUIT
    // ==========================================

    @EventHandler
    public void onQuit(
            PlayerQuitEvent event
    ) {

        if (!gameRunning) {

            return;
        }

        String name =
                event.getPlayer().getName();

        if (team1.contains(name)) {

            team2Score++;

            broadcast(
                    "&c"
                            + name
                            + " &7from Team 1 left!"
            );

            broadcast(
                    "&aTeam 2 gets &e1 point!"
            );

        } else if (team2.contains(name)) {

            team1Score++;

            broadcast(
                    "&c"
                            + name
                            + " &7from Team 2 left!"
            );

            broadcast(
                    "&aTeam 1 gets &e1 point!"
            );
        }

        broadcastScore();

        checkWinner();
    }

    // ==========================================
    // WINNER
    // ==========================================

    private void checkWinner() {

        int pointsToWin =
                getConfig().getInt(
                        "points-to-win",
                                                3
                );

        if (team1Score >= pointsToWin) {

            broadcast(
                    "&b&lTEAM 1 WINS!"
            );

            endGame();

            return;
        }

        if (team2Score >= pointsToWin) {

            broadcast(
                    "&c&lTEAM 2 WINS!"
            );

            endGame();
        }
    }

    // ==========================================
    // END GAME
    // ==========================================

    private void endGame() {

        gameRunning = false;

        cancelTasks();

        remainingSeconds = 0;

        removeScoreboards();

        broadcast(
                "&eDeathSwap game has ended!"
        );
    }

    // ==========================================
    // TIMER COMMAND
    // ==========================================

    private void showTimer(
            CommandSender sender
    ) {

        if (!gameRunning) {

            send(
                    sender,
                    "&cDeathSwap is not running!"
            );

            return;
        }

        send(
                sender,
                "&eDeathSwap time remaining: &c"
                        + remainingSeconds
                        + " seconds"
        );
    }

    // ==========================================
    // SCORE COMMAND
    // ==========================================

    private void showScore(
            CommandSender sender
    ) {

        send(
                sender,
                "&eSCORE &7| "
                        + "&bTeam 1: &f"
                        + team1Score
                        + " &7- "
                        + "&cTeam 2: &f"
                        + team2Score
        );
    }

    private void broadcastScore() {

        broadcast(
                "&eSCORE &7| "
                        + "&bTeam 1: &f"
                        + team1Score
                        + " &7- "
                        + "&cTeam 2: &f"
                        + team2Score
        );
    }

    // ==========================================
    // TEAMS COMMAND
    // ==========================================

    private void showTeams(
            CommandSender sender
    ) {

        send(
                sender,
                "&bTeam 1: &f"
                        + (team1.isEmpty()
                        ? "None"
                        : String.join(
                                ", ",
                                team1
                        ))
        );

        send(
                sender,
                "&cTeam 2: &f"
                        + (team2.isEmpty()
                        ? "None"
                        : String.join(
                                ", ",
                                team2
                        ))
        );
    }

    // ==========================================
    // STOP
    // ==========================================

    private void stopGame(
            CommandSender sender
    ) {

        if (!gameRunning) {

            send(
                    sender,
                    "&cDeathSwap is not running!"
            );

            return;
        }

        gameRunning = false;

        cancelTasks();

        remainingSeconds = 0;

        removeScoreboards();

        broadcast(
                "&c&lDeathSwap STOPPED!"
        );

        send(
                sender,
                "&aTimer and game stopped."
        );
    }

    // ==========================================
    // RESET
    // ==========================================

    private void resetGame(
            CommandSender sender
    ) {

        gameRunning = false;

        cancelTasks();

        team1.clear();
        team2.clear();

        team1Score = 0;
        team2Score = 0;
        team1RoundDeaths = 0;
        team2RoundDeaths = 0;

        remainingSeconds = 0;

        removeScoreboards();

        broadcast(
                "&eDeathSwap has been completely reset!"
        );
    }


    // ==========================================
    // SCOREBOARD
    // ==========================================

    private void handleScoreboardCommand(
            CommandSender sender,
            String[] args
    ) {

        if (!(sender instanceof Player)) {

            send(
                    sender,
                    "&cThis command can only be used by a player."
            );

            return;
        }

        if (args.length < 2) {

            send(
                    sender,
                    "&eUsage: /deathswap scoreboard <show|hide>"
            );

            return;
        }

        Player player = (Player) sender;

        if (args[1].equalsIgnoreCase("show")) {

            hiddenScoreboards.remove(
                    player.getName().toLowerCase()
            );

            send(
                    player,
                    "&aDeathSwap scoreboard is now visible."
            );

            return;
        }

        if (args[1].equalsIgnoreCase("hide")) {

            hiddenScoreboards.add(
                    player.getName().toLowerCase()
            );

            Scoreboard mainBoard =
                    Bukkit.getScoreboardManager()
                            .getMainScoreboard();

            player.setScoreboard(mainBoard);

            send(
                    player,
                    "&eDeathSwap scoreboard is now hidden."
            );

            return;
        }

        send(
                player,
                "&cUsage: /deathswap scoreboard <show|hide>"
        );
    }

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

                if (Bukkit.getScoreboardManager() == null) {
                    return;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {

                    if (hiddenScoreboards.contains(
                            player.getName().toLowerCase()
                    )) {
                        continue;
                    }

                    Scoreboard board =
                            Bukkit.getScoreboardManager()
                                    .getNewScoreboard();

                    Objective objective =
                            board.registerNewObjective(
                                    "deathswap",
                                    "dummy",
                                    color("&c&lDEATH SWAP")
                            );

                    objective.setDisplaySlot(
                            DisplaySlot.SIDEBAR
                    );

                    objective.getScore(
                            color("&7----------------")
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
                            color("&7----------------")
                    ).setScore(2);

                    objective.getScore(
                            color("&a/deathswap score")
                    ).setScore(1);

                    player.setScoreboard(board);
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int remaining = seconds % 60;

        return String.format(
                "%02d:%02d",
                minutes,
                remaining
        );
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
                Bukkit.getScoreboardManager()
                        .getMainScoreboard();

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(mainBoard);
        }
    }

    // ==========================================
    // CANCEL TASKS
    // ==========================================

    private void cancelTasks() {

        if (gameTask != null) {

            gameTask.cancel();

            gameTask = null;
        }

        if (scoreboardTask != null) {

            scoreboardTask.cancel();

            scoreboardTask = null;
        }
    }

    // ==========================================
    // TAB COMPLETION
    // ==========================================

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {

        if (!command.getName()
                .equalsIgnoreCase("deathswap")) {

            return new ArrayList<>();
        }

        if (args.length == 1) {

            List<String> commands =
                    Arrays.asList(
                            "team1",
                            "team2",
                            "start",
                            "stop",
                            "reset",
                            "timer",
                            "score",
                            "teams",
                            "scoreboard"
                    );

            List<String> result =
                    new ArrayList<>();

            for (String option : commands) {

                if (option.toLowerCase()
                        .startsWith(
                                args[0].toLowerCase()
                        )) {

                    result.add(option);
                }
            }

            return result;
        }

        if (args.length == 2 &&
                args[0].equalsIgnoreCase("scoreboard")) {

            return Arrays.asList("show", "hide");
        }

        if (args.length >= 2 &&
                (args[0].equalsIgnoreCase("team1") ||
                 args[0].equalsIgnoreCase("team2"))) {

            List<String> players =
                    new ArrayList<>();

            String input =
                    args[args.length - 1]
                            .toLowerCase();

            for (Player player :
                    Bukkit.getOnlinePlayers()) {

                if (player.getName()
                        .toLowerCase()
                        .startsWith(input)) {

                    players.add(
                            player.getName()
                    );
                }
            }

            return players;
        }

        return new ArrayList<>();
    }
}