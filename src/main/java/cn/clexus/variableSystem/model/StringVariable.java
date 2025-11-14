package cn.clexus.variableSystem.model;

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
        var newStr = this.value.replaceFirst(value, "");
        if(newStr.length() < variableDefinition.minValue()) {
            if(autoFix && this.value.length() >= variableDefinition.minValue()) this.value = this.value.substring(variableDefinition.minValue().intValue());
            return ValidateResult.BELOW_MIN;
        }
        return ValidateResult.PASS;
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
