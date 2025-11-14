package cn.clexus.variableSystem.model;

import cn.clexus.variableSystem.VariableSystem;
import com.google.gson.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
public abstract class Variable<T> {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public enum ValidateResult {
        PASS,
        BELOW_MIN,
        ABOVE_MAX,
        INVALID_TYPE,
        NOOP
    }

    VariableDefinition<T> variableDefinition;
    @Setter
    T value;
    long expireTime;
    long createTime;

    public Variable(VariableDefinition<T> variableDefinition, long createTime) {
        this(variableDefinition, variableDefinition.defaultValue(), createTime);
    }

    public Variable(VariableDefinition<T> variableDefinition, T value, long createTime) {
        this.variableDefinition = variableDefinition;
        this.value = value;
        this.createTime = createTime;
        long expireAfter = variableDefinition.expireAfter();
        long expireAt = variableDefinition.expireAt();

        if (expireAfter == -1 && expireAt == -1) {
            this.expireTime = -1;
        } else if(expireAt == -1) {
            this.expireTime = createTime + expireAfter;
        } else if(expireAfter == -1) {
            this.expireTime = expireAt;
        }else{
            this.expireTime = Math.min(createTime + expireAfter, expireAt);
        }
    }


    public boolean expires() {
        long now = System.currentTimeMillis();
        return expireTime != -1 && now >= expireTime;
    }

    public void reset(){
        value = variableDefinition.defaultValue();
    }

    public abstract ValidateResult validate(T value);

    public abstract ValidateResult add(T value, boolean autoFix);

    public abstract ValidateResult take(T value, boolean autoFix);

    public ValidateResult add(T value) {
        return add(value, false);
    }

    public ValidateResult take(T value) {
        return take(value, false);
    }

    public static String valueAsString(Object value) {
        StringBuilder builder = new StringBuilder();
        switch (value){
            case Boolean b -> builder.append(b);
            case String s -> builder.append(s);
            case Number n -> builder.append(n);
            case List<?> l -> {
                builder.append("[");
                for (int i = 0; i < l.size(); i++) {
                    if (i == l.size() - 1) {
                        builder.append(l.get(i));
                    }else{
                        builder.append(l.get(i)).append(",");
                    }
                }
                builder.append("]");
            }
            case Map<?, ?> m -> {
                builder.append("{");
                for (Map.Entry<?, ?> entry : m.entrySet()) {
                    builder.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
                }
                builder.deleteCharAt(builder.length() - 1);
                builder.append("}");
            }
            default -> builder.append(value);
        }
        return builder.toString();
    }

    public String valueAsString() {
        return valueAsString(value);
    }

    public String serialize() {
        JsonObject json = new JsonObject();
        json.addProperty("id", variableDefinition.id());
        json.addProperty("type", this.getClass().getSimpleName());
        json.addProperty("createTime", createTime);

        switch (value) {
            case null -> json.add("value", null);
            case Number number -> json.addProperty("value", number);
            case Boolean b -> json.addProperty("value", b);
            case String s -> json.addProperty("value", s);
            default ->
                    json.add("value", GSON.toJsonTree(value));
        }

        return GSON.toJson(json);
    }

    @SuppressWarnings("unchecked")
    public static Variable<?> deserialize(String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        String id = obj.get("id").getAsString();
        String type = obj.get("type").getAsString();
        long createTime = obj.get("createTime").getAsLong();
        JsonElement valueElem = obj.get("value");

        VariableDefinition<?> def = null;
        for (VariableDefinition<?> d : VariableSystem.instance.getLoader().getDefinitions()) {
            if (d.id().equals(id)) {
                def = d;
                break;
            }
        }
        if (def == null) {
            VariableSystem.instance.warn("未找到ID为 " + id + " 的变量定义，将使用默认定义");
            switch (type) {
                case "BooleanVariable" ->
                        def = VariableDefinition.defaultBooleanDefinition;
                case "IntVariable" ->
                        def = VariableDefinition.defaultIntDefinition;
                case "FloatVariable" ->
                        def = VariableDefinition.defaultFloatDefinition;
                case "StringVariable" ->
                        def = VariableDefinition.defaultStringDefinition;
                case "NumberList", "StringList" ->
                        def = VariableDefinition.defaultListDefinition;
                case "StringNumberMap", "StringStringMap" ->
                        def = VariableDefinition.defaultMapDefinition;
                default -> throw new IllegalArgumentException("未知变量类型: " + type);
            };
        }

        return switch (type) {
            case "BooleanVariable" ->
                    new BooleanVariable((VariableDefinition<Boolean>) def, valueElem.getAsBoolean(), createTime);
            case "IntVariable" ->
                    new IntVariable((VariableDefinition<Long>) def, valueElem.getAsLong(), createTime);
            case "FloatVariable" ->
                    new FloatVariable((VariableDefinition<Float>) def, valueElem.getAsFloat(), createTime);
            case "StringVariable" ->
                    new StringVariable((VariableDefinition<String>) def, valueElem.getAsString(), createTime);
            case "NumberList" -> new NumberList((VariableDefinition<List<Double>>) def,
                    GSON.fromJson(valueElem, List.class), createTime);
            case "StringList" -> new StringList((VariableDefinition<List<String>>) def,
                    GSON.fromJson(valueElem, List.class), createTime);
            case "StringNumberMap" -> new StringNumberMap((VariableDefinition<Map<String, Double>>) def,
                    GSON.fromJson(valueElem, Map.class), createTime);
            case "StringStringMap" -> new StringStringMap((VariableDefinition<Map<String, String>>) def,
                    GSON.fromJson(valueElem, Map.class), createTime);
            default -> throw new IllegalArgumentException("未知变量类型: " + type);
        };
    }


}
