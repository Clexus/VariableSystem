package cn.clexus.variableSystem;

import cn.clexus.variableSystem.commands.VariableCommand;
import cn.clexus.variableSystem.config.VariableConfigLoader;
import cn.clexus.variableSystem.events.EventListener;
import cn.clexus.variableSystem.service.VariableManager;
import cn.clexus.variableSystem.storage.DatabaseManager;
import cn.clexus.variableSystem.support.PlaceholderAPISupport;
import cn.clexus.variableSystem.tasks.SyncAndExpireTask;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;

@Getter
public final class VariableSystem extends JavaPlugin {

    public static VariableSystem instance;
    private VariableConfigLoader loader;
    private VariableManager variableManager;
    private DatabaseManager databaseManager;
    private boolean UUIDMode;
    private static final MiniMessage mm = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        databaseManager = new DatabaseManager();
        if (getConfig().getBoolean("connect")) {
            databaseManager.connect();
        } else {
            info("请配置好数据库信息并将connect改为true开始连接数据库");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        UUIDMode = getConfig().getBoolean("UUIDMode", true);
        loader = new VariableConfigLoader(this);
        variableManager = new VariableManager();
        loader.load();
        variableManager.loadGlobalVariables();
        getServer().getPluginManager().registerEvents(new EventListener(), this);
        new SyncAndExpireTask().runTaskTimer(this, 12000, 12000);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(
                    VariableCommand.register()
            );
        });
        PlaceholderAPISupport.init();
    }

    public void warn(String message) {
        getComponentLogger().warn(mm.deserialize(message));
    }

    public void info(String message) {
        getComponentLogger().info(mm.deserialize(message));
    }

    @Override
    public void onDisable() {
        info("正在保存数据...");
        CompletableFuture<?>[] playerFutures = getServer().getOnlinePlayers().stream()
                .map(player -> databaseManager.savePlayerVariables(player))
                .toArray(CompletableFuture[]::new);

        CompletableFuture<Boolean> globalFuture = variableManager.saveGlobalVariables();

        CompletableFuture<Void> allDone = CompletableFuture.allOf(playerFutures)
                .thenCombine(globalFuture, (v, globalSaved) -> {
                    if (!globalSaved) {
                        warn("全局变量保存失败！");
                    }
                    return null;
                });

        try {
            allDone.join();
            info("所有玩家变量和全局变量已保存完毕，正在关闭数据库...");
        } catch (Exception e) {
            warn("保存数据时发生异常: " + e.getMessage());
        } finally {
            databaseManager.close();
            info("数据库连接已关闭，插件安全卸载完成");
        }
    }

    public String getIdentifier(Player player) {
        if(UUIDMode) return player.getUniqueId().toString();
        return player.getName();
    }
}
