package com.monitor.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitor.shared.SystemMetrics;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Main client application for the Remote System Performance Monitor.
 * Collects metrics periodically and transmits them to the server via TCP socket.
 * Implements automatic reconnection on connection failures.
 */
public class ClientApp {
    private static final int DEFAULT_SERVER_PORT = 5000;
    private static final String DEFAULT_SERVER_HOST = "localhost";
    private static final int METRICS_COLLECTION_INTERVAL = 5; // seconds
    private static final int CONNECTION_TIMEOUT = 3000; // milliseconds
    private static final int RECONNECTION_DELAY = 5; // seconds

    private String serverHost;
    private int serverPort;
    private Socket socket;
    private BufferedWriter writer;
    private ScheduledExecutorService scheduler;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile boolean running = true;

    /**
     * Constructs client with server connection details.
     * @param serverHost Target server hostname or IP address
     * @param serverPort Target server TCP port
     */
    public ClientApp(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    /**
     * Main entry point for client application.
     * Accepts optional command-line arguments: [hostname] [port]
     */
    public static void main(String[] args) {
        String host = DEFAULT_SERVER_HOST;
        int port = DEFAULT_SERVER_PORT;

        // Parse command-line arguments
        if (args.length >= 1) {
            host = args[0];
        }
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                ClientLogger.warn("Invalid port number, using default: " + DEFAULT_SERVER_PORT);
            }
        }

        ClientApp client = new ClientApp(host, port);
        client.start();
    }

    /**
     * Starts the client application and metrics collection scheduler.
     */
    public void start() {
        ClientLogger.info("Starting client application...");
        ClientLogger.info("Connecting to server at " + serverHost + ":" + serverPort);

        scheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "MetricsCollectorThread");
            t.setDaemon(false);
            return t;
        });

        // Schedule metrics collection every 5 seconds
        scheduler.scheduleAtFixedRate(
                this::sendMetricsCycle,
                0,
                METRICS_COLLECTION_INTERVAL,
                TimeUnit.SECONDS
        );

        // Handle shutdown gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        // Keep application running
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            ClientLogger.warn("Client interrupted");
            shutdown();
        }
    }

    /**
     * Single cycle of metrics collection and transmission.
     * Handles connection establishment if socket is disconnected.
     */
    private void sendMetricsCycle() {
        try {
            // Reconnect if socket is closed
            if (socket == null || socket.isClosed()) {
                connectToServer();
            }

            // Collect metrics from system
            SystemMetrics metrics = MetricsCollector.collectMetrics();
            ClientLogger.info("Collected metrics: CPU=" + metrics.getCpuUsage() + "% RAM=" + metrics.getRamUsage() + "%");

            // Send metrics to server as JSON
            String jsonMetrics = objectMapper.writeValueAsString(metrics);
            writer.write(jsonMetrics);
            writer.write("\n");
            writer.flush();

            ClientLogger.debug("Metrics snapshot sent to server");

        } catch (Exception e) {
            ClientLogger.error("Error in metrics cycle: " + e.getMessage(), e);
            cleanupConnection();
        }
    }

    /**
     * Establishes TCP socket connection to the server.
     * Includes retry logic with exponential backoff.
     */
    private void connectToServer() {
        int retries = 0;
        int maxRetries = 3;

        while (retries < maxRetries && running) {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(serverHost, serverPort), CONNECTION_TIMEOUT);
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                ClientLogger.info("Successfully connected to server at " + serverHost + ":" + serverPort);
                return;

            } catch (IOException e) {
                retries++;
                ClientLogger.warn("Connection attempt " + retries + " failed: " + e.getMessage());

                if (retries < maxRetries) {
                    try {
                        Thread.sleep(RECONNECTION_DELAY * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        if (running) {
            ClientLogger.error("Failed to connect to server after " + maxRetries + " attempts", null);
        }
    }

    /**
     * Closes socket and writer connections gracefully.
     */
    private void cleanupConnection() {
        try {
            if (writer != null) {
                writer.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            ClientLogger.error("Error closing connection", e);
        }
    }

    /**
     * Gracefully shuts down the client application.
     * Cancels scheduled tasks and closes connections.
     */
    private void shutdown() {
        ClientLogger.info("Shutting down client application...");
        running = false;

        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        cleanupConnection();
        ClientLogger.info("Client shutdown complete");
    }
}
