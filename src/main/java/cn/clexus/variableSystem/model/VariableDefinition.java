package cn.clexus.variableSystem.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record VariableDefinition<T>(String id, T defaultValue, Class<T> type, Double minValue, Double maxValue, Long expireAfter, Long expireAt) {
    public static VariableDefinition<String> defaultStringDefinition = new VariableDefinition<>("defaultString", "", String.class, 0D, Double.MAX_VALUE, -1L, -1L);
    public static VariableDefinition<Float> defaultFloatDefinition = new VariableDefinition<>("defaultFloat", 0F, Float.class, 0D, Double.MAX_VALUE, -1L, -1L);
    public static VariableDefinition<Boolean> defaultBooleanDefinition = new VariableDefinition<>("defaultBoolean", false, Boolean.class, 0D, Double.MAX_VALUE, -1L, -1L);
    public static VariableDefinition<Long> defaultIntDefinition = new VariableDefinition<>("defaultInt", 0L, Long.class, 0D, Double.MAX_VALUE, -1L, -1L);
    public static VariableDefinition<List> defaultListDefinition = new VariableDefinition<>("defaultList", new ArrayList<>(), List.class, 0D, Double.MAX_VALUE, -1L, -1L);
    public static VariableDefinition<Map> defaultMapDefinition = new VariableDefinition<>("defaultMap", new HashMap<>(), Map.class, 0D, Double.MAX_VALUE, -1L, -1L);
}
