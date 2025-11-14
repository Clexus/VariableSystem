package cn.clexus.variableSystem.service;

import cn.clexus.variableSystem.VariableSystem;
import cn.clexus.variableSystem.model.*;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class VariableManager {

    private static final VariableSystem plugin = VariableSystem.instance;
    private final Map<String, Variable<?>> globalMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Variable<?>>> playerMap = new ConcurrentHashMap<>();

    public CompletableFuture<Boolean> loadVariables(Player player) {
        return loadVariables(plugin.getIdentifier(player));
    }

    public CompletableFuture<Boolean> loadVariables(String player) {
        if (playerMap.containsKey(player)) {
            return CompletableFuture.completedFuture(true);
        }

        return plugin.getDatabaseManager().getPlayerVariables(player)
                .thenApply(vars -> {
                    if (vars == null) {
                        plugin.warn("未能从数据库加载玩家 " + player + " 的变量。");
                        return false;
                    }
                    playerMap.put(player, vars);
                    return true;
                })
                .exceptionally(ex -> {
                    plugin.warn("加载玩家变量时出错: " + ex.getMessage());
                    return false;
                });
    }

    public CompletableFuture<Boolean> loadGlobalVariables() {
        return plugin.getDatabaseManager().getGlobalVariables().thenApply(vars -> {
            globalMap.putAll(vars);
            return true;
        }).exceptionally(ex -> {
            plugin.warn("加载全局变量时出错: " + ex.getMessage());
            return false;
        });
    }

    public CompletableFuture<Boolean> saveGlobalVariables() {
        return plugin.getDatabaseManager().saveGlobalVariables(globalMap);
    }

    public void savePlayerVariables() {
        for(var player : plugin.getVariableManager().getPlayerMap().keySet()){
            plugin.getDatabaseManager().savePlayerVariables(player).thenAccept(state -> {
                if(plugin.isUUIDMode() && player.contains("-")){
                  if(Bukkit.getPlayer(UUID.fromString(player)) == null){
                      plugin.getVariableManager().getPlayerMap().remove(player);
                  }
                } else if(Bukkit.getPlayerExact(player) == null){
                    plugin.getVariableManager().getPlayerMap().remove(player);
                }
            });
        }
    }

    public CompletableFuture<Boolean> addVariable(String player, Variable<?> variable) {
        if (variable == null) {
            return CompletableFuture.completedFuture(false);
        }
        Player player1;
        if(plugin.isUUIDMode() && !player.contains("-")){
            player = getOfflinePlayer(player).getUniqueId().toString();
            player1 = Bukkit.getPlayer(UUID.fromString(player));
        }else{
            player1 = Bukkit.getPlayerExact(player);
        }
        if (player1 != null || playerMap.containsKey(player)) {
            playerMap.computeIfAbsent(player, k -> new HashMap<>());
            var id = variable.getVariableDefinition().id();
            if (playerMap.get(player).containsKey(id)) {
                return CompletableFuture.completedFuture(false);
            }
            playerMap.get(player).put(id, variable);
            return CompletableFuture.completedFuture(true);
        }

        String finalPlayer = player;
        return loadVariables(player).thenApply(loaded -> {
            if (!loaded) return false;

            playerMap.computeIfAbsent(finalPlayer, k -> new HashMap<>());
            var id = variable.getVariableDefinition().id();
            if (playerMap.get(finalPlayer).containsKey(id)) {
                return false;
            }
            playerMap.get(finalPlayer).put(id, variable);
            return true;
        });
    }

    public CompletableFuture<Boolean> addVariable(Player player, Variable<?> variable) {
        return addVariable(plugin.getIdentifier(player), variable);
    }

    public CompletableFuture<Boolean> addVariable(String player, String id, Object value) {
        Variable<?> variable = toVariable(id, value);
        return addVariable(player, variable);
    }

    public CompletableFuture<Boolean> addVariable(String player, String id) {
        Variable<?> variable = toVariable(id, null);
        return addVariable(player, variable);
    }

    @SuppressWarnings("unchecked")
    public Variable<?> toVariable(String id, Object value) {
        var definitions = plugin.getLoader().getDefinitions();
        VariableDefinition<?> def = null;
        for (VariableDefinition<?> definition : definitions) {
            if (definition.id().equals(id)) {
                def = definition;
                break;
            }
        }
        if (def == null) {
            plugin.warn("未找到对应变量: " + id);
            return null;
        }
        if (value != null && !def.type().isAssignableFrom(value.getClass())) {
            plugin.warn(id + "变量不支持" + value.getClass().getSimpleName() + "类型的值");
            return null;
        }
        if (value == null) {
            value = def.defaultValue();
        }
        var time = System.currentTimeMillis();
        Variable<?> variable = null;
        if (def.type().equals(Boolean.class)) {
            variable = new BooleanVariable((VariableDefinition<Boolean>) def, (Boolean) value, time);
        } else if (def.type().equals(String.class)) {
            variable = new StringVariable((VariableDefinition<String>) def, (String) value, time);
        } else if (def.type().equals(Long.class)) {
            variable = new IntVariable((VariableDefinition<Long>) def, (Long) value, time);
        } else if (def.type().equals(Float.class)) {
            variable = new FloatVariable((VariableDefinition<Float>) def, (Float) value, time);
        } else if (def.type().equals(List.class)) {
            List<?> list = (List<?>) value;
            if (list.getFirst() instanceof String) {
                variable = new StringList((VariableDefinition<List<String>>) def, (List<String>) list, time);
            } else if (list.getFirst() instanceof Number) {
                variable = new NumberList((VariableDefinition<List<Double>>) def, (List<Double>) list, time);
            }
        } else if (def.type().equals(Map.class)) {
            Map<?, ?> map = (Map<?, ?>) value;
            if (map.values().iterator().next() instanceof String) {
                variable = new StringStringMap((VariableDefinition<Map<String, String>>) def, (Map<String, String>) map, time);
            } else if (map.values().iterator().next() instanceof Number) {
                variable = new StringNumberMap((VariableDefinition<Map<String, Double>>) def, (Map<String, Double>) map, time);
            }
        }
        return variable;
    }

    public void addVariable(Player player, String id, Object value) {
        addVariable(plugin.getIdentifier(player), id, value);
    }

    public boolean addGlobalVariable(String id, Object value) {
        return addGlobalVariable(toVariable(id, value));
    }

    public boolean addGlobalVariable(String id) {
        return addGlobalVariable(toVariable(id, null));
    }

    public boolean addGlobalVariable(Variable<?> variable) {
        if (variable == null) return false;
        var id = variable.getVariableDefinition().id();
        if(globalMap.containsKey(id)){
            return false;
        }
        globalMap.put(id, variable);
        return true;
    }

    public CompletableFuture<Boolean> removeVariable(String player, String id) {
        if (!playerMap.containsKey(player)) {
            return loadVariables(player).thenCompose(loaded -> {
                if (!loaded) {
                    return CompletableFuture.completedFuture(false);
                }
                return removeVariable(player, id);
            });
        }

        var map = playerMap.get(player);
        if (map == null || !map.containsKey(id)) {
            return CompletableFuture.completedFuture(false);
        }

        map.remove(id);
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> removeVariable(Player player, String id) {
        return removeVariable(plugin.getIdentifier(player), id);
    }

    public CompletableFuture<Boolean> removeVariable(Player player, Variable<?> variable) {
        return removeVariable(player, variable.getVariableDefinition().id());
    }

    public Variable<?> getOnlineVariable(Player player, String variableName) {
        return getOnlineVariable(plugin.getIdentifier(player), variableName);
    }

    public Variable<?> getOnlineVariable(String player, String variableName) {
        if(plugin.isUUIDMode() && !player.contains("-")){
            player = getOfflinePlayer(player).getUniqueId().toString();
        }
        Map<String, Variable<?>> vars = playerMap.get(player);
        if (vars == null) return null;
        var variable = vars.get(variableName);
        if (variable == null) return null;
        if (variable.expires()) {
            plugin.getDatabaseManager().expirePlayerVariable(player, variable);
            removeVariable(player, variableName);
            return null;
        }
        return variable;
    }

    public CompletableFuture<Variable<?>> getOfflineVariable(String offlinePlayer, String variableName) {
        if(plugin.isUUIDMode() && !offlinePlayer.contains("-")){
            offlinePlayer = Bukkit.getOfflinePlayer(offlinePlayer).getUniqueId().toString();
        }
        String finalOfflinePlayer = offlinePlayer;
        return plugin.getDatabaseManager().getPlayerVariables(offlinePlayer)
                .thenApply(vars -> {
                    if (vars == null) return null;
                    var variable = vars.get(variableName);
                    if (variable == null) return null;
                    if (variable.expires()) {
                        plugin.getDatabaseManager().expirePlayerVariable(finalOfflinePlayer, variable);
                        return null;
                    }
                    playerMap.put(finalOfflinePlayer, vars);
                    return variable;
                });
    }

    public CompletableFuture<Variable<?>> getVariable(String player, String variableName) {
        Variable<?> variable = getOnlineVariable(player, variableName);
        if (variable != null) {
            return CompletableFuture.completedFuture(variable);
        }
        return getOfflineVariable(player, variableName);
    }

    public CompletableFuture<Boolean> hasVariable(String player, String variableName) {
        if (hasOnlineVariable(player, variableName)) return CompletableFuture.completedFuture(true);
        return hasOfflineVariable(player, variableName);
    }

    public boolean hasOnlineVariable(String player, String variableName) {
        return getOnlineVariable(player, variableName) != null;
    }

    public CompletableFuture<Boolean> hasOfflineVariable(String offlinePlayer, String variableName) {
        return CompletableFuture.completedFuture(getOfflineVariable(offlinePlayer, variableName) != null);
    }

    public CompletableFuture<Double> sumAll(Variable<?> variable) {
        return sumAll(variable.getVariableDefinition().id());
    }

    public CompletableFuture<Double> sumAll(String varId) {
        double[] sum = new double[1];
        sum[0] = 0;

        Map<String, Map<String, Variable<?>>> onlineMap = plugin.getVariableManager().getPlayerMap();
        for (Map<String, Variable<?>> playerVars : onlineMap.values()) {
            Variable<?> v = playerVars.get(varId);
            if (v != null) {
                switch (v) {
                    case StringVariable stringVariable -> sum[0] += stringVariable.getValue().length();
                    case IntVariable intVariable -> sum[0] += intVariable.getValue();
                    case FloatVariable floatVariable -> sum[0] += floatVariable.getValue();
                    case NumberList listVariable -> sum[0] += listVariable.getValue().size();
                    case StringList listVariable -> sum[0] += listVariable.getValue().size();
                    case StringNumberMap mapVariable -> sum[0] += mapVariable.getValue().size();
                    case StringStringMap mapVariable -> sum[0] += mapVariable.getValue().size();
                    default -> sum[0] += 1;
                }
            }
        }

        return plugin.getDatabaseManager().getAllPlayerVariables().thenApply(offlineMap -> {
            for (Map.Entry<String, Map<String, Variable<?>>> entry : offlineMap.entrySet()) {
                String player = entry.getKey();
                if (onlineMap.containsKey(player)) continue;

                Variable<?> v = entry.getValue().get(varId);
                if (v != null) {
                    switch (v) {
                        case StringVariable stringVariable -> sum[0] += stringVariable.getValue().length();
                        case IntVariable intVariable -> sum[0] += intVariable.getValue();
                        case FloatVariable floatVariable -> sum[0] += floatVariable.getValue();
                        case NumberList listVariable -> sum[0] += listVariable.getValue().size();
                        case StringList listVariable -> sum[0] += listVariable.getValue().size();
                        case StringNumberMap mapVariable -> sum[0] += mapVariable.getValue().size();
                        case StringStringMap mapVariable -> sum[0] += mapVariable.getValue().size();
                        default -> sum[0] += 1;
                    }
                }
            }

            return sum[0];
        });
    }

    public @NotNull Map<String, Variable<?>> getOnlineVariables(String player) {
        if(plugin.isUUIDMode() && !player.contains("-")){
            player = getOfflinePlayer(player).getUniqueId().toString();
        }
        Map<String, Variable<?>> vars = playerMap.get(player);
        if(vars == null) return new HashMap<>();
        Map<String, Variable<?>> newMap = new HashMap<>();
        String finalPlayer = player;
        vars.forEach((k, v) -> {
            if (v.expires()) {
                plugin.getDatabaseManager().expirePlayerVariable(finalPlayer, v);
                removeVariable(finalPlayer, k);
            } else {
                newMap.put(k, v);
            }
        });
        return newMap;
    }

    public @NotNull CompletableFuture<Map<String, Variable<?>>> getOfflineVariables(String offlinePlayer) {
        if(plugin.isUUIDMode() && !offlinePlayer.contains("-")){
            offlinePlayer = Bukkit.getOfflinePlayer(offlinePlayer).getUniqueId().toString();
        }
        String finalOfflinePlayer = offlinePlayer;
        return plugin.getDatabaseManager().getPlayerVariables(offlinePlayer)
                .thenApply(vars -> {
                    Map<String, Variable<?>> result = new HashMap<>();
                    if (vars == null) return result;
                    vars.forEach((k, v) -> {
                        if (v.expires()) {
                            plugin.getDatabaseManager().expirePlayerVariable(finalOfflinePlayer, v);
                        } else {
                            result.put(k, v);
                        }
                    });
                    return result;
                });
    }

    public @NotNull CompletableFuture<Map<String, Variable<?>>> getVariables(String player) {
        var vars = getOnlineVariables(player);
        if(!vars.isEmpty()){
            return CompletableFuture.completedFuture(vars);
        }
        return getOfflineVariables(player);
    }

    public Variable<?> getGlobalVariable(String variableName) {
        return globalMap.get(variableName);
    }

    public OfflinePlayer getOfflinePlayer(String player) {
        var offlinePlayer = Bukkit.getOfflinePlayerIfCached(player);
        return offlinePlayer == null ? Bukkit.getOfflinePlayer(player) : offlinePlayer;
    }
}
