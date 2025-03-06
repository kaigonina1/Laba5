package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBManager {
    private String url;
    private String user;
    private String password;

    public DBManager(String dbName, String user, String password) {
        this.url = "jdbc:postgresql://localhost:5432/" + dbName;
        this.user = user;
        this.password = password;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public Connection getConnection(String dbName) throws SQLException {
        String dbUrl = "jdbc:postgresql://localhost:5432/" + dbName + "?options=-c%20search_path=public";
        return DriverManager.getConnection(dbUrl, user, password);
    }

    public void initProcedures() throws Exception {
        String dbName = getDatabaseName();
        String script = SQLScriptExecutor.readSQLScript("/stored_procedures.sql");
        SQLScriptExecutor.executeSQLScript(dbName, script, user, password);
    }

    private String getDatabaseName() {
        return url.substring(url.lastIndexOf("/") + 1);
    }

    public boolean databaseExists(String dbName) {
        String testUrl = "jdbc:postgresql://localhost:5432/" + dbName;
        try (Connection conn = DriverManager.getConnection(testUrl, user, password)) {
            return true;  // Если соединение успешно, значит база существует
        } catch (SQLException e) {
            // Если ошибка подключения, значит база не существует
            if (e.getSQLState().equals("08006")) { // Проверяем ошибку подключения к базе данных
                return false;
            }
            // Логируем все другие исключения
            e.printStackTrace();
            return false;
        }
    }


    public void createDatabase(String newDbName) throws SQLException {
        // Проверка существования базы данных
        if (!databaseExists(newDbName)) {
            try (Connection conn = getConnection("postgres");
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE " + newDbName);
                System.out.println("✅ База данных " + newDbName + " успешно создана!");
            }
        } else {
            System.out.println("⚠️ База данных " + newDbName + " уже существует.");
        }
    }



    public void dropDatabase(String dbName) throws SQLException {
        try (Connection conn = getConnection("postgres");
             CallableStatement stmt = conn.prepareCall("CALL sp_drop_database(?, ?)")) {
            stmt.setString(1, dbName);
            stmt.setString(2, password);
            stmt.execute();
        }
    }

    public void createTable(String tableName) throws SQLException {
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall("CALL sp_create_table(?)")) {
            stmt.setString(1, tableName);
            stmt.execute();
        }
    }

    public void clearTable(String tableName) throws SQLException {
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall("CALL sp_clear_table(?)")) {
            stmt.setString(1, tableName);
            stmt.execute();
        }
    }

    public void addChild(String tableName, String surname, String name, String lastName, int group) throws SQLException {
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall("CALL sp_add_child(?, ?, ?, ?, ?)")) {
            stmt.setString(1, tableName);
            stmt.setString(2, surname);
            stmt.setString(3, name);
            stmt.setString(4, lastName);
            stmt.setInt(5, group);
            stmt.execute();
        }
    }

    public List<Child> searchChildBySurname(String tableName, String surname) throws SQLException {
        List<Child> children = new ArrayList<>();
        String sql = "SELECT * FROM sp_search_child_by_surname(?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName);
            stmt.setString(2, surname);
            try (ResultSet rs = stmt.executeQuery()) {
                while(rs.next()){
                    Child child = new Child(
                            rs.getInt("id"),
                            rs.getString("surname"),
                            rs.getString("name"),
                            rs.getString("lastName"),
                            rs.getInt("group")
                    );
                    children.add(child);
                }
            }
        }
        return children;
    }

    public void updateChild(String tableName, int id, String surname, String name, String lastName, int group) throws SQLException {
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall("CALL sp_update_child(?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, tableName);
            stmt.setInt(2, id);
            stmt.setString(3, surname);
            stmt.setString(4, name);
            stmt.setString(5, lastName);
            stmt.setInt(6, group);
            stmt.execute();
        }
    }

    public void deleteChildBySurname(String tableName, String surname) throws SQLException {
        try (Connection conn = getConnection();
             CallableStatement stmt = conn.prepareCall("CALL sp_delete_child_by_surname(?, ?)")) {
            stmt.setString(1, tableName);
            stmt.setString(2, surname);
            stmt.execute();
        }
    }

}