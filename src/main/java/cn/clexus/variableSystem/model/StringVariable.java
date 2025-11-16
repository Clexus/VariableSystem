package cn.clexus.variableSystem.model;

import java.util.regex.Pattern;

public class StringVariable extends Variable<String> {
    public StringVariable(VariableDefinition<String> variableDefinition, String value, long createTime) {
        super(variableDefinition, value, createTime);
    }

    @Override
    public ValidateResult validate(String value) {
        if (value.length() > variableDefinition.maxValue()) {
            return ValidateResult.ABOVE_MAX;
        } else if (value.length() < variableDefinition.minValue()) {
            return ValidateResult.BELOW_MIN;
        }
        return ValidateResult.PASS;
    }

    @Override
    public ValidateResult add(String value, boolean autoFix) {
        this.value = this.value + value;
        if (this.value.length() > variableDefinition.maxValue()) {
            if(autoFix) this.value = this.value.substring(0, variableDefinition.maxValue().intValue());
            return ValidateResult.ABOVE_MAX;
        }else{
            return ValidateResult.PASS;
        }
    }

    @Override
    public ValidateResult take(String value, boolean autoFix) {
        int min = variableDefinition.minValue().intValue();
        String original = this.value;
        int idx = original.indexOf(value);
        if (idx == -1) {
            return ValidateResult.NOOP;
        }

        if (!autoFix) {
            String simulated = original.substring(0, idx) + original.substring(idx + value.length());
            if (simulated.length() < min) {
                return ValidateResult.BELOW_MIN;
            }
            this.value = simulated;
            return ValidateResult.PASS;
        }

        StringBuilder sb = new StringBuilder(original);

        boolean deletedAny = false;
        for (int i = 0; i < value.length(); i++) {
            if (sb.length() <= min) break;
            sb.deleteCharAt(idx);
            deletedAny = true;
        }

        if (!deletedAny) {
            return ValidateResult.NOOP;
        }

        this.value = sb.toString();

        return this.value.length() >= min ? ValidateResult.PASS : ValidateResult.BELOW_MIN;
    }


    public ValidateResult takeAll(String value, boolean autoFix) {
        var newStr = this.value.replace(value, "");
        if(newStr.length() < variableDefinition.minValue()) {
            if(autoFix && this.value.length() >= variableDefinition.minValue()) this.value = this.value.substring(variableDefinition.minValue().intValue());
            return ValidateResult.BELOW_MIN;
        }
        return ValidateResult.PASS;
    }
}
