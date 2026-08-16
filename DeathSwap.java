package me.deathswap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DeathSwap extends JavaPlugin implements CommandExecutor, Listener, TabCompleter {

    private final List<String> team1 = new ArrayList<>();
    private final List<String> team2 = new ArrayList<>();

    private final Set<String> hiddenScoreboards = new HashSet<>();
    private final Map<String, Location> lastLocations = new HashMap<>();
    private final Map<String, Location> pendingJoinLocations = new HashMap<>();

    private BukkitTask gameTask;
    private BukkitTask scoreboardTask;
    private BukkitTask countdownTask;

    private boolean gameRunning;
    private boolean firstSwapCompleted;
    private int remainingSeconds;

    private int team1Score;
    private int team2Score;

    // Deaths are counted only after a swap and are scored at the next swap.
    private int team1RoundDeaths;
    private int team2RoundDeaths;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (getCommand("deathswap") != null) {
            getCommand("deathswap").setExecutor(this);
            getCommand("deathswap").setTabCompleter(this);
        }

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("DeathSwap enabled!");
    }

    @Override
    public void onDisable() {
        cancelTasks();
        removeScoreboards();
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private void send(CommandSender sender, String text) {
        sender.sendMessage(color(getConfig().getString("messages.prefix", "") + text));
    }

    private void broadcast(String text) {
        Bukkit.broadcastMessage(color(getConfig().getString("messages.prefix", "") + text));
    }

    private boolean isAdmin(CommandSender sender) {
        return sender.hasPermission("deathswap.admin") || sender.isOp();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("deathswap")) return true;

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

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

        if (!isAdmin(sender)) {
            send(sender, "&cYou don't have permission to use that command.");
            return true;
        }

        switch (sub) {
            case "team1" -> addPlayersToTeam(sender, args, team1, 1);
            case "team2" -> addPlayersToTeam(sender, args, team2, 2);
            case "start" -> startGame(sender);
            case "stop" -> stopGame(sender);
            case "reset" -> resetGame(sender);
            default -> send(sender, "&cUnknown command. Use &e/deathswap&c for help.");
        }
        return true;
    }

    private void showHelp(CommandSender sender) {
        send(sender, "&6&lDeathSwap Commands");
        send(sender, "&e/deathswap timer &7- Show remaining time");
        send(sender, "&e/deathswap score &7- Show score");
        send(sender, "&e/deathswap teams &7- Show teams");
        send(sender, "&e/deathswap scoreboard show &7- Show scoreboard");
        send(sender, "&e/deathswap scoreboard hide &7- Hide scoreboard");

        if (isAdmin(sender)) {
            send(sender, "&e/deathswap team1 <players>");
            send(sender, "&e/deathswap team2 <players>");
            send(sender, "&e/deathswap start");
            send(sender, "&e/deathswap stop");
            send(sender, "&e/deathswap reset");
        }
    }

    private void addPlayersToTeam(CommandSender sender, String[] args, List<String> team, int teamNumber) {
        if (gameRunning) {
            send(sender, "&cYou cannot change teams while DeathSwap is running!");
            return;
        }
        if (args.length < 2) {
            send(sender, "&cUsage: /deathswap team" + teamNumber + " <player1> <player2> ...");
            return;
        }

        int added = 0;
        int alreadyInTeam = 0;
        for (int i = 1; i < args.length; i++) {
            String playerName = args[i];
            if (teamNumber == 1) {
                team2.removeIf(name -> name.equalsIgnoreCase(playerName));
            } else {
                team1.removeIf(name -> name.equalsIgnoreCase(playerName));
            }

            boolean exists = false;
            for (String name : team) {
                if (name.equalsIgnoreCase(playerName)) {
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

        send(sender, "&aTeam " + teamNumber + " updated!");
        if (added > 0) send(sender, "&aAdded players: &f" + added);
        if (alreadyInTeam > 0) send(sender, "&eAlready in Team " + teamNumber + ": &f" + alreadyInTeam);
    }

    private void startGame(CommandSender sender) {
        if (gameRunning) {
            send(sender, "&cGame is already running!");
            return;
        }
        if (team1.isEmpty()) {
            send(sender, "&cTeam 1 has no players!");
            return;
        }
        if (team2.isEmpty()) {
            send(sender, "&cTeam 2 has no players!");
            return;
        }

        team1Score = 0;
        team2Score = 0;
        team1RoundDeaths = 0;
        team2RoundDeaths = 0;
        firstSwapCompleted = false;
        gameRunning = true;

        rememberOnlineLocations();
        broadcast("&aDeathSwap game is starting!");
        startCountdown();
        startScoreboard();
    }

    private void startCountdown() {
        if (countdownTask != null) countdownTask.cancel();
        countdownTask = new BukkitRunnable() {
            int count = getConfig().getInt("start-countdown-seconds", 3);

            @Override
            public void run() {
                if (!gameRunning) {
                    cancel();
                    countdownTask = null;
                    return;
                }
                if (count <= 0) {
                    broadcast("&c&lTHE DEATH SWAP IS STARTING NOW!");
                    startTimer();
                    cancel();
                    countdownTask = null;
                    return;
                }
                broadcast("&eStarting in &c" + count + "&e...");
                count--;
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private void startTimer() {
        if (gameTask != null) gameTask.cancel();
        remainingSeconds = getConfig().getInt("swap-time-seconds", 300);

        gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!gameRunning) {
                    cancel();
                    gameTask = null;
                    return;
                }

                if (remainingSeconds <= 10 && remainingSeconds > 0) {
                    broadcast("&c&l" + remainingSeconds);
                    remainingSeconds--;
                    return;
                }

                if (remainingSeconds <= 0) {
                    performSwap();
                    remainingSeconds = getConfig().getInt("swap-time-seconds", 300);
                    return;
                }

                remainingSeconds--;
                if (remainingSeconds == 60 || remainingSeconds == 30) {
                    broadcast("&eSwap in &c" + remainingSeconds + " seconds!");
                }
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    private void performSwap() {
        // Score the deaths that happened since the previous swap.
        if (firstSwapCompleted) {
            resolveRoundDeaths();
            if (!gameRunning) return;
        }

        rememberOnlineLocations();
        broadcast("&b&lDEATH SWAP!");
        playSwapSound();

        List<Player> players1 = getOnlinePlayers(team1);
        List<Player> players2 = getOnlinePlayers(team2);
        List<Location> locations1 = getTeamLocations(team1);
        List<Location> locations2 = getTeamLocations(team2);

        if (locations1.isEmpty() || locations2.isEmpty()) {
            broadcast("&cSwap skipped because a team has no usable location.");
            return;
        }

        Collections.shuffle(players1);
        Collections.shuffle(players2);
        Collections.shuffle(locations1);
        Collections.shuffle(locations2);

        for (int i = 0; i < players1.size(); i++) {
            Location target = locations2.get(i % locations2.size());
            players1.get(i).teleport(target.clone());
            sendSwapMessage(players1.get(i));
        }
        for (int i = 0; i < players2.size(); i++) {
            Location target = locations1.get(i % locations1.size());
            players2.get(i).teleport(target.clone());
            sendSwapMessage(players2.get(i));
        }

        // Offline players receive their target location when they join.
        for (String name : team1) {
            if (Bukkit.getPlayerExact(name) == null) {
                Location target = locations2.get(team1.indexOf(name) % locations2.size()).clone();
                pendingJoinLocations.put(key(name), target);
            }
        }
        for (String name : team2) {
            if (Bukkit.getPlayerExact(name) == null) {
                Location target = locations1.get(team2.indexOf(name) % locations1.size()).clone();
                pendingJoinLocations.put(key(name), target);
            }
        }

        team1RoundDeaths = 0;
        team2RoundDeaths = 0;
        firstSwapCompleted = true;
        broadcast("&aRandom team swap completed!");
        broadcastScore();
    }

    private void resolveRoundDeaths() {
        if (team1RoundDeaths == team2RoundDeaths) {
            if (team1RoundDeaths > 0) {
                broadcast("&eBoth teams had " + team1RoundDeaths + " death(s). No points awarded.");
            }
            return;
        }

        if (team1RoundDeaths > team2RoundDeaths) {
            int points = team1RoundDeaths - team2RoundDeaths;
            team2Score += points;
            broadcast("&aTeam 2 gets &e" + points + " point(s) &abecause Team 1 had more deaths!");
        } else {
            int points = team2RoundDeaths - team1RoundDeaths;
            team1Score += points;
            broadcast("&aTeam 1 gets &e" + points + " point(s) &abecause Team 2 had more deaths!");
        }
        broadcastScore();
        checkWinner();
    }

    private void rememberOnlineLocations() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (containsIgnoreCase(team1, player.getName()) || containsIgnoreCase(team2, player.getName())) {
                lastLocations.put(key(player.getName()), player.getLocation().clone());
            }
        }
    }

    private List<Location> getTeamLocations(List<String> team) {
        List<Location> result = new ArrayList<>();
        for (String name : team) {
            Player player = Bukkit.getPlayerExact(name);
            if (player != null && player.isOnline()) {
                result.add(player.getLocation().clone());
            } else {
                Location saved = lastLocations.get(key(name));
                if (saved != null) result.add(saved.clone());
            }
        }
        return result;
    }

    private List<Player> getOnlinePlayers(List<String> names) {
        List<Player> result = new ArrayList<>();
        for (String name : names) {
            Player player = Bukkit.getPlayerExact(name);
            if (player != null && player.isOnline()) result.add(player);
        }
        return result;
    }

    private void sendSwapMessage(Player player) {
        player.sendMessage(color("&b&lDEATH SWAP! &fYou have been swapped!"));
    }

    private void playSwapSound() {
        String soundName = getConfig().getString("swap-sound", "ENTITY_TNT_PRIMED");
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase(Locale.ROOT));
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            }
        } catch (IllegalArgumentException ignored) {
            getLogger().warning("Invalid swap-sound in config: " + soundName);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!gameRunning || !firstSwapCompleted) return;

        String name = event.getEntity().getName();
        if (containsIgnoreCase(team1, name)) {
            team1RoundDeaths++;
        } else if (containsIgnoreCase(team2, name)) {
            team2RoundDeaths++;
        } else {
            return;
        }

        broadcast("&c" + name + " &7died. Death recorded for the current swap round.");
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!gameRunning) return;
        event.getPlayer().sendMessage(color("&eYou are back in DeathSwap!"));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!gameRunning) return;
        Player player = event.getPlayer();
        String name = player.getName();
        if (containsIgnoreCase(team1, name) || containsIgnoreCase(team2, name)) {
            lastLocations.put(key(name), player.getLocation().clone());
            broadcast("&7" + name + " left the server. No point is awarded.");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Location pending = pendingJoinLocations.remove(key(player.getName()));
        if (pending != null) {
            Bukkit.getScheduler().runTask(this, () -> {
                if (player.isOnline()) player.teleport(pending.clone());
            });
        }
        if (containsIgnoreCase(team1, player.getName()) || containsIgnoreCase(team2, player.getName())) {
            lastLocations.put(key(player.getName()), player.getLocation().clone());
        }
    }

    private boolean containsIgnoreCase(List<String> list, String value) {
        for (String s : list) if (s.equalsIgnoreCase(value)) return true;
        return false;
    }

    private String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private void checkWinner() {
        int pointsToWin = getConfig().getInt("points-to-win", 3);
        if (team1Score >= pointsToWin) {
            broadcast("&b&lTEAM 1 WINS!");
            endGame();
        } else if (team2Score >= pointsToWin) {
            broadcast("&c&lTEAM 2 WINS!");
            endGame();
        }
    }

    private void endGame() {
        gameRunning = false;
        cancelTasks();
        remainingSeconds = 0;
        removeScoreboards();
        broadcast("&eDeathSwap game has ended!");
    }

    private void showTimer(CommandSender sender) {
        if (!gameRunning) {
            send(sender, "&cDeathSwap is not running!");
            return;
        }
        send(sender, "&eDeathSwap time remaining: &c" + remainingSeconds + " seconds");
    }

    private void showScore(CommandSender sender) {
        send(sender, "&eSCORE &7| &bTeam 1: &f" + team1Score + " &7- &cTeam 2: &f" + team2Score);
    }

    private void broadcastScore() {
        broadcast("&eSCORE &7| &bTeam 1: &f" + team1Score + " &7- &cTeam 2: &f" + team2Score);
    }

    private void showTeams(CommandSender sender) {
        send(sender, "&bTeam 1: &f" + (team1.isEmpty() ? "None" : String.join(", ", team1)));
        send(sender, "&cTeam 2: &f" + (team2.isEmpty() ? "None" : String.join(", ", team2)));
    }

    private void stopGame(CommandSender sender) {
        if (!gameRunning) {
            send(sender, "&cDeathSwap is not running!");
            return;
        }
        gameRunning = false;
        cancelTasks();
        remainingSeconds = 0;
        removeScoreboards();
        broadcast("&c&lDeathSwap STOPPED!");
        send(sender, "&aTimer and game stopped.");
    }

    private void resetGame(CommandSender sender) {
        gameRunning = false;
        cancelTasks();
        team1.clear();
        team2.clear();
        team1Score = 0;
        team2Score = 0;
        team1RoundDeaths = 0;
        team2RoundDeaths = 0;
        firstSwapCompleted = false;
        remainingSeconds = 0;
        lastLocations.clear();
        pendingJoinLocations.clear();
        hiddenScoreboards.clear();
        removeScoreboards();
        broadcast("&eDeathSwap has been completely reset!");
    }

    private void handleScoreboardCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "&cThis command can only be used by a player.");
            return;
        }
        if (args.length < 2) {
            send(sender, "&eUsage: /deathswap scoreboard <show|hide>");
            return;
        }
        if (args[1].equalsIgnoreCase("hide")) {
            hiddenScoreboards.add(key(player.getName()));
            setMainScoreboard(player);
            send(sender, "&aScoreboard hidden.");
        } else if (args[1].equalsIgnoreCase("show")) {
            hiddenScoreboards.remove(key(player.getName()));
            updateScoreboard(player);
            send(sender, "&aScoreboard shown.");
        } else {
            send(sender, "&cUsage: /deathswap scoreboard <show|hide>");
        }
    }

    private void startScoreboard() {
        if (scoreboardTask != null) scoreboardTask.cancel();
        scoreboardTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!gameRunning) {
                    cancel();
                    scoreboardTask = null;
                    return;
                }
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!hiddenScoreboards.contains(key(player.getName()))) updateScoreboard(player);
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private void updateScoreboard(Player player) {
        if (Bukkit.getScoreboardManager() == null) return;
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("deathswap", "dummy", color("&c&lDEATH SWAP"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.getScore(color("&7----------------")).setScore(6);
        objective.getScore(color("&fTime: &e" + formatTime(remainingSeconds))).setScore(5);
        objective.getScore(color("&bTeam 1: &f" + team1Score)).setScore(4);
        objective.getScore(color("&cTeam 2: &f" + team2Score)).setScore(3);
        objective.getScore(color("&7Round deaths: &f" + team1RoundDeaths + " - " + team2RoundDeaths)).setScore(2);
        objective.getScore(color("&e/deathswap score")).setScore(1);
        player.setScoreboard(board);
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int remaining = seconds % 60;
        return String.format("%02d:%02d", minutes, remaining);
    }

    private void setMainScoreboard(Player player) {
        if (Bukkit.getScoreboardManager() != null) player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    private void removeScoreboards() {
        if (scoreboardTask != null) {
            scoreboardTask.cancel();
            scoreboardTask = null;
        }
        if (Bukkit.getScoreboardManager() == null) return;
        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player player : Bukkit.getOnlinePlayers()) player.setScoreboard(mainBoard);
    }

    private void cancelTasks() {
        if (gameTask != null) { gameTask.cancel(); gameTask = null; }
        if (scoreboardTask != null) { scoreboardTask.cancel(); scoreboardTask = null; }
        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("deathswap")) return new ArrayList<>();

        if (args.length == 1) {
            List<String> commands = new ArrayList<>(Arrays.asList("timer", "score", "teams", "scoreboard"));
            if (isAdmin(sender)) commands.addAll(Arrays.asList("team1", "team2", "start", "stop", "reset"));
            List<String> result = new ArrayList<>();
            for (String option : commands) if (option.startsWith(args[0].toLowerCase(Locale.ROOT))) result.add(option);
            return result;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("scoreboard")) return Arrays.asList("show", "hide");

        if (args.length >= 2 && isAdmin(sender) && (args[0].equalsIgnoreCase("team1") || args[0].equalsIgnoreCase("team2"))) {
            List<String> players = new ArrayList<>();
            String input = args[args.length - 1].toLowerCase(Locale.ROOT);
            for (Player player : Bukkit.getOnlinePlayers()) if (player.getName().toLowerCase(Locale.ROOT).startsWith(input)) players.add(player.getName());
            return players;
        }
        return new ArrayList<>();
    }
}
