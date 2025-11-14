package cn.clexus.variableSystem.model;

public class IntVariable extends Variable<Long> {

    public IntVariable(VariableDefinition<Long> variableDefinition, Long value,  long createTime) {
        super(variableDefinition, value, createTime);
    }

    @Override
    public ValidateResult validate(Long value) {
        if (value > variableDefinition.maxValue()) {
            return ValidateResult.ABOVE_MAX;
        } else if (value < variableDefinition.minValue()) {
            return ValidateResult.BELOW_MIN;
        } else {
            return ValidateResult.PASS;
        }
    }

    @Override
    public ValidateResult add(Long value, boolean autoFix) {
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
        return add(value.longValue());
    }

    @Override
    public ValidateResult take(Long value, boolean autoFix) {
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
        return take(value.longValue());
    }

    private void trim() {
        if (value > variableDefinition.maxValue()) {
            value = variableDefinition.maxValue().longValue();
        } else if (value < variableDefinition.minValue()) {
            value = variableDefinition.minValue().longValue();
        }
    }
}
