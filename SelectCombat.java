package com.selectcombat;

import com.selectcombat.combat.CombatManager;
import com.selectcombat.combat.CombatTimer;
import com.selectcombat.command.CombatCommand;
import com.selectcombat.listener.CombatListener;
import com.selectcombat.listener.KillEffectListener;
import com.selectcombat.listener.WandListener;
import com.selectcombat.region.RegionManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SelectCombat extends JavaPlugin {

    private static SelectCombat instance;

    private RegionManager regionManager;
    private CombatManager combatManager;
    private CombatTimer combatTimer;
    private WandListener wandListener;

    @Override
    public void onEnable() {

        instance = this;

        saveDefaultConfig();

        // Managers
        regionManager = new RegionManager(this);
        combatManager = new CombatManager(this);
        combatTimer = new CombatTimer(this, combatManager);

        // ONE WandListener instance.
        // The command and event listener use the same selection data.
        wandListener = new WandListener();

        // Command
        PluginCommand command = getCommand("scombat");

        if (command == null) {
            getLogger().severe(
                    "Could not register /scombat!"
            );

            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        command.setExecutor(
                new CombatCommand(
                        this,
                        regionManager,
                        wandListener
                )
        );

        // Listeners
        getServer().getPluginManager().registerEvents(
        new CombatListener(
                this,
                combatManager,
                regionManager
        ),
        this
);

        getServer().getPluginManager().registerEvents(
                wandListener,
                this
        );

        getServer().getPluginManager().registerEvents(
                new KillEffectListener(
                        combatManager,
                        regionManager
                ),
                this
        );

        // Start combat timer
        combatTimer.start();

        getLogger().info("================================");
        getLogger().info("       SELECT-COMBAT ENABLED");
        getLogger().info("       Version: " + getDescription().getVersion());
        getLogger().info("================================");
    }

    @Override
    public void onDisable() {

        if (combatTimer != null) {
            combatTimer.shutdown();
        }

        if (combatManager != null) {
            combatManager.endAllCombats();
        }

        if (regionManager != null) {
            regionManager.save();
        }

        instance = null;

        getLogger().info("SELECT-COMBAT disabled.");
    }

    public static SelectCombat getInstance() {
        return instance;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public CombatTimer getCombatTimer() {
        return combatTimer;
    }

    public WandListener getWandListener() {
        return wandListener;
    }
}