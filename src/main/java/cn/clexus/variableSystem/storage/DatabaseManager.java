package cn.clexus.variableSystem.storage;

import cn.clexus.variableSystem.VariableSystem;
import cn.clexus.variableSystem.model.Variable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseManager {

    private final VariableSystem plugin = VariableSystem.instance;
    private HikariDataSource dataSource;
    private final ExecutorService dbExecutor = Executors.newFixedThreadPool(4);

    private static final Gson GSON = new Gson();
    private static final Type MAP_STRING_STRING = new TypeToken<Map<String, String>>() {}.getType();

    public void connect() {
        plugin.info("正在连接数据库...");
        long start = System.currentTimeMillis();

        try {
            HikariConfig hikariConfig = getHikariConfig();
            dataSource = new HikariDataSource(hikariConfig);
            createTables();
        } catch (SQLException e) {
            plugin.warn("数据库连接失败: " + e.getMessage());
        }

        long end = System.currentTimeMillis();
        plugin.info("已连接到数据库，耗时 " + (end - start) + "ms");
    }

    private HikariConfig getHikariConfig() {
        var config = plugin.getConfig();
        var section = config.getConfigurationSection("database");
        if (section == null) throw new IllegalStateException("配置文件中缺失 database 部分!");

        String type = section.getString("type", "sqlite").toLowerCase();
        HikariConfig hikariConfig = new HikariConfig();

        switch (type) {
            case "postgres": {
                String host = section.getString("host", "localhost");
                int port = section.getInt("port", 5432);
                String db = section.getString("db", "postgres");
                String user = section.getString("user", "postgres");
                String password = section.getString("password", "");
                hikariConfig.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + db);
                hikariConfig.setUsername(user);
                hikariConfig.setPassword(password);
                hikariConfig.setDriverClassName("org.postgresql.Driver");
                break;
            }

            case "mysql": {
                String host = section.getString("host", "localhost");
                int port = section.getInt("port", 3306);
                String db = section.getString("db", "minecraft");
                String user = section.getString("user", "root");
                String password = section.getString("password", "");
                hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&serverTimezone=UTC");
                hikariConfig.setUsername(user);
                hikariConfig.setPassword(password);
                hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
                break;
            }

            case "sqlite": {
                String filePath = section.getString("file", "data.db");
                File dbFile = new File(plugin.getDataFolder(), filePath);
                hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
                hikariConfig.setDriverClassName("org.sqlite.JDBC");
                break;
            }

            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + type);
        }

        var pool = section.getConfigurationSection("pool");
        if (pool != null) {
            if (pool.contains("maximumPoolSize")) hikariConfig.setMaximumPoolSize(pool.getInt("maximumPoolSize"));
            if (pool.contains("minimumIdle")) hikariConfig.setMinimumIdle(pool.getInt("minimumIdle"));
            if (pool.contains("connectionTimeout")) hikariConfig.setConnectionTimeout(pool.getLong("connectionTimeout"));
            if (pool.contains("idleTimeout")) hikariConfig.setIdleTimeout(pool.getLong("idleTimeout"));
        }

        return hikariConfig;
    }


    private void createTables() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            String sqlPlayer = """
                    CREATE TABLE IF NOT EXISTS player_variables (
                        player_name VARCHAR(36) PRIMARY KEY,
                        variables TEXT
                    );
                    """;
            String sqlGlobal = """
                    CREATE TABLE IF NOT EXISTS global_variables (
                        variable_name VARCHAR(100) PRIMARY KEY,
                        variables TEXT
                    );
                    """;
            String sqlExpired = """
                    CREATE TABLE IF NOT EXISTS player_expired_variables (
                        player_name VARCHAR(36) PRIMARY KEY,
                        variables TEXT
                    );
                    """;

            stmt.execute(sqlPlayer);
            stmt.execute(sqlGlobal);
            stmt.execute(sqlExpired);

            plugin.info("数据库表创建完成");
        }
    }

    public CompletableFuture<Map<String, Map<String, Variable<?>>>> getAllPlayerVariables() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Map<String, Variable<?>>> result = new HashMap<>();
            String sql = "SELECT player_name, variables FROM player_variables";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, String>>() {}.getType();

                while (rs.next()) {
                    String playerName = rs.getString("player_name");
                    String json = rs.getString("variables");
                    if (json == null || json.isEmpty()) continue;

                    Map<String, String> map = gson.fromJson(json, type);
                    if (map == null) continue;

                    Map<String, Variable<?>> playerVars = new HashMap<>();
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        try {
                            Variable<?> var = Variable.deserialize(entry.getValue());
                            playerVars.put(entry.getKey(), var);
                        } catch (Exception e) {
                            plugin.warn("解析玩家变量失败: " + entry.getKey() + " -> " + e.getMessage());
                        }
                    }
                    result.put(playerName, playerVars);
                }

            } catch (SQLException e) {
                plugin.warn("加载所有离线玩家变量失败: " + e.getMessage());
                throw new RuntimeException(e);
            }

            return result;
        }, dbExecutor);
    }

    public CompletableFuture<Boolean> savePlayerVariables(@NotNull Player player) {
        return savePlayerVariables(plugin.getIdentifier(player));
    }

    public CompletableFuture<Boolean> savePlayerVariables(@NotNull String player) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                Map<String, Variable<?>> variables = plugin.getVariableManager().getOnlineVariables(player);

                Map<String, String> jsonMap = new HashMap<>();
                for (Map.Entry<String, Variable<?>> entry : variables.entrySet()) {
                    jsonMap.put(entry.getKey(), entry.getValue().serialize());
                }
                String json = GSON.toJson(jsonMap);

                String sql = """
                    INSERT INTO player_variables(player_name, variables)
                    VALUES (?, ?)
                    ON CONFLICT(player_name)
                    DO UPDATE SET variables = EXCLUDED.variables;
                    """;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, player);
                    stmt.setString(2, json);
                    stmt.executeUpdate();
                }
                return true;
            } catch (SQLException e) {
                plugin.warn("保存玩家变量失败: " + e.getMessage());
                return false;
            }
        }, dbExecutor);
    }

    public CompletableFuture<Boolean> expirePlayerVariable(@NotNull Player player, Variable<?> variable) {
        return expirePlayerVariable(plugin.getIdentifier(player), variable);
    }
    public CompletableFuture<Boolean> expirePlayerVariable(@NotNull String player, Variable<?> variable) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {

                String selectSql = "SELECT variables FROM player_expired_variables WHERE player_name = ?";
                Map<String, String> jsonMap = new HashMap<>();

                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setString(1, player);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (rs.next()) {
                            String existingJson = rs.getString("variables");
                            if (existingJson != null && !existingJson.isEmpty()) {
                                Map<String, String> existingMap = GSON.fromJson(existingJson, MAP_STRING_STRING);
                                if (existingMap != null) {
                                    jsonMap.putAll(existingMap);
                                }
                            }
                        }
                    }
                }

                jsonMap.put(variable.getVariableDefinition().id(), variable.serialize());
                String newJson = GSON.toJson(jsonMap);

                String insertSql = """
                INSERT INTO player_expired_variables(player_name, variables)
                VALUES (?, ?)
                ON CONFLICT(player_name)
                DO UPDATE SET variables = EXCLUDED.variables;
            """;

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, player);
                    insertStmt.setString(2, newJson);
                    insertStmt.executeUpdate();
                }

                return true;
            } catch (SQLException e) {
                plugin.warn("保存过期变量失败: " + e.getMessage());
                return false;
            }
        }, dbExecutor);
    }


    public CompletableFuture<Map<String, Variable<?>>> getPlayerVariables(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Variable<?>> result = new HashMap<>();
            try (Connection conn = dataSource.getConnection()) {
                String sql = "SELECT variables FROM player_variables WHERE player_name = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, playerName);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            String json = rs.getString("variables");
                            if (json != null && !json.isEmpty()) {
                                Map<String, String> map = GSON.fromJson(json, MAP_STRING_STRING);
                                if (map != null) {
                                    for (Map.Entry<String, String> entry : map.entrySet()) {
                                        try {
                                            Variable<?> var = Variable.deserialize(entry.getValue());
                                            result.put(entry.getKey(), var);
                                        } catch (Exception e) {
                                            plugin.warn("解析玩家变量失败: " + entry.getKey() + " -> " + e.getMessage());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.warn("加载玩家变量失败: " + e.getMessage());
                throw new RuntimeException(e);
            }
            return result;
        }, dbExecutor);
    }

    public void saveGlobalVariable(String variableName, Variable<?> variable) {
        dbExecutor.submit(() -> {
            try (Connection conn = dataSource.getConnection()) {
                String json = GSON.toJson(Map.of(variableName, variable.serialize()));

                String sql = """
                        INSERT INTO global_variables(variable_name, variables)
                        VALUES (?, ?)
                        ON CONFLICT(variable_name)
                        DO UPDATE SET variables = EXCLUDED.variables;
                        """;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, variableName);
                    stmt.setString(2, json);
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.warn("保存全局变量失败: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<Boolean> saveGlobalVariables(Map<String, Variable<?>> variables) {
        return CompletableFuture.supplyAsync(() -> {
            if (variables == null || variables.isEmpty()) return true;

            try (Connection conn = dataSource.getConnection()) {
                String sql = """
                    INSERT INTO global_variables(variable_name, variables)
                    VALUES (?, ?)
                    ON CONFLICT(variable_name)
                    DO UPDATE SET variables = EXCLUDED.variables;
                    """;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    for (Map.Entry<String, Variable<?>> entry : variables.entrySet()) {
                        String name = entry.getKey();
                        String json = GSON.toJson(Map.of(name, entry.getValue().serialize()));
                        stmt.setString(1, name);
                        stmt.setString(2, json);
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }

                return true;
            } catch (SQLException e) {
                plugin.warn("保存全局变量失败: " + e.getMessage());
                return false;
            }
        }, dbExecutor);
    }


    public CompletableFuture<Map<String, Variable<?>>> getGlobalVariables() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Variable<?>> result = new HashMap<>();
            try (Connection conn = dataSource.getConnection()) {
                String sql = "SELECT variables FROM global_variables";

                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {
                        String json = rs.getString("variables");
                        if (json == null || json.isEmpty()) continue;

                        Map<String, String> map = GSON.fromJson(json, MAP_STRING_STRING);
                        if (map == null) continue;

                        for (Map.Entry<String, String> entry : map.entrySet()) {
                            try {
                                Variable<?> var = Variable.deserialize(entry.getValue());
                                result.put(entry.getKey(), var);
                            } catch (Exception e) {
                                plugin.warn("解析全局变量失败: " + entry.getKey() + " -> " + e.getMessage());
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.warn("加载全局变量失败: " + e.getMessage());
                throw new RuntimeException(e);
            }
            return result;
        }, dbExecutor);
    }

    public void close() {
        dbExecutor.shutdown();
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
