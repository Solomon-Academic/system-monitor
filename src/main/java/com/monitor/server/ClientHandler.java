package com.monitor.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitor.server.db.DatabaseManager;
import com.monitor.shared.SystemMetrics;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles individual client connections on the server.
 * Runs in separate thread for each connected client.
 * Reads incoming metrics, persists to database, and updates active registry.
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ConcurrentHashMap<String, SystemMetrics> activeRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String clientId = "unknown";

    /**
     * Constructs handler for a client connection.
     * @param socket The client socket connection
     * @param activeRegistry Shared registry of active clients and their metrics
     */
    public ClientHandler(Socket socket, ConcurrentHashMap<String, SystemMetrics> activeRegistry) {
        this.socket = socket;
        this.activeRegistry = activeRegistry;
    }

    /**
     * Main thread execution loop.
     * Reads metrics from client, persists, and updates registry.
     */
    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;

            ServerLogger.info("Client connected from " + socket.getInetAddress().getHostAddress());

            while ((line = reader.readLine()) != null && !Thread.currentThread().isInterrupted()) {
                try {
                    // Parse JSON metrics from client
                    SystemMetrics metrics = objectMapper.readValue(line, SystemMetrics.class);
                    clientId = metrics.getClientId();

                    // Store in database
                    DatabaseManager.saveMetrics(metrics);

                    // Update active registry with latest metrics
                    activeRegistry.put(clientId, metrics);

                    ServerLogger.debug("Metrics received from " + clientId);

                } catch (Exception e) {
                    ServerLogger.warn("Error processing metrics: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            ServerLogger.error("Error in client handler: " + e.getMessage(), e);
        } finally {
            handleClientDisconnection();
        }
    }

    /**
     * Handles cleanup when client disconnects.
     * Removes client from active registry but keeps database records.
     */
    private void handleClientDisconnection() {
        try {
            socket.close();
            activeRegistry.remove(clientId);
            ServerLogger.info("Client " + clientId + " disconnected");
        } catch (Exception e) {
            ServerLogger.error("Error during client disconnection", e);
        }
    }
}

/**
 * Simple server-side logging utility.
 */
class ServerLogger {
    public static void info(String message) {
        System.out.println("[INFO] " + message);
    }

    public static void debug(String message) {
        System.out.println("[DEBUG] " + message);
    }

    public static void warn(String message) {
        System.out.println("[WARN] " + message);
    }

    public static void error(String message, Exception e) {
        System.out.println("[ERROR] " + message);
        if (e != null) {
            e.printStackTrace();
        }
    }
}
