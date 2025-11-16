package cn.clexus.variableSystem.model;

import java.util.ArrayList;
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
    public ValidateResult take(List<String> input, boolean autoFix) {
        int min = variableDefinition.minValue().intValue();
        List<String> original = this.value;

        boolean anyMatch = input.stream().anyMatch(original::contains);
        if (!anyMatch) return ValidateResult.NOOP;

        if (!autoFix) {
            List<String> sim = new ArrayList<>(original);
            for (String v : input) {
                sim.remove(v);
            }
            if (sim.size() < min) {
                return ValidateResult.BELOW_MIN;
            }
            this.value = sim;
            return ValidateResult.PASS;
        }

        List<String> working = new ArrayList<>(original);
        boolean removedAny = false;
        for (String v : input) {
            if (working.size() <= min) break;
            boolean removed = working.remove(v);
            if (removed) {
                removedAny = true;
            }
        }

        if (!removedAny) return ValidateResult.NOOP;

        this.value = working;
        return this.value.size() >= min ? ValidateResult.PASS : ValidateResult.BELOW_MIN;
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
