package org.example.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB {
    private static final String URL  = "jdbc:mysql://localhost:3306/BANCO?serverTimezone=UTC";
    private static final String USER = "root";          // <-- tu usuario
    private static final String PASS = "Mancora#1";   // <-- tu password

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}

