package cn.clexus.variableSystem.config;

import cn.clexus.variableSystem.VariableSystem;
import cn.clexus.variableSystem.model.VariableDefinition;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VariableConfigLoader {

    private final VariableSystem plugin;
    private final File variablesFolder;
    @Getter
    private final Set<VariableDefinition<?>> definitions = new HashSet<>();
    private final Map<String, String> names = new HashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public VariableConfigLoader(VariableSystem plugin) {
        this.plugin = plugin;
        this.variablesFolder = new File(plugin.getDataFolder(), "variables");
    }

    public void load() {
        if (!variablesFolder.exists()) {
            variablesFolder.mkdir();
        }
        Logger logger = plugin.getLogger();
        File[] files = variablesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            logger.info("variables 文件夹中没有配置文件。");
            return;
        }
        for (File file : files) {
            try {
                String fileName = file.getName();
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                for (var key : config.getKeys(false)) {
                    if(names.containsKey(key)){
                        plugin.warn(fileName+"中的变量"+key+"与在"+names.get(key)+"中的已有变量ID重复，已跳过");
                        continue;
                    }else{
                        names.put(key, fileName);
                    }
                    var section = config.getConfigurationSection(key);
                    if (section == null) continue;
                    String type = section.getString("type");
                    if (type == null) continue;
                    double min = section.getDouble("min", Double.MIN_VALUE);
                    double max = section.getDouble("max", Double.MAX_VALUE);
                    String expireAtStr = section.getString("expireAt", null);
                    long expireAt = expireAtStr == null ? -1 : dateFormat.parse(expireAtStr).getTime();
                    String expireAfterStr = section.getString("expireAfter", null);
                    long expireAfter = expireAfterStr == null ? -1 : parseDuration(expireAfterStr);
                    switch (type.toLowerCase()) {
                        case "boolean" -> {
                            Boolean d = section.getBoolean("default");
                            definitions.add(new VariableDefinition<>(key, d, Boolean.class, min, max, expireAt, expireAfter));
                        }
                        case "int" -> {
                            Long d = section.getLong("default");
                            definitions.add(new VariableDefinition<>(key, d, Long.class, min, max, expireAt, expireAfter));
                        }
                        case "float" -> {
                            Double d = section.getDouble("default");
                            definitions.add(new VariableDefinition<>(key, d, Double.class, min, max, expireAt, expireAfter));
                        }
                        case "string" -> {
                            String d = section.getString("default", "");
                            definitions.add(new VariableDefinition<>(key, d, String.class, min, max, expireAt, expireAfter));
                        }
                        case "numberlist" -> {
                            List<Double> d = section.getDoubleList("default");
                            definitions.add(new VariableDefinition<>(key, d, List.class, min, max, expireAt, expireAfter));
                        }
                        case "stringlist" -> {
                            List<String> d = section.getStringList("default");
                            definitions.add(new VariableDefinition<>(key, d, List.class, min, max, expireAt, expireAfter));
                        }
                        case "stringnumbermap" -> {
                            ConfigurationSection mapSection = section.getConfigurationSection("default");
                            Map<String, Double> d = new HashMap<>();
                            if(mapSection != null){
                                mapSection.getKeys(false).forEach(str -> {
                                    if(mapSection.isDouble(str)){
                                        d.put(str, mapSection.getDouble(str));
                                    }
                                });
                            }
                            definitions.add(new VariableDefinition<>(key, d, Map.class, min, max, expireAt, expireAfter));
                        }
                        case "stringstringmap" -> {
                            ConfigurationSection mapSection = section.getConfigurationSection("default");
                            Map<String, String> d = new HashMap<>();
                            if(mapSection != null){
                                mapSection.getKeys(false).forEach(str -> {
                                    if(mapSection.isString(str)){
                                        d.put(str, mapSection.getString(str, ""));
                                    }
                                });
                            }
                            definitions.add(new VariableDefinition<>(key, d, Map.class, min, max, expireAt, expireAfter));
                        }
                    }
                }
                logger.info("成功加载变量配置文件: " + file.getName());
            } catch (Exception e) {
                logger.log(Level.WARNING, "加载变量文件失败: " + file.getName(), e);
            }
        }
        names.clear();
    }

    private long parseDuration(String input) {
        long totalMillis = 0;
        int number = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c >= '0' && c <= '9') {
                number = number * 10 + (c - '0');
            } else {
                if (c == 'd') {
                    totalMillis += number * 24L * 60L * 60L * 1000L;
                } else if (c == 'h') {
                    totalMillis += number * 60L * 60L * 1000L;
                } else if (c == 'm') {
                    totalMillis += number * 60L * 1000L;
                } else if (c == 's') {
                    totalMillis += number * 1000L;
                }
                number = 0;
            }
        }

        return totalMillis;
    }
}
