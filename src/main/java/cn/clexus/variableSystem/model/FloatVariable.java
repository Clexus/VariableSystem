package cn.clexus.variableSystem.model;

public class FloatVariable extends Variable<Float> {


    public FloatVariable(VariableDefinition<Float> variableDefinition, Float value, long createTime) {
        super(variableDefinition, value, createTime);
    }

    @Override
    public ValidateResult validate(Float value) {
        if (value > variableDefinition.maxValue()) {
            return ValidateResult.ABOVE_MAX;
        } else if (value < variableDefinition.minValue()) {
            return ValidateResult.BELOW_MIN;
        } else {
            return ValidateResult.PASS;
        }
    }

    @Override
    public ValidateResult add(Float value, boolean autoFix) {
        var newValue = this.value + value;
        var result = validate(newValue);
        if (result == ValidateResult.PASS) {
            this.value = newValue;
        } else if (autoFix) {
            this.value = newValue;
            trim();
        }
        return result;
    }

    public ValidateResult add(Number value) {
        return add(value.floatValue());
    }

    @Override
    public ValidateResult take(Float value, boolean autoFix) {
        var newValue = this.value - value;
        var result = validate(newValue);
        if (result == ValidateResult.PASS) {
            this.value = newValue;
        } else if (autoFix) {
            this.value = newValue;
            trim();
        }
        return result;
    }

    public ValidateResult take(Number value) {
        return take(value.floatValue());
    }

    private void trim() {
        if (value > variableDefinition.maxValue()) {
            value = variableDefinition.maxValue().floatValue();
        } else if (value < variableDefinition.minValue()) {
            value = variableDefinition.minValue().floatValue();
        }
    }
}
