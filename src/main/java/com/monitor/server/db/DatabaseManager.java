package com.monitor.server.db;

import com.monitor.shared.SystemMetrics;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages SQLite database operations for metrics persistence.
 * Handles initialization, metric storage, and historical data queries.
 * Thread-safe implementation for concurrent access.
 */
public class DatabaseManager {
    private static final String DATABASE_URL = "jdbc:sqlite:monitor.db";
    private static final String TABLE_METRICS = "metrics_history";
    private static final String TABLE_CLIENTS = "client_metadata";

    /**
     * Initializes database connection and creates necessary tables if they don't exist.
     * @throws SQLException if database operations fail
     */
    public static void initialize() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Create metrics history table
            String createMetricsTable = "CREATE TABLE IF NOT EXISTS " + TABLE_METRICS + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "client_id TEXT NOT NULL," +
                    "hostname TEXT NOT NULL," +
                    "timestamp LONG NOT NULL," +
                    "cpu_usage REAL NOT NULL," +
                    "ram_usage REAL NOT NULL," +
                    "ram_total LONG NOT NULL," +
                    "ram_used LONG NOT NULL" +
                    ")";
            stmt.execute(createMetricsTable);

            // Create client metadata table
            String createClientsTable = "CREATE TABLE IF NOT EXISTS " + TABLE_CLIENTS + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "client_id TEXT UNIQUE NOT NULL," +
                    "hostname TEXT NOT NULL," +
                    "first_seen LONG NOT NULL," +
                    "last_seen LONG NOT NULL" +
                    ")";
            stmt.execute(createClientsTable);

            // Create index for faster queries
            String createIndex = "CREATE INDEX IF NOT EXISTS idx_client_timestamp ON " +
                    TABLE_METRICS + "(client_id, timestamp)";
            stmt.execute(createIndex);
        }
    }

    /**
     * Gets database connection.
     * @return Connection to SQLite database
     * @throws SQLException if connection fails
     */
    public static synchronized Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL);
    }

    /**
     * Saves metrics snapshot to database.
     * @param metrics SystemMetrics object to persist
     * @throws SQLException if database operation fails
     */
    public static void saveMetrics(SystemMetrics metrics) throws SQLException {
        String insertQuery = "INSERT INTO " + TABLE_METRICS +
                " (client_id, hostname, timestamp, cpu_usage, ram_usage, ram_total, ram_used)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
            pstmt.setString(1, metrics.getClientId());
            pstmt.setString(2, metrics.getHostname());
            pstmt.setLong(3, metrics.getTimestamp());
            pstmt.setDouble(4, metrics.getCpuUsage());
            pstmt.setDouble(5, metrics.getRamUsage());
            pstmt.setLong(6, metrics.getRamTotal());
            pstmt.setLong(7, metrics.getRamUsed());

            pstmt.executeUpdate();
        }

        // Update client metadata
        updateClientMetadata(metrics.getClientId(), metrics.getHostname());
    }

    /**
     * Updates client metadata with latest connection information.
     */
    private static void updateClientMetadata(String clientId, String hostname) throws SQLException {
        long now = System.currentTimeMillis();

        String updateQuery = "INSERT OR REPLACE INTO " + TABLE_CLIENTS +
                " (client_id, hostname, first_seen, last_seen)" +
                " VALUES (?, ?, COALESCE((SELECT first_seen FROM " + TABLE_CLIENTS +
                " WHERE client_id = ?), ?), ?)";

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
            pstmt.setString(1, clientId);
            pstmt.setString(2, hostname);
            pstmt.setString(3, clientId);
            pstmt.setLong(4, now);
            pstmt.setLong(5, now);

            pstmt.executeUpdate();
        }
    }

    /**
     * Retrieves latest metrics for a specific client.
     * @param clientId Client identifier
     * @return Latest SystemMetrics or null if not found
     */
    public static SystemMetrics getLatestMetrics(String clientId) throws SQLException {
        String query = "SELECT * FROM " + TABLE_METRICS +
                " WHERE client_id = ? ORDER BY timestamp DESC LIMIT 1";

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, clientId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                SystemMetrics metrics = new SystemMetrics();
                metrics.setClientId(rs.getString("client_id"));
                metrics.setHostname(rs.getString("hostname"));
                metrics.setTimestamp(rs.getLong("timestamp"));
                metrics.setCpuUsage(rs.getDouble("cpu_usage"));
                metrics.setRamUsage(rs.getDouble("ram_usage"));
                metrics.setRamTotal(rs.getLong("ram_total"));
                metrics.setRamUsed(rs.getLong("ram_used"));
                return metrics;
            }
        }
        return null;
    }

    /**
     * Retrieves historical metrics for a client within time range.
     * @param clientId Client identifier
     * @param startTime Start timestamp in milliseconds
     * @param endTime End timestamp in milliseconds
     * @return List of metrics within range
     */
    public static List<SystemMetrics> getMetricsRange(String clientId, long startTime, long endTime)
            throws SQLException {
        List<SystemMetrics> metrics = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_METRICS +
                " WHERE client_id = ? AND timestamp BETWEEN ? AND ?" +
                " ORDER BY timestamp ASC";

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, clientId);
            pstmt.setLong(2, startTime);
            pstmt.setLong(3, endTime);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                SystemMetrics m = new SystemMetrics();
                m.setClientId(rs.getString("client_id"));
                m.setHostname(rs.getString("hostname"));
                m.setTimestamp(rs.getLong("timestamp"));
                m.setCpuUsage(rs.getDouble("cpu_usage"));
                m.setRamUsage(rs.getDouble("ram_usage"));
                m.setRamTotal(rs.getLong("ram_total"));
                m.setRamUsed(rs.getLong("ram_used"));
                metrics.add(m);
            }
        }
        return metrics;
    }

    /**
     * Gets count of metrics records in database.
     * @return Total number of metrics records
     */
    public static long getMetricsCount() throws SQLException {
        String query = "SELECT COUNT(*) as count FROM " + TABLE_METRICS;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                return rs.getLong("count");
            }
        }
        return 0;
    }

    /**
     * Deletes metrics older than specified timestamp.
     * Useful for cleanup and maintaining database size.
     * @param beforeTimestamp Timestamp threshold
     * @return Number of records deleted
     */
    public static int deleteMetricsOlderThan(long beforeTimestamp) throws SQLException {
        String query = "DELETE FROM " + TABLE_METRICS + " WHERE timestamp < ?";

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setLong(1, beforeTimestamp);
            return pstmt.executeUpdate();
        }
    }
}
