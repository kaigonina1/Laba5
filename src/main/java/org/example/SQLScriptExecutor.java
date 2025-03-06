package org.example;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public class SQLScriptExecutor {

    public static String readSQLScript(String resourcePath) throws Exception {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new Exception("Resource not found: " + resourcePath);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    public static void executeSQLScript(String dbName, String sqlScript, String user, String password) throws Exception {
        String url = "jdbc:postgresql://localhost:5432/" + dbName;
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            String[] commands = sqlScript.split("(?<!\\$\\$);\\s*");

            for (String command : commands) {
                command = command.trim();
                if (!command.isEmpty()) {
                    try {
                        System.out.println("Executing: " + command); // Логирование команды
                        stmt.execute(command);
                    } catch (SQLException e) {
                        System.err.println("Ошибка при выполнении команды: " + command);
                        e.printStackTrace(); // Вывести стек вызовов ошибки
                    }
                }
            }
        }
    }
}
