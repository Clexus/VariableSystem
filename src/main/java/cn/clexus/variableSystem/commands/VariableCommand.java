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

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.string;

@SuppressWarnings({"SameReturnValue", "unchecked", "DataFlowIssue"})
public class VariableCommand {

    static VariableSystem plugin = VariableSystem.instance;
    static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    public static LiteralCommandNode<CommandSourceStack> register() {
        return Commands.literal("var")
                .requires(ctx -> ctx.getSender().hasPermission("variablesystem.admin"))
                .then(Commands.literal("player")
                        .then(Commands.argument("player", string())
                                .suggests((ctx, builder) -> {
                                        Bukkit.getOnlinePlayers().forEach(player -> builder.suggest(player.getName()));
                                    return builder.buildFuture();
                                })
                                .then(Commands.literal("list").executes(VariableCommand::listPlayer))
                                .then(Commands.literal("get")
                                        .then(Commands.argument("id", string())
                                                .suggests(VariableCommand::suggestPlayerVariableId)
                                                .executes(VariableCommand::getVariable)
                                                .then(Commands.literal("at")
                                                        .then(Commands.argument("index", integer())
                                                                .suggests(VariableCommand::suggestIndexPlayer)
                                                                .executes(VariableCommand::getVariable)
                                                        )
                                                )
                                                .then(Commands.literal("from")
                                                        .then(Commands.argument("key", string())
                                                                .suggests(VariableCommand::suggestKeyPlayer)
                                                                .executes(VariableCommand::getVariable)
                                                        )
                                                )
                                        )
                                )
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
                                .then(Commands.literal("setAt")
                                        .then(Commands.argument("id", string())
                                                .suggests(VariableCommand::suggestPlayerListVariableId)
                                                .then(Commands.argument("index", integer())
                                                        .suggests(VariableCommand::suggestIndexPlayer)
                                                        .then(Commands.argument("value", string())
                                                                .suggests(VariableCommand::suggestValueAtIndexPlayer)
                                                                .executes(VariableCommand::setVariableAtPlayer)
                                                        )
                                                )
                                        )
                                )
                                .then(Commands.literal("setFrom")
                                        .then(Commands.argument("id", string())
                                                .suggests(VariableCommand::suggestPlayerMapVariableId)
                                                .then(Commands.argument("key", string())
                                                        .suggests(VariableCommand::suggestKeyPlayer)
                                                        .then(Commands.argument("value", string())
                                                                .suggests(VariableCommand::suggestValueAtKeyPlayer)
                                                                .executes(VariableCommand::setVariableFromPlayer)
                                                        )
                                                )
                                        )
                                )
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
                                .then(Commands.literal("takeAt")
                                        .then(Commands.argument("id", string())
                                                .suggests(VariableCommand::suggestPlayerListVariableId)
                                                .then(Commands.argument("index", integer())
                                                        .suggests(VariableCommand::suggestIndexPlayer)
                                                        .executes(VariableCommand::takeVariableAtPlayer)
                                                )
                                        )
                                )
                                .then(Commands.literal("takeFrom")
                                        .then(Commands.argument("id", string())
                                                .suggests(VariableCommand::suggestPlayerMapVariableId)
                                                .then(Commands.argument("key", string())
                                                        .suggests(VariableCommand::suggestKeyPlayer)
                                                        .executes(VariableCommand::takeVariableFromPlayer)
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
                                        .then(Commands.literal("at")
                                                .then(Commands.argument("index", integer())
                                                        .suggests(VariableCommand::suggestIndexGlobal)
                                                        .executes(VariableCommand::getVariableGlobal)
                                                )
                                        )
                                        .then(Commands.literal("from")
                                                .then(Commands.argument("key", string())
                                                        .suggests(VariableCommand::suggestKeyGlobal)
                                                        .executes(VariableCommand::getVariableGlobal)
                                                )
                                        )
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
                        .then(Commands.literal("setAt")
                                .then(Commands.argument("id", string())
                                        .suggests(VariableCommand::suggestGlobalListVariableId)
                                        .then(Commands.argument("index", integer())
                                                .suggests(VariableCommand::suggestIndexGlobal)
                                                .then(Commands.argument("value", string())
                                                        .suggests(VariableCommand::suggestValueAtIndexGlobal)
                                                        .executes(VariableCommand::setVariableAtGlobal)
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("setFrom")
                                .then(Commands.argument("id", string())
                                        .suggests(VariableCommand::suggestGlobalMapVariableId)
                                        .then(Commands.argument("key", string())
                                                .suggests(VariableCommand::suggestKeyGlobal)
                                                .then(Commands.argument("value", string())
                                                        .suggests(VariableCommand::suggestValueAtKeyGlobal)
                                                        .executes(VariableCommand::setVariableFromGlobal)
                                                )
                                        )
                                )
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
                        .then(Commands.literal("takeAt")
                                .then(Commands.argument("id", string())
                                        .suggests(VariableCommand::suggestGlobalListVariableId)
                                        .then(Commands.argument("index", integer())
                                                .suggests(VariableCommand::suggestIndexGlobal)
                                                .executes(VariableCommand::takeVariableAtGlobal)
                                        )
                                )
                        )
                        .then(Commands.literal("takeFrom")
                                .then(Commands.argument("id", string())
                                        .suggests(VariableCommand::suggestGlobalMapVariableId)
                                        .then(Commands.argument("key", string())
                                                .suggests(VariableCommand::suggestKeyGlobal)
                                                .executes(VariableCommand::takeVariableFromGlobal)
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

    private static CompletableFuture<Suggestions> suggestIndexPlayer(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String pName, varId;
        pName = ctx.getArgument("player", String.class);
        varId = ctx.getArgument("id", String.class);
        return plugin.getVariableManager().getVariable(pName, varId).thenApply(result -> {
            if (result == null || result.getValue() instanceof Map) return builder.build();
            List<?> value = (List<?>) result.getValue();
            for (int i = 0; i < value.size(); i++) {
                builder.suggest(i);
            }
            return builder.build();
        });
    }

    private static CompletableFuture<Suggestions> suggestValueAtIndexPlayer(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String pName, varId;
        pName = ctx.getArgument("player", String.class);
        varId = ctx.getArgument("id", String.class);
        int index = ctx.getArgument("index", int.class);
        return plugin.getVariableManager().getVariable(pName, varId).thenApply(result -> {
            if (result == null || !(result.getValue() instanceof List<?> value)) return builder.build();
            if (index >= value.size() || index < 0) return builder.build();
            builder.suggest(value.get(index).toString());
            return builder.build();
        });
    }

    private static CompletableFuture<Suggestions> suggestValueAtKeyPlayer(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String pName, varId, key;
        pName = ctx.getArgument("player", String.class);
        varId = ctx.getArgument("id", String.class);
        key = ctx.getArgument("key", String.class);
        return plugin.getVariableManager().getVariable(pName, varId).thenApply(result -> {
            if (result == null || !(result.getValue() instanceof Map<?, ?> map)) return builder.build();
            if (!map.containsKey(key)) return builder.build();
            builder.suggest(map.get(key).toString());
            return builder.build();
        });
    }

    private static CompletableFuture<Suggestions> suggestValueAtIndexGlobal(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String varId = ctx.getArgument("id", String.class);
        int index = ctx.getArgument("index", int.class);
        var result = plugin.getVariableManager().getGlobalVariable(varId);
        if (result == null || !(result.getValue() instanceof List<?> value)) return builder.buildFuture();
        if (index >= value.size() || index < 0) return builder.buildFuture();
        builder.suggest(value.get(index).toString());
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestValueAtKeyGlobal(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String varId, key;
        varId = ctx.getArgument("id", String.class);
        key = ctx.getArgument("key", String.class);
        var result = plugin.getVariableManager().getGlobalVariable(varId);
        if (result == null || !(result.getValue() instanceof Map<?, ?> map)) return builder.buildFuture();
        if (!map.containsKey(key)) return builder.buildFuture();
        builder.suggest(map.get(key).toString());
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestKeyPlayer(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String pName, varId;
        pName = ctx.getArgument("player", String.class);
        varId = ctx.getArgument("id", String.class);
        return plugin.getVariableManager().getVariable(pName, varId).thenApply(result -> {
            if (result == null || result instanceof List<?>) return builder.build();
            Map<?, ?> value = (Map<?, ?>) result.getValue();
            value.keySet().forEach(key -> builder.suggest((String) key));
            return builder.build();
        });
    }

    private static CompletableFuture<Suggestions> suggestIndexGlobal(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String varId;
        varId = ctx.getArgument("id", String.class);
        var result = plugin.getVariableManager().getGlobalVariable(varId);
        if (result == null || result.getValue() instanceof Map) return builder.buildFuture();
        List<?> value = (List<?>) result.getValue();
        for (int i = 0; i < value.size(); i++) {
            builder.suggest(i);
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestKeyGlobal(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String varId;
        varId = ctx.getArgument("id", String.class);
        var result = plugin.getVariableManager().getGlobalVariable(varId);
        if (result == null || result instanceof List<?>) return builder.buildFuture();
        Map<?, ?> value = (Map<?, ?>) result.getValue();
        value.keySet().forEach(key -> builder.suggest((String) key));
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestGlobal(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        for (var variable : plugin.getVariableManager().getGlobalMap().keySet()) {
            builder.suggest(variable);
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestPlayerListVariableId(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
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
                            if (variable.getVariableDefinition().type() == List.class) {
                                builder.suggest(variable.getVariableDefinition().id());
                            }
                        }
                    }
                    return builder.build();
                });
    }

    private static CompletableFuture<Suggestions> suggestPlayerMapVariableId(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
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
                            if (variable.getVariableDefinition().type() == Map.class) {
                                builder.suggest(variable.getVariableDefinition().id());
                            }
                        }
                    }
                    return builder.build();
                });
    }

    private static CompletableFuture<Suggestions> suggestGlobalListVariableId(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        plugin.getVariableManager().getGlobalMap()
                .values().forEach(variable -> {
                    if (variable.getVariableDefinition().type() == List.class) {
                        builder.suggest(variable.getVariableDefinition().id());
                    }
                });
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestGlobalMapVariableId(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        plugin.getVariableManager().getGlobalMap()
                .values().forEach(variable -> {
                    if (variable.getVariableDefinition().type() == Map.class) {
                        builder.suggest(variable.getVariableDefinition().id());
                    }
                });
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
            builder.suggest("\"" + var.valueAsString() + "\"");
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
                builder.suggest("\"" + var.valueAsString() + "\"");
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
        int index = -1;
        String key = null;
        try {
            index = ctx.getArgument("index", Integer.class);
        } catch (IllegalArgumentException ignored) {
        }
        try {
            key = ctx.getArgument("key", String.class);
        } catch (IllegalArgumentException ignored) {
        }
        Variable<?> var = plugin.getVariableManager().getGlobalVariable(id);
        if (var == null) {
            sendMessage(ctx, "该变量不存在");
        } else {
            if (index == -1 && key == null) {
                sendVar(ctx, var);
            } else if (index != -1) {
                Object value = var.getValue();
                if (value instanceof List<?> list) {
                    sendMessage(ctx, String.valueOf(list.get(index)));
                } else {
                    sendMessage(ctx, "该变量不是列表类型");
                }
            } else {
                Object value = var.getValue();
                if (value instanceof Map<?, ?> map) {
                    sendMessage(ctx, String.valueOf(map.get(key)));
                } else {
                    sendMessage(ctx, "该变量不是表类型");
                }
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int getVariable(CommandContext<CommandSourceStack> ctx) {
        String playerName = ctx.getArgument("player", String.class);
        String id = ctx.getArgument("id", String.class);
        int index = -1;
        String key = null;
        try {
            index = ctx.getArgument("index", Integer.class);
        } catch (IllegalArgumentException ignored) {
        }
        try {
            key = ctx.getArgument("key", String.class);
        } catch (IllegalArgumentException ignored) {
        }
        if (index == -1 && key == null) {
            plugin.getVariableManager().getVariable(playerName, id).thenAccept(var -> sendVar(ctx, var));
        } else if (index != -1) {
            int finalIndex = index;
            plugin.getVariableManager().getVariable(playerName, id).thenAccept(var -> {
                Object value = var.getValue();
                if (value instanceof List<?> list) {
                    sendMessage(ctx, String.valueOf(list.get(finalIndex)));
                } else {
                    sendMessage(ctx, "该变量不是列表类型");
                }
            });
        } else {
            String finalKey = key;
            plugin.getVariableManager().getVariable(playerName, id).thenAccept(var -> {
                Object value = var.getValue();
                if (value instanceof Map<?, ?> map) {
                    sendMessage(ctx, String.valueOf(map.get(finalKey)));
                } else {
                    sendMessage(ctx, "该变量不是表类型");
                }
            });
        }
        return Command.SINGLE_SUCCESS;
    }

    private static void sendVar(CommandContext<CommandSourceStack> ctx, Variable<?> v) {
        sendMessage(ctx, "创建日期: " + dateFormat.format(new Date(v.getCreateTime())));
        if (v.getExpireTime() == -1) {
            sendMessage(ctx, "过期时间: 永不过期");
        } else {
            sendMessage(ctx, "过期时间: " + dateFormat.format(new Date(v.getExpireTime())));
        }
        sendMessage(ctx, "变量值: " + v.valueAsString());
        if (v.getValue() instanceof List<?> list) {
            sendMessage(ctx, "变量长度: " + list.size());
        } else if (v.getValue() instanceof Map<?, ?> map) {
            sendMessage(ctx, "变量长度: " + map.size());
        }
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
        return addVar(ctx, id, value, autoFix, variable);
    }

    private static int addVar(CommandContext<CommandSourceStack> ctx, String id, String value, boolean autoFix, Variable<?> variable) {
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

    private static int setVariableAtPlayer(CommandContext<CommandSourceStack> ctx) {
        String id = ctx.getArgument("id", String.class);
        String player = ctx.getArgument("player", String.class);
        int index = ctx.getArgument("index", Integer.class);
        String value = ctx.getArgument("value", String.class);
        var variable = plugin.getVariableManager().getVariable(player, id).join();
        return setAt(ctx, index, value, variable);
    }

    private static int setAt(CommandContext<CommandSourceStack> ctx, int index, String value, Variable<?> variable) {
        switch (variable) {
            case null -> {
                sendMessage(ctx, "变量不存在");
                return Command.SINGLE_SUCCESS;
            }
            case NumberList list -> {
                List<Double> origin = list.getValue();
                if (index < 0 || origin.size() <= index) {
                    sendMessage(ctx, "索引超出列表范围，有效值为0-" + (origin.size() - 1));
                    return Command.SINGLE_SUCCESS;
                }
                try {
                    double d = Double.parseDouble(value);
                    origin.set(index, d);
                    sendMessage(ctx, "变量设置完成");
                } catch (Exception e) {
                    sendMessage(ctx, value + "不是有效的数字");
                    return Command.SINGLE_SUCCESS;
                }
            }
            case StringList stringList -> {
                List<String> origin = stringList.getValue();
                if (index < 0 || origin.size() <= index) {
                    sendMessage(ctx, "索引超出列表范围，有效值为0-" + (origin.size() - 1));
                    return Command.SINGLE_SUCCESS;
                }
                origin.set(index, value);
                sendMessage(ctx, "变量设置完成");
            }
            default -> sendMessage(ctx, "该变量不是列表类型");
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int takeVariableAtPlayer(CommandContext<CommandSourceStack> ctx) {
        String id = ctx.getArgument("id", String.class);
        String player = ctx.getArgument("player", String.class);
        int index = ctx.getArgument("index", Integer.class);
        var variable = plugin.getVariableManager().getVariable(player, id).join();
        return takeAt(ctx, index, variable);
    }

    private static int takeAt(CommandContext<CommandSourceStack> ctx, int index, Variable<?> variable) {
        switch (variable) {
            case null -> {
                sendMessage(ctx, "变量不存在");
                return Command.SINGLE_SUCCESS;
            }
            case NumberList list -> {
                List<Double> origin = list.getValue();
                if (index < 0 || origin.size() <= index) {
                    sendMessage(ctx, "索引超出列表范围，有效值为0-" + (origin.size() - 1));
                    return Command.SINGLE_SUCCESS;
                }
                origin.remove(index);
                sendMessage(ctx, "变量减少成功");
            }
            case StringList list -> {
                List<String> origin = list.getValue();
                if (index < 0 || origin.size() <= index) {
                    sendMessage(ctx, "索引超出列表范围，有效值为0-" + (origin.size() - 1));
                    return Command.SINGLE_SUCCESS;
                }
                origin.remove(index);
                sendMessage(ctx, "变量减少成功");
            }
            default -> sendMessage(ctx, "该变量不是列表类型");
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int setVariableFromPlayer(CommandContext<CommandSourceStack> ctx) {
        String id = ctx.getArgument("id", String.class);
        String player = ctx.getArgument("player", String.class);
        String key = ctx.getArgument("key", String.class);
        String value = ctx.getArgument("value", String.class);
        var variable = plugin.getVariableManager().getVariable(player, id).join();
        return setFrom(ctx, key, value, variable);
    }

    private static int setFrom(CommandContext<CommandSourceStack> ctx, String key, String value, Variable<?> variable) {
        switch (variable) {
            case null -> {
                sendMessage(ctx, "变量不存在");
                return Command.SINGLE_SUCCESS;
            }
            case StringNumberMap map -> {
                Map<String, Double> origin = map.getValue();
                if (!origin.containsKey(key)) {
                    sendMessage(ctx, "表中没有对应键");
                    return Command.SINGLE_SUCCESS;
                }
                try {
                    double d = Double.parseDouble(value);
                    origin.put(key, d);
                    sendMessage(ctx, "变量设置完成");
                } catch (Exception e) {
                    sendMessage(ctx, value + "不是有效的数字");
                }
            }
            case StringStringMap map -> {
                Map<String, String> origin = map.getValue();
                if (!origin.containsKey(key)) {
                    sendMessage(ctx, "表中没有对应键");
                    return Command.SINGLE_SUCCESS;
                }
                origin.put(key, value);
                sendMessage(ctx, "变量设置完成");
            }
            default -> sendMessage(ctx, "该变量不是列表类型");
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int takeVariableFromPlayer(CommandContext<CommandSourceStack> ctx) {
        String id = ctx.getArgument("id", String.class);
        String player = ctx.getArgument("player", String.class);
        String key = ctx.getArgument("key", String.class);
        var variable = plugin.getVariableManager().getVariable(player, id).join();
        return takeFrom(ctx, key, variable);
    }

    private static int takeFrom(CommandContext<CommandSourceStack> ctx, String key, Variable<?> variable) {
        switch (variable) {
            case null -> {
                sendMessage(ctx, "变量不存在");
                return Command.SINGLE_SUCCESS;
            }
            case StringNumberMap map -> {
                Map<String, Double> origin = map.getValue();
                if (!origin.containsKey(key)) {
                    sendMessage(ctx, "表中没有对应键");
                    return Command.SINGLE_SUCCESS;
                }
                origin.remove(key);
                sendMessage(ctx, "变量减少成功");
            }
            case StringStringMap map -> {
                Map<String, String> origin = map.getValue();
                if (!origin.containsKey(key)) {
                    sendMessage(ctx, "表中没有对应键");
                    return Command.SINGLE_SUCCESS;
                }
                origin.remove(key);
                sendMessage(ctx, "变量减少成功");
            }
            default -> sendMessage(ctx, "该变量不是列表类型");
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int setVariableAtGlobal(CommandContext<CommandSourceStack> ctx) {
        String id = ctx.getArgument("id", String.class);
        int index = ctx.getArgument("index", Integer.class);
        String value = ctx.getArgument("value", String.class);
        var variable = plugin.getVariableManager().getGlobalVariable(id);
        return setAt(ctx, index, value, variable);
    }

    private static int takeVariableAtGlobal(CommandContext<CommandSourceStack> ctx) {
        String id = ctx.getArgument("id", String.class);
        int index = ctx.getArgument("index", Integer.class);
        var variable = plugin.getVariableManager().getGlobalVariable(id);
        return takeAt(ctx, index, variable);
    }

    private static int setVariableFromGlobal(CommandContext<CommandSourceStack> ctx) {
        String id = ctx.getArgument("id", String.class);
        String key = ctx.getArgument("key", String.class);
        String value = ctx.getArgument("value", String.class);
        var variable = plugin.getVariableManager().getGlobalVariable(id);
        return setFrom(ctx, key, value, variable);
    }

    private static int takeVariableFromGlobal(CommandContext<CommandSourceStack> ctx) {
        String id = ctx.getArgument("id", String.class);
        String key = ctx.getArgument("key", String.class);
        var variable = plugin.getVariableManager().getGlobalVariable(id);
        return takeFrom(ctx, key, variable);
    }

    private static int setVariableGlobal(CommandContext<CommandSourceStack> ctx) {
        String id = ctx.getArgument("id", String.class);
        String value = ctx.getArgument("value", String.class);
        var variable = plugin.getVariableManager().getGlobalVariable(id);
        if (variable == null) {
            sendMessage(ctx, "变量不存在");
            return Command.SINGLE_SUCCESS;
        }
        return setVar(ctx, value, variable);
    }

    private static int setVar(CommandContext<CommandSourceStack> ctx, String value, Variable<?> variable) {
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
        return takeVar(ctx, id, value, autoFix, variable);
    }

    private static int takeVar(CommandContext<CommandSourceStack> ctx, String id, String value, boolean autoFix, Variable<?> variable) {
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
        return addVar(ctx, id, value, autoFix, variable);
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
        return setVar(ctx, value, variable);
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
        return takeVar(ctx, id, value, autoFix, variable);
    }

    private static int resetGlobalVariable(CommandContext<CommandSourceStack> ctx) {
        String id = ctx.getArgument("id", String.class);
        var var = plugin.getVariableManager().getGlobalVariable(id);
        var.reset();
        sendMessage(ctx, "已将变量 " + id + " 重置为默认值 " + Variable.valueAsString(var.getVariableDefinition().defaultValue()));
        return Command.SINGLE_SUCCESS;
    }

    private static int resetPlayerVariable(CommandContext<CommandSourceStack> ctx) {
        String playerName = ctx.getArgument("player", String.class);
        String id = ctx.getArgument("id", String.class);
        plugin.getVariableManager().getVariable(playerName, id).thenAccept(var -> {
            var.reset();
            sendMessage(ctx, "已将变量 " + id + " 重置为默认值 " + Variable.valueAsString(var.getVariableDefinition().defaultValue()));
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