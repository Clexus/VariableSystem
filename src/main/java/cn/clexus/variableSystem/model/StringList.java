package cn.clexus.variableSystem.model;

import java.util.List;

public class StringList extends Variable<List<String>> {
    public StringList(VariableDefinition<List<String>> variableDefinition, List<String> value, long createTime) {
        super(variableDefinition, value, createTime);
    }

    @Override
    public ValidateResult validate(List<String> value) {
        if(value.size() > variableDefinition.maxValue().intValue()){
            return ValidateResult.ABOVE_MAX;
        } else if(value.size() < variableDefinition.minValue().intValue()){
            return ValidateResult.BELOW_MIN;
        } else {
            return ValidateResult.PASS;
        }
    }

    @Override
    public ValidateResult add(List<String> value, boolean autoFix) {
        int max = variableDefinition.maxValue().intValue();
        int newSize = this.value.size() + value.size();

        if (newSize > max) {
            if (autoFix) {
                int remaining = max - this.value.size();
                if (remaining > 0) {
                    this.value.addAll(value.subList(0, remaining));
                }
            }
            return ValidateResult.ABOVE_MAX;
        }

        this.value.addAll(value);
        return ValidateResult.PASS;
    }


    public ValidateResult add(String value) {
        return add(List.of(value));
    }

    @Override
    public ValidateResult take(List<String> value, boolean autoFix) {
        boolean contains = value.stream().anyMatch(this.value::contains);
        if (!contains) {
            return ValidateResult.NOOP;
        }

        int min = variableDefinition.minValue().intValue();
        int newSize = this.value.size() - value.size();

        if (newSize < min) {
            if (autoFix) {
                int canRemove = this.value.size() - min;
                this.value.removeAll(value.subList(0, Math.min(canRemove, value.size())));
            }
            return ValidateResult.BELOW_MIN;
        }

        this.value.removeAll(value);
        return ValidateResult.PASS;
    }


    public ValidateResult take(String value) {
        if (!this.value.contains(value)) {
            return ValidateResult.NOOP;
        }
        if (this.value.size() - 1 < variableDefinition.minValue().intValue()) {
            return ValidateResult.BELOW_MIN;
        }
        this.value.remove(value);
        return ValidateResult.PASS;
    }

    public ValidateResult takeAt(int index) {
        if (index < 0 || index >= value.size()) {
            return ValidateResult.NOOP;
        }
        if (this.value.size() - 1 < variableDefinition.minValue().intValue()) {
            return ValidateResult.BELOW_MIN;
        }
        this.value.remove(index);
        return ValidateResult.PASS;
    }
}
