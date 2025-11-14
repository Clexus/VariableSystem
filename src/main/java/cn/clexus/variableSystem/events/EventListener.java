package cn.clexus.variableSystem.events;

import cn.clexus.variableSystem.VariableSystem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class EventListener implements Listener {

    @EventHandler
    public void onPlayerJoin(AsyncPlayerPreLoginEvent event) {
        var UUIDMode = VariableSystem.instance.isUUIDMode();
        VariableSystem.instance.getVariableManager().loadVariables(UUIDMode ? event.getPlayerProfile().getId().toString() : event.getPlayerProfile().getName());
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        VariableSystem.instance.getDatabaseManager().savePlayerVariables(event.getPlayer());
    }
}
