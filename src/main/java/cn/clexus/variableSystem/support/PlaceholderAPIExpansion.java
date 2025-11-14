package cn.clexus.variableSystem.support;

import cn.clexus.variableSystem.VariableSystem;
import cn.clexus.variableSystem.model.Variable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;


public class PlaceholderAPIExpansion extends PlaceholderExpansion {
    static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    @Override
    @NotNull
    public String getAuthor() {
        return "Clexus";
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "var";
    }

    @Override
    @NotNull
    public String getVersion() {
        return VariableSystem.instance.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        String result = null;
        int first = params.indexOf('_');
        int last = params.lastIndexOf('_');

        if(params.startsWith("sum")) {
            if(first != -1){
                var varId = params.substring(first+1);
                return BigDecimal.valueOf(VariableSystem.instance.getVariableManager().sumAll(varId).join()).stripTrailingZeros().toPlainString();
            }else{
                return "invalidFormat";
            }
        }
        if (first == -1 || last == -1 || first == last) {
            return "invalidFormat";
        }
        String mode = params.substring(0, first);
        String varId = params.substring(first + 1, last);
        String op = params.substring(last + 1);
        String identifier = VariableSystem.instance.isUUIDMode() ? player.getUniqueId().toString() : player.getName();
        Variable<?> variable = null;
        switch (mode.toLowerCase()) {
            case "player" -> variable = VariableSystem.instance.getVariableManager().getVariable(identifier, varId).join();
            case "global" -> variable = VariableSystem.instance.getVariableManager().getGlobalVariable(varId);
        }
        if (variable == null) return "nullVariable";
        switch (op.toLowerCase()) {
            case "value", "v" -> result = variable.valueAsString();
            case "createtime", "ct" -> result = String.valueOf(variable.getCreateTime());
            case "createdate", "cd" -> result = dateFormat.format(new Date(variable.getCreateTime()));
            case "expiretime", "et" -> result = String.valueOf(variable.getExpireTime());
            case "expiredate", "ed" -> result = variable.getExpireTime() == -1 ? "-1" : dateFormat.format(new Date(variable.getExpireTime()));
            case "defaultvalue", "default", "dv", "d" -> result = Variable.valueAsString(variable.getVariableDefinition().defaultValue());
            case "minvalue", "min" -> result = String.valueOf(variable.getVariableDefinition().minValue());
            case "maxvalue", "max" -> result = String.valueOf(variable.getVariableDefinition().maxValue());
        }
        return result;
    }
}