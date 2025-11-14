package cn.clexus.variableSystem.model;

import java.util.Map;

public class StringStringMap extends Variable<Map<String, String>> {
    public StringStringMap(VariableDefinition<Map<String, String>> variableDefinition, Map<String, String> value, long createTime) {
        super(variableDefinition, value, createTime);
    }

    @Override
    public ValidateResult validate(Map<String, String> value) {
        if (value.size() > variableDefinition.maxValue().intValue()) {
            return ValidateResult.ABOVE_MAX;
        } else if (value.size() < variableDefinition.minValue().intValue()) {
            return ValidateResult.BELOW_MIN;
        } else {
            return ValidateResult.PASS;
        }
    }

    @Override
    public ValidateResult add(Map<String, String> value, boolean autoFix) {
        this.value.putAll(value);
        return ValidateResult.PASS;
    }

    public ValidateResult add(String key, String value) {
        this.value.put(key, value);
        return ValidateResult.PASS;
    }

    @Override
    public ValidateResult take(Map<String, String> value, boolean autoFix) {
        for (Map.Entry<String, String> entry : value.entrySet()) {
            this.value.remove(entry.getKey(), entry.getValue());
        }
        return ValidateResult.PASS;
    }

    public ValidateResult take(String key) {
        this.value.remove(key);
        return ValidateResult.PASS;
    }

    public ValidateResult take(String key, String value) {
        this.value.remove(key, value);
        return ValidateResult.PASS;
    }
}
