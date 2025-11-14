package cn.clexus.variableSystem.commands;

import cn.clexus.variableSystem.VariableSystem;
import cn.clexus.variableSystem.model.*;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.arguments.StringArgumentType.string;

@SuppressWarnings("SameReturnValue")
public class VariableCommand {

    static VariableSystem plugin = VariableSystem.instance;
    static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    public static LiteralCommandNode<CommandSourceStack> register() {
        return Commands.literal("var")
                .then(Commands.literal("player")
                        .then(Commands.argument("player", string())
                                .suggests((ctx, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(player -> builder.suggest(player.getName()));
                                    return builder.buildFuture();
                                })
                                .then(Commands.literal("list").executes(VariableCommand::listPlayer))
                                .then(Commands.literal("get").then(Commands.argument("id", string())
                                        .suggests(VariableCommand::suggestPlayerVariableId)
                                        .executes(VariableCommand::getVariable)))
                                .then(Commands.literal("create").then(Commands.argument("id", string())
                                        .suggests(VariableCommand::suggestVariableId)
                                        .executes(VariableCommand::createVariableNoValue)
                                        .then(Commands.argument("value", string())
                                                .suggests(VariableCommand::suggestExampleValue)
                                                .executes(VariableCommand::createVariable))))
                                .then(Commands.literal("add")
                                        .then(Commands.argument("id", string())
                                                .suggests(VariableCommand::suggestAddableVariableId)
                                                .then(Commands.argument("value", string())
                                                        .suggests(VariableCommand::suggestValue)
                                                        .executes(VariableCommand::addVariable)
                                                        .then(Commands.argument("autoFix", BoolArgumentType.bool())
                                                                .executes(VariableCommand::addVariable)
                                                        )
                                                )
                                        )
                                )
                                .then(Commands.literal("set").then(Commands.argument("id", string())
                                        .suggests(VariableCommand::suggestPlayerVariableId)
                                        .then(Commands.argument("value", string())
                                                .suggests(VariableCommand::suggestValue)
                                                .executes(VariableCommand::setVariable))))
                                .then(Commands.literal("take").then(Commands.argument("id", string())
                                                .suggests(VariableCommand::suggestAddableVariableId)
                                                .then(Commands.argument("value", string())
                                                        .suggests(VariableCommand::suggestValue)
                                                        .executes(VariableCommand::takeVariable)
                                                        .then(Commands.argument("autoFix", BoolArgumentType.bool())
                                                                .executes(VariableCommand::takeVariable)
                                                        )
                                                )
                                        )
                                )
                                .then(Commands.literal("reset").then(Commands.argument("id", string())
                                                .suggests(VariableCommand::suggestPlayerVariableId)
                                                .executes(VariableCommand::resetPlayerVariable)
                                        )
                                )
                                .then(Commands.literal("remove").then(Commands.argument("id", string())
                                        .suggests(VariableCommand::suggestPlayerVariableId)
                                        .executes(VariableCommand::removeVariable)))
                        )
                )
                .then(Commands.literal("global")
                        .then(Commands.literal("list")
                                .executes(VariableCommand::listGlobal)
                        )
                        .then(Commands.literal("get")
                                .then(Commands.argument("id", string())
                                        .suggests(VariableCommand::suggestGlobal)
                                        .executes(VariableCommand::getVariableGlobal)
                                )
                        )
                        .then(Commands.literal("create")
                                .then(Commands.argument("id", string())
                                        .suggests(VariableCommand::suggestVariableId)
                                        .executes(VariableCommand::createVariableNoValueGlobal)
                                        .then(Commands.argument("value", string())
                                                .suggests(VariableCommand::suggestExampleValue)
                                                .executes(VariableCommand::createVariableGlobal)
                                        )
                                )
                        )
                        .then(Commands.literal("add")
                                .then(Commands.argument("id", string())
                                        .suggests(VariableCommand::suggestAddableVariableIdGlobal)
                                        .then(Commands.argument("value", string())
                                                .suggests(VariableCommand::suggestValueGlobal)
                                                .executes(VariableCommand::addVariableGlobal)
                                                .then(Commands.argument("autoFix", BoolArgumentType.bool())
                                                        .executes(VariableCommand::addVariableGlobal)
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("set").then(Commands.argument("id", string())
                                .suggests(VariableCommand::suggestGlobal)
                                .then(Commands.argument("value", string())
                                        .suggests(VariableCommand::suggestValueGlobal)
                                        .executes(VariableCommand::setVariableGlobal)))
                        )
                        .then(Commands.literal("take").then(Commands.argument("id", string())
                                        .suggests(VariableCommand::suggestAddableVariableIdGlobal)
                                        .then(Commands.argument("value", string())
                                                .suggests(VariableCommand::suggestValueGlobal)
                                                .executes(VariableCommand::takeVariableGlobal)
                                                .then(Commands.argument("autoFix", BoolArgumentType.bool())
                                                        .executes(VariableCommand::takeVariableGlobal)
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("reset").then(Commands.argument("id", string())
                                        .suggests(VariableCommand::suggestGlobal)
                                        .executes(VariableCommand::resetGlobalVariable)
                                )
                        )
                        .then(Commands.literal("remove").then(Commands.argument("id", string())
                                .suggests(VariableCommand::suggestGlobal)
                                .executes(VariableCommand::removeVariableGlobal))
                        )
                )
                .then(Commands.literal("sum")
                        .then(Commands.argument("id", string())
                                .suggests(VariableCommand::suggestVariableId)
                                .executes(ctx -> {
                                    plugin.getVariableManager().sumAll(ctx.getArgument("id", String.class)).thenAccept(result -> sendMessage(ctx, BigDecimal.valueOf(result).stripTrailingZeros().toPlainString()));
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            plugin.getLoader().getDefinitions().clear();
                            plugin.getLoader().load();
                            sendMessage(ctx, "重载完毕");
                            return 1;
                        })
                )
                .then(Commands.literal("save")
                        .executes(ctx -> {
                            plugin.getVariableManager().savePlayerVariables();
                            plugin.getVariableManager().saveGlobalVariables();
                            sendMessage(ctx, "已手动保存数据");
                            return 1;
                        })
                )
                .build();
    }

    private static CompletableFuture<Suggestions> suggestExampleValue(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String pName, varId;
        pName = "str";
        try {
            pName = ctx.getArgument("player", String.class);
        } catch (IllegalArgumentException ignored) {
        }
        varId = ctx.getArgument("id", String.class);
        VariableDefinition<?> def = plugin.getLoader().getDefinitions()
                .stream().filter(d -> d.id().equalsIgnoreCase(varId)).findFirst().orElse(null);
        if (def == null) return builder.buildFuture();
        if (def.type() == Boolean.class) {
            builder.suggest("true");
            builder.suggest("false");
        } else if (def.type() == String.class) {
            builder.suggest(pName);
        } else if (def.type() == Long.class) {
            builder.suggest("0");
        } else if (def.type() == Float.class) {
            builder.suggest("0.0");
        } else if (def.type() == List.class) {
            builder.suggest("[str,str]");
            builder.suggest("[0,1]");
        } else if (def.type() == Map.class) {
            builder.suggest("{key=str,key2=str2}");
            builder.suggest("{key=1,key2=2}");
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestGlobal(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        for (var variable : plugin.getVariableManager().getGlobalMap().keySet()) {
            builder.suggest(variable);
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestPlayerVariableId(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String playerName;
        try {
            playerName = ctx.getArgument("player", String.class);
        } catch (IllegalArgumentException ex) {
            return builder.buildFuture();
        }
        return plugin.getVariableManager().getVariables(playerName)
                .thenApply(variables -> {
                    if (variables != null) {
                        for (var variable : variables.values()) {
                            builder.suggest(variable.getVariableDefinition().id());
                        }
                    }
                    return builder.build();
                });
    }


    private static CompletableFuture<Suggestions> suggestVariableId(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        plugin.getLoader().getDefinitions().forEach(def -> builder.suggest(def.id()));
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestAddableVariableId(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String playerName;
        try {
            playerName = ctx.getArgument("player", String.class);
        } catch (IllegalArgumentException ex) {
            return builder.buildFuture();
        }

        return plugin.getVariableManager().getVariables(playerName)
                .thenApply(variables -> {
                    if (variables != null) {
                        for (var variable : variables.values()) {
                            if (variable.getVariableDefinition().type() != Boolean.class) {
                                builder.suggest(variable.getVariableDefinition().id());
                            }
                        }
                    }
                    return builder.build();
                });
    }

    private static CompletableFuture<Suggestions> suggestAddableVariableIdGlobal(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        plugin.getVariableManager().getGlobalMap().values().forEach(def -> {
            if (def.getVariableDefinition().type() != Boolean.class) {
                builder.suggest(def.getVariableDefinition().id());
            }
        });
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestValueGlobal(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String varId;
        try {
            varId = ctx.getArgument("id", String.class);
        } catch (IllegalArgumentException ex) {
            return builder.buildFuture();
        }

        VariableDefinition<?> def = plugin.getLoader().getDefinitions()
                .stream().filter(d -> d.id().equalsIgnoreCase(varId)).findFirst().orElse(null);
        if (def == null) return builder.buildFuture();

        var vars = plugin.getVariableManager().getGlobalMap();

        var var = vars.get(varId);
        if (var != null) {
            Object value = var.getValue();

            if (value instanceof List<?>) {
                for (Object v : (List<?>) value) builder.suggest(v.toString());
            } else if (value instanceof Map<?, ?>) {
                for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet())
                    builder.suggest(e.getKey() + "," + e.getValue());
            } else if (value != null) {
                builder.suggest(value.toString());
            }
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestValue(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String playerName, varId;
        try {
            playerName = ctx.getArgument("player", String.class);
            varId = ctx.getArgument("id", String.class);
        } catch (IllegalArgumentException ex) {
            return builder.buildFuture();
        }

        VariableDefinition<?> def = plugin.getLoader().getDefinitions()
                .stream().filter(d -> d.id().equalsIgnoreCase(varId)).findFirst().orElse(null);
        if (def == null) return builder.buildFuture();


        return plugin.getVariableManager().getVariables(playerName).thenApply(vars -> {
            Variable<?> var = vars.get(varId);
            if (var == null) var = vars.get(varId.toLowerCase());

            if (var != null) {
                Object value = var.getValue();

                if (value instanceof List<?>) {
                    for (Object v : (List<?>) value) builder.suggest(v.toString());
                } else if (value instanceof Map<?, ?>) {
                    for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet())
                        builder.suggest(e.getKey() + "," + e.getValue());
                } else if (value != null) {
                    builder.suggest(value.toString());
                }
            }
            return builder.build();
        });
    }

    //TODO: 做成GUI
    private static int listPlayer(CommandContext<CommandSourceStack> ctx) {
        String playerName = ctx.getArgument("player", String.class);

        plugin.getVariableManager().getVariables(playerName).thenAccept(variables -> {
            if (variables.isEmpty()) {
                sendMessage(ctx, playerName + "没有变量");
            } else {
                variables.forEach((varId, variable) -> sendMessage(ctx, varId + ": " + variable.valueAsString()));
            }
        });

        return Command.SINGLE_SUCCESS;
    }

    private static int listGlobal(CommandContext<CommandSourceStack> ctx) {
        Map<String, Variable<?>> vars = plugin.getVariableManager().getGlobalMap();

        if (!vars.isEmpty()) {
            vars.forEach((varId, variable) -> sendMessage(ctx, varId + ": " + variable.valueAsString()));
        } else {
            sendMessage(ctx, "目前没有全局变量");
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int getVariableGlobal(CommandContext<CommandSourceStack> ctx) {
        String id = ctx.getArgument("id", String.class);
        Variable<?> var = plugin.getVariableManager().getGlobalVariable(id);
        if (var == null) {
            sendMessage(ctx, "该变量不存在");
        } else {
            sendVar(ctx, var);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int getVariable(CommandContext<CommandSourceStack> ctx) {
        String playerName = ctx.getArgument("player", String.class);
        String id = ctx.getArgument("id", String.class);
        plugin.getVariableManager().getVariable(playerName, id).thenAccept(var-> sendVar(ctx, var));
        return Command.SINGLE_SUCCESS;
    }

    private static void sendVar(CommandContext<CommandSourceStack> ctx, Variable<?> v) {
        sendMessage(ctx, "创建日期: " + dateFormat.format(new Date(v.getCreateTime())));
        if (v.getExpireTime() == -1) {
            sendMessage(ctx, "过期时间: 永不过期");
        } else {
            sendMessage(ctx, "过期时间: " + dateFormat.format(new Date(v.getExpireTime())));
        }
        sendMessage(ctx, v.getValue().toString());
    }

    private static int createVariableNoValueGlobal(CommandContext<CommandSourceStack> ctx) {
        String id = ctx.getArgument("id", String.class);
        var variable = plugin.getLoader().getDefinitions().stream().filter(d -> d.id().equalsIgnoreCase(id)).findFirst().orElse(null);
        if (variable == null) {
            sendMessage(ctx, "变量不存在");
            return Command.SINGLE_SUCCESS;
        }
        if (plugin.getVariableManager().addGlobalVariable(id)) {
            sendMessage(ctx, "变量创建成功: " + id);
        } else {
            sendMessage(ctx, "已有对应全局变量");
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int createVariableNoValue(CommandContext<CommandSourceStack> ctx) {
        String playerName = ctx.getArgument("player", String.class);
        String id = ctx.getArgument("id", String.class);
        var variable = plugin.getLoader().getDefinitions().stream().filter(d -> d.id().equalsIgnoreCase(id)).findFirst().orElse(null);
        if (variable == null) {
            sendMessage(ctx, "变量不存在");
            return Command.SINGLE_SUCCESS;
        }
        plugin.getVariableManager().addVariable(playerName, id).thenAccept(state -> {
            if (state) {
                sendMessage(ctx, "变量创建成功: " + id);
            } else {
                sendMessage(ctx, "玩家已有对应变量");
            }
        });
        return Command.SINGLE_SUCCESS;
    }

    private static int createVariableGlobal(CommandContext<CommandSourceStack> ctx) {
        String id = ctx.getArgument("id", String.class);
        String value = ctx.getArgument("value", String.class);
        var variable = plugin.getLoader().getDefinitions().stream().filter(d -> d.id().equalsIgnoreCase(id)).findFirst().orElse(null);
        if (variable == null) {
            sendMessage(ctx, "变量不存在");
            return Command.SINGLE_SUCCESS;
        }
        Class<?> type = variable.type();
        boolean success = false;
        if (type == String.class) {
            success = plugin.getVariableManager().addGlobalVariable(id, value);
        } else if (type == Long.class) {
            success = plugin.getVariableManager().addGlobalVariable(id, Long.parseLong(value));
        } else if (type == Boolean.class) {
            success = plugin.getVariableManager().addGlobalVariable(id, Boolean.parseBoolean(value));
        } else if (type == Float.class) {
            success = plugin.getVariableManager().addGlobalVariable(id, Float.parseFloat(value));
        } else if (type == List.class) {
            success = plugin.getVariableManager().addGlobalVariable(id, castTo(List.class, value));
        } else if (type == Map.class) {
            success = plugin.getVariableManager().addGlobalVariable(id, castTo(Map.class, value));
        }
        if (success) {
            sendMessage(ctx, "变量创建成功: " + id);
        } else {
            sendMessage(ctx, "已有对应全局变量");
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int createVariable(CommandContext<CommandSourceStack> ctx) {
        String playerName = ctx.getArgument("player", String.class);
        String id = ctx.getArgument("id", String.class);
        String value = ctx.getArgument("value", String.class);
        var variable = plugin.getLoader().getDefinitions().stream().filter(d -> d.id().equalsIgnoreCase(id)).findFirst().orElse(null);
        if (variable == null) {
            sendMessage(ctx, "变量不存在");
            return Command.SINGLE_SUCCESS;
        }
        Class<?> type = variable.type();
        if (type == String.class) {
            plugin.getVariableManager().addVariable(playerName, id, value).thenAccept(state -> sendState(ctx, state, id));
        } else if (type == Long.class) {
            plugin.getVariableManager().addVariable(playerName, id, Long.parseLong(value)).thenAccept(state -> sendState(ctx, state, id));
        } else if (type == Boolean.class) {
            plugin.getVariableManager().addVariable(playerName, id, Boolean.parseBoolean(value)).thenAccept(state -> sendState(ctx, state, id));
        } else if (type == Float.class) {
            plugin.getVariableManager().addVariable(playerName, id, Float.parseFloat(value)).thenAccept(state -> sendState(ctx, state, id));
        } else if (type == List.class) {
            plugin.getVariableManager().addVariable(playerName, id, castTo(List.class, value)).thenAccept(state -> sendState(ctx, state, id));
        } else if (type == Map.class) {
            plugin.getVariableManager().addVariable(playerName, id, castTo(Map.class, value)).thenAccept(state -> sendState(ctx, state, id));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static void sendState(CommandContext<CommandSourceStack> ctx, Boolean state, String id) {
        if (state) {
            sendMessage(ctx, "变量创建成功: " + id);
        } else {
            sendMessage(ctx, "玩家已有对应变量");
        }
    }

    private static int addVariableGlobal(CommandContext<CommandSourceStack> ctx) {
        String id = ctx.getArgument("id", String.class);
        String value = ctx.getArgument("value", String.class);
        boolean autoFix = false;
        try {
            autoFix = ctx.getArgument("autoFix", boolean.class);
        } catch (Exception ignored) {
        }
        var variable = plugin.getVariableManager().getGlobalVariable(id);
        if (variable == null) {
            sendMessage(ctx, "变量不存在");
            return Command.SINGLE_SUCCESS;
        }
        switch (variable) {
            case StringVariable stringVariable -> stringVariable.add(value, autoFix);
            case IntVariable intVariable -> intVariable.add(Long.parseLong(value), autoFix);
            case BooleanVariable booleanVariable -> booleanVariable.add(Boolean.parseBoolean(value), autoFix);
            case FloatVariable floatVariable -> floatVariable.add(Float.parseFloat(value), autoFix);
            case StringList stringList -> stringList.add(castTo(List.class, value), autoFix);
            case NumberList numberList -> numberList.add(castTo(List.class, value), autoFix);
            case StringStringMap stringStringMap -> stringStringMap.add(castTo(Map.class, value), autoFix);
            case StringNumberMap stringNumberMap -> stringNumberMap.add(castTo(Map.class, value), autoFix);
            default -> variable.add(null);
        }
        sendMessage(ctx, "变量增加成功: " + id);
        return Command.SINGLE_SUCCESS;
    }

    private static int setVariableGlobal(CommandContext<CommandSourceStack> ctx) {
        String id = ctx.getArgument("id", String.class);
        String value = ctx.getArgument("value", String.class);
        var variable = plugin.getVariableManager().getGlobalVariable(id);
        if (variable == null) {
            sendMessage(ctx, "变量不存在");
            return Command.SINGLE_SUCCESS;
        }
        switch (variable) {
            case StringVariable stringVariable -> stringVariable.setValue(value);
            case IntVariable intVariable -> intVariable.setValue(Long.parseLong(value));
            case BooleanVariable booleanVariable -> booleanVariable.setValue(Boolean.parseBoolean(value));
            case FloatVariable floatVariable -> floatVariable.setValue(Float.parseFloat(value));
            case StringList stringList -> stringList.setValue(castTo(List.class, value));
            case NumberList numberList -> numberList.setValue(castTo(List.class, value));
            case StringStringMap stringStringMap -> stringStringMap.setValue(castTo(Map.class, value));
            case StringNumberMap stringNumberMap -> stringNumberMap.setValue(castTo(Map.class, value));
            default -> variable.setValue(null);
        }
        sendMessage(ctx, "变量设置成功");
        return Command.SINGLE_SUCCESS;
    }

    private static int takeVariableGlobal(CommandContext<CommandSourceStack> ctx) {
        String id = ctx.getArgument("id", String.class);
        String value = ctx.getArgument("value", String.class);
        boolean autoFix = false;
        try {
            autoFix = ctx.getArgument("autoFix", boolean.class);
        } catch (Exception ignored) {
        }
        var variable = plugin.getVariableManager().getGlobalVariable(id);
        if (variable == null) {
            sendMessage(ctx, "变量不存在");
            return Command.SINGLE_SUCCESS;
        }
        switch (variable) {
            case StringVariable stringVariable -> stringVariable.take(value, autoFix);
            case IntVariable intVariable -> intVariable.take(Long.parseLong(value), autoFix);
            case BooleanVariable booleanVariable -> booleanVariable.take(Boolean.parseBoolean(value), autoFix);
            case FloatVariable floatVariable -> floatVariable.take(Float.parseFloat(value), autoFix);
            case StringList stringList -> stringList.take(castTo(List.class, value), autoFix);
            case NumberList numberList -> numberList.take(castTo(List.class, value), autoFix);
            case StringStringMap stringStringMap -> stringStringMap.take(castTo(Map.class, value), autoFix);
            case StringNumberMap stringNumberMap -> stringNumberMap.take(castTo(Map.class, value), autoFix);
            default -> variable.take(null);
        }
        sendMessage(ctx, "变量减少成功: " + id);
        return Command.SINGLE_SUCCESS;
    }

    private static int removeVariableGlobal(CommandContext<CommandSourceStack> ctx) {
        String id = ctx.getArgument("id", String.class);
        var map = plugin.getVariableManager().getGlobalMap();
        if (map.containsKey(id)) {
            map.remove(id);
            sendMessage(ctx, "已移除全局变量:" + id);
        } else {
            sendMessage(ctx, "不存在ID为" + id + "的全局变量");
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int addVariable(CommandContext<CommandSourceStack> ctx) {
        String playerName = ctx.getArgument("player", String.class);
        String id = ctx.getArgument("id", String.class);
        String value = ctx.getArgument("value", String.class);
        boolean autoFix = false;
        try {
            autoFix = ctx.getArgument("autoFix", boolean.class);
        } catch (Exception ignored) {
        }
        var variable = plugin.getVariableManager().getVariable(playerName, id).join();
        if (variable == null) {
            sendMessage(ctx, "变量不存在");
            return Command.SINGLE_SUCCESS;
        }
        switch (variable) {
            case StringVariable stringVariable -> stringVariable.add(value, autoFix);
            case IntVariable intVariable -> intVariable.add(Long.parseLong(value), autoFix);
            case BooleanVariable booleanVariable -> booleanVariable.add(Boolean.parseBoolean(value), autoFix);
            case FloatVariable floatVariable -> floatVariable.add(Float.parseFloat(value), autoFix);
            case StringList stringList -> stringList.add(castTo(List.class, value), autoFix);
            case NumberList numberList -> numberList.add(castTo(List.class, value), autoFix);
            case StringStringMap stringStringMap -> stringStringMap.add(castTo(Map.class, value), autoFix);
            case StringNumberMap stringNumberMap -> stringNumberMap.add(castTo(Map.class, value), autoFix);
            default -> variable.add(null);
        }
        sendMessage(ctx, "变量增加成功: " + id);
        return Command.SINGLE_SUCCESS;
    }

    private static int setVariable(CommandContext<CommandSourceStack> ctx) {
        String playerName = ctx.getArgument("player", String.class);
        String id = ctx.getArgument("id", String.class);
        String value = ctx.getArgument("value", String.class);
        var variable = plugin.getVariableManager().getVariable(playerName, id).join();
        if (variable == null) {
            sendMessage(ctx, "变量不存在");
            return Command.SINGLE_SUCCESS;
        }
        switch (variable) {
            case StringVariable stringVariable -> stringVariable.setValue(value);
            case IntVariable intVariable -> intVariable.setValue(Long.parseLong(value));
            case BooleanVariable booleanVariable -> booleanVariable.setValue(Boolean.parseBoolean(value));
            case FloatVariable floatVariable -> floatVariable.setValue(Float.parseFloat(value));
            case StringList stringList -> stringList.setValue(castTo(List.class, value));
            case NumberList numberList -> numberList.setValue(castTo(List.class, value));
            case StringStringMap stringStringMap -> stringStringMap.setValue(castTo(Map.class, value));
            case StringNumberMap stringNumberMap -> stringNumberMap.setValue(castTo(Map.class, value));
            default -> variable.setValue(null);
        }
        sendMessage(ctx, "变量设置成功");
        return Command.SINGLE_SUCCESS;
    }

    private static int takeVariable(CommandContext<CommandSourceStack> ctx) {
        String playerName = ctx.getArgument("player", String.class);
        String id = ctx.getArgument("id", String.class);
        String value = ctx.getArgument("value", String.class);
        boolean autoFix = false;
        try {
            autoFix = ctx.getArgument("autoFix", boolean.class);
        } catch (Exception ignored) {
        }
        var variable = plugin.getVariableManager().getVariable(playerName, id).join();
        if (variable == null) {
            sendMessage(ctx, "变量不存在");
            return Command.SINGLE_SUCCESS;
        }
        switch (variable) {
            case StringVariable stringVariable -> stringVariable.take(value, autoFix);
            case IntVariable intVariable -> intVariable.take(Long.parseLong(value), autoFix);
            case BooleanVariable booleanVariable -> booleanVariable.take(Boolean.parseBoolean(value), autoFix);
            case FloatVariable floatVariable -> floatVariable.take(Float.parseFloat(value), autoFix);
            case StringList stringList -> stringList.take(castTo(List.class, value), autoFix);
            case NumberList numberList -> numberList.take(castTo(List.class, value), autoFix);
            case StringStringMap stringStringMap -> stringStringMap.take(castTo(Map.class, value), autoFix);
            case StringNumberMap stringNumberMap -> stringNumberMap.take(castTo(Map.class, value), autoFix);
            default -> variable.take(null);
        }
        sendMessage(ctx, "变量减少成功: " + id);
        return Command.SINGLE_SUCCESS;
    }

    private static int resetGlobalVariable(CommandContext<CommandSourceStack> ctx) {
        String id = ctx.getArgument("id", String.class);
        var var = plugin.getVariableManager().getGlobalVariable(id);
        var.reset();
        sendMessage(ctx, "已将变量 "+ id +" 重置为默认值 "+Variable.valueAsString(var.getVariableDefinition().defaultValue()));
        return Command.SINGLE_SUCCESS;
    }

    private static int resetPlayerVariable(CommandContext<CommandSourceStack> ctx) {
        String playerName = ctx.getArgument("player", String.class);
        String id = ctx.getArgument("id", String.class);
        plugin.getVariableManager().getVariable(playerName, id).thenAccept(var -> {
            var.reset();
            sendMessage(ctx, "已将变量 "+ id +" 重置为默认值 "+Variable.valueAsString(var.getVariableDefinition().defaultValue()));
        });
        return Command.SINGLE_SUCCESS;
    }

    private static int removeVariable(CommandContext<CommandSourceStack> ctx) {
        String playerName = ctx.getArgument("player", String.class);
        String id = ctx.getArgument("id", String.class);
        plugin.getVariableManager().removeVariable(playerName, id).thenAccept(state -> {
            if (state) {
                sendMessage(ctx, "变量移除成功: " + id);
            } else {
                sendMessage(ctx, "变量移除失败，玩家无法加载或玩家没有该变量");
            }
        });
        return Command.SINGLE_SUCCESS;
    }

    private static void sendMessage(CommandContext<CommandSourceStack> ctx, String message) {
        ctx.getSource().getSender().sendRichMessage(message);
    }

    @SuppressWarnings("unchecked")
    private static <T> T castTo(Class<T> clazz, String value) {
        if (clazz == List.class) {
            boolean isNumber = true;
            if (value.startsWith("[") && value.endsWith("]")) {
                value = value.substring(1, value.length() - 1);
            }
            List<Object> list = new ArrayList<>();
            if (!value.isEmpty()) {
                String[] parts = value.split(",");
                try {
                    Double.parseDouble(parts[0]);
                } catch (NumberFormatException e) {
                    isNumber = false;
                }
                for (String part : parts) {
                    if (isNumber) {
                        list.add(Double.parseDouble(part));
                    } else {
                        list.add(part.trim());
                    }
                }
            }
            return (T) list;
        } else if (clazz == Map.class) {
            boolean isNumber = true;
            if (value.startsWith("{") && value.endsWith("}")) {
                value = value.substring(1, value.length() - 1);
            }
            Map<String, Object> map = new HashMap<>();
            if (!value.isEmpty()) {
                String[] entries = value.split(",");
                if (entries.length > 0) {
                    String[] kv = entries[0].split("=", 2);
                    try {
                        Double.parseDouble(kv[1]);
                    } catch (NumberFormatException e) {
                        isNumber = false;
                    }
                }
                for (String entry : entries) {
                    String[] kv = entry.split("=", 2);
                    if (kv.length == 2) {
                        if (isNumber) {
                            map.put(kv[0].trim(), Double.parseDouble(kv[1]));
                        } else {
                            map.put(kv[0].trim(), kv[1].trim());
                        }
                    }
                }
            }
            return (T) map;
        }
        return null;
    }
}