package cn.clexus.variableSystem.support;

import cn.clexus.variableSystem.VariableSystem;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderAPISupport {
    public static boolean usePlaceholderAPI = true;
    static PlaceholderAPIPlugin papiPlugin;
    private static boolean hasSupport = false;
    public static boolean hasSupport(){
        return hasSupport;
    }
    private static final VariableSystem plugin = VariableSystem.instance;
    public static void init() {
        try{
            Plugin PAPI = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI");
            if (!usePlaceholderAPI || !(PAPI instanceof PlaceholderAPIPlugin)) {
                return;
            }
            String papiVersion = PAPI.getPluginMeta().getVersion();
            plugin.info("已找到PlaceholderAPI: " + papiVersion);
            Pattern pattern = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
            Matcher matcher = pattern.matcher(papiVersion);
            if (matcher.find()) {
                int middleNumber = Integer.parseInt(matcher.group(2));
                int lastNumber = Integer.parseInt(matcher.group(3));
                if (middleNumber < 10 || (middleNumber == 10 && lastNumber < 2)) {
                    plugin.warn("需要 2.10.2 或更高的版本的 PlaceholderAPI, 已取消兼容");
                    hasSupport = false;
                    return;
                }
            } else {
                plugin.warn("解析 PlaceholderAPI 版本时出错, 已取消兼容");
                hasSupport = false;
                return;
            }
            hasSupport = true;
        }catch (Exception e){
            plugin.getLogger().log(Level.WARNING, "进行 PlaceholderAPI 兼容时出错", e);
            hasSupport = false;
        }
        if(hasSupport()){
            new PlaceholderAPIExpansion().register();
        }
    }
}
