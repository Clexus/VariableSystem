package cn.clexus.variableSystem.tasks;

import cn.clexus.variableSystem.VariableSystem;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class SyncAndExpireTask extends BukkitRunnable {
    VariableSystem plugin = VariableSystem.instance;

    @Override
    public void run() {
        plugin.getVariableManager().saveGlobalVariables();
        plugin.getVariableManager().savePlayerVariables();
    }
}
