package cn.clexus.variableSystem.model;

public class BooleanVariable extends Variable<Boolean> {
    public BooleanVariable(VariableDefinition<Boolean> variableDefinition, Boolean value, long createTime) {
        super(variableDefinition, value, createTime);
    }

    @Override
    public ValidateResult validate(Boolean value) {
        return ValidateResult.INVALID_TYPE;
    }

    @Override
    public ValidateResult add(Boolean value, boolean autoFix) {
        return ValidateResult.INVALID_TYPE;
    }

    @Override
    public ValidateResult take(Boolean value, boolean autoFix) {
        return ValidateResult.INVALID_TYPE;
    }
}
