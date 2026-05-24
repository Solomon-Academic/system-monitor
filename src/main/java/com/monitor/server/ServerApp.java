package com.monitor.server;

import com.monitor.server.db.DatabaseManager;
import com.monitor.server.ui.DashboardFrame;
import com.monitor.server.ui.LoginDialog;
import com.monitor.server.web.HttpDashboardServer;
import com.monitor.shared.SystemMetrics;

import javax.swing.JOptionPane;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main server application for Remote System Performance Monitor.
 * Orchestrates TCP socket server, HTTP web dashboard, database, and GUI components.
 * Manages client connections and metric aggregation.
 */
public class ServerApp {
    private static final int DEFAULT_TCP_PORT = 5000;
    private static final int DEFAULT_HTTP_PORT = 8080;

    private int tcpPort = DEFAULT_TCP_PORT;
    private int httpPort = DEFAULT_HTTP_PORT;
    private boolean headlessMode = false;

    private ServerSocket serverSocket;
    private HttpDashboardServer httpServer;
    private DashboardFrame dashboardFrame;
    private ExecutorService threadPool;
    private final ConcurrentHashMap<String, SystemMetrics> activeRegistry = new ConcurrentHashMap<>();
    private volatile boolean running = true;

    /**
     * Main entry point for server application.
     * Accepts optional arguments: [--tcp=port] [--http=port] [--headless]
     */
    public static void main(String[] args) {
        ServerApp server = new ServerApp();

        // Parse command-line arguments
        for (String arg : args) {
            if (arg.startsWith("--tcp=")) {
                try {
                    server.tcpPort = Integer.parseInt(arg.substring(6));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid TCP port: " + e.getMessage());
                }
            } else if (arg.startsWith("--http=")) {
                try {
                    server.httpPort = Integer.parseInt(arg.substring(7));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid HTTP port: " + e.getMessage());
                }
            } else if (arg.equals("--headless")) {
                server.headlessMode = true;
            }
        }

        server.start();
    }

    /**
     * Starts the server application.
     * Initializes database, TCP listener, HTTP server, and GUI (if not headless).
     */
    public void start() {
        try {
            ServerLogger.info("Starting Remote System Performance Monitor Server");

            // Initialize database
            initializeDatabase();

            // Start TCP socket listener
            startTcpListener();

            // Start HTTP web dashboard
            startHttpDashboard();

            // Start GUI dashboard if not in headless mode
            if (!headlessMode && isDisplayAvailable()) {
                startGuiDashboard();
            } else if (!headlessMode) {
                ServerLogger.info("No display available, running in headless mode");
            }

            // Handle graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

            // Keep server running
            Thread.currentThread().join();

        } catch (Exception e) {
            ServerLogger.error("Failed to start server", e);
            System.exit(1);
        }
    }

    /**
     * Initializes database connection and schema.
     */
    private void initializeDatabase() {
        try {
            DatabaseManager.initialize();
            long metricsCount = DatabaseManager.getMetricsCount();
            ServerLogger.info("Database initialized. Existing metrics: " + metricsCount);
        } catch (SQLException e) {
            ServerLogger.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Starts TCP socket listener for client connections.
     * Accepts connections in separate thread pool.
     */
    private void startTcpListener() {
        try {
            serverSocket = new ServerSocket(tcpPort);
            threadPool = Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "ClientHandlerThread");
                t.setDaemon(false);
                return t;
            });

            Thread listenerThread = new Thread(() -> {
                ServerLogger.info("TCP Listener started on port " + tcpPort);
                ServerLogger.info("Waiting for client agents to connect...");

                while (running && !Thread.currentThread().isInterrupted()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        ClientHandler handler = new ClientHandler(clientSocket, activeRegistry);
                        threadPool.execute(handler);
                    } catch (Exception e) {
                        if (running) {
                            ServerLogger.error("Error accepting client connection", e);
                        }
                    }
                }
            });

            listenerThread.setName("TCPListenerThread");
            listenerThread.setDaemon(false);
            listenerThread.start();

        } catch (Exception e) {
            ServerLogger.error("Failed to start TCP listener", e);
            throw new RuntimeException("TCP listener initialization failed", e);
        }
    }

    /**
     * Starts HTTP web dashboard server.
     */
    private void startHttpDashboard() {
        httpServer = new HttpDashboardServer(httpPort, activeRegistry);
        Thread httpThread = new Thread(httpServer, "HttpDashboardThread");
        httpThread.setDaemon(false);
        httpThread.start();
    }

    /**
     * Starts GUI dashboard (if display available).
     * Shows login dialog before displaying dashboard.
     */
    private void startGuiDashboard() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            LoginDialog loginDialog = new LoginDialog(null);
            loginDialog.setVisible(true);

            if (loginDialog.isAuthenticated()) {
                dashboardFrame = new DashboardFrame(activeRegistry);
                dashboardFrame.setVisible(true);
            } else {
                System.exit(0);
            }
        });
    }

    /**
     * Checks if display is available for GUI.
     */
    private boolean isDisplayAvailable() {
        try {
            java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            return System.getenv("DISPLAY") != null || System.getProperty("os.name").startsWith("Windows") ||
                    System.getProperty("os.name").startsWith("Mac");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gracefully shuts down the server.
     */
    private void shutdown() {
        ServerLogger.info("Shutting down server...");
        running = false;

        // Close TCP listener
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            ServerLogger.error("Error closing socket", e);
        }

        // Shutdown thread pool
        if (threadPool != null) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Stop HTTP server
        if (httpServer != null) {
            httpServer.stop();
        }

        // Close GUI
        if (dashboardFrame != null) {
            dashboardFrame.dispose();
        }

        ServerLogger.info("Server shutdown complete");
    }

    /**
     * Gets the active client metrics registry.
     */
    public ConcurrentHashMap<String, SystemMetrics> getActiveRegistry() {
        return activeRegistry;
    }
}
