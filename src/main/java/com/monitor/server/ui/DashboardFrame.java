package com.monitor.server.ui;

import com.monitor.shared.SystemMetrics;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main GUI dashboard for server application.
 * Displays real-time metrics from all connected clients.
 * Updates automatically from active registry.
 */
public class DashboardFrame extends JFrame {
    private final ConcurrentHashMap<String, SystemMetrics> activeRegistry;
    private final JTable metricsTable;
    private final DefaultTableModel tableModel;
    private final JLabel statusLabel;

    /**
     * Constructs dashboard frame.
     * @param activeRegistry Reference to active client metrics
     */
    public DashboardFrame(ConcurrentHashMap<String, SystemMetrics> activeRegistry) {
        this.activeRegistry = activeRegistry;

        // Set look and feel
        try {
            javax.swing.UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            System.err.println("Error setting look and feel: " + e.getMessage());
        }

        setTitle("Remote System Performance Monitor - Server Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create header with status
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Active Clients Metrics");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        statusLabel = new JLabel("Waiting for clients...");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(statusLabel, BorderLayout.EAST);

        // Create table for metrics
        String[] columnNames = {"Client ID", "Hostname", "CPU Usage (%)", "RAM Usage (%)", "RAM (Used/Total GB)", "Last Update"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Read-only table
            }
        };

        metricsTable = new JTable(tableModel);
        metricsTable.setRowHeight(25);
        metricsTable.setFont(new Font("Arial", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(metricsTable);

        // Add components to main panel
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        setContentPane(mainPanel);

        // Start auto-refresh timer
        startAutoRefresh();
    }

    /**
     * Starts background thread to refresh metrics display.
     * Updates every 2 seconds.
     */
    private void startAutoRefresh() {
        Thread refreshThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(2000); // Refresh every 2 seconds
                    updateMetricsDisplay();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    /**
     * Updates table with current metrics from active registry.
     */
    private void updateMetricsDisplay() {
        // Clear existing rows
        tableModel.setRowCount(0);

        // Add rows for each active client
        for (Map.Entry<String, SystemMetrics> entry : activeRegistry.entrySet()) {
            SystemMetrics metrics = entry.getValue();

            long usedGB = metrics.getRamUsed() / (1024 * 1024 * 1024);
            long totalGB = metrics.getRamTotal() / (1024 * 1024 * 1024);
            String ramDisplay = usedGB + "GB / " + totalGB + "GB";

            String timestamp = new java.text.SimpleDateFormat("HH:mm:ss")
                    .format(new java.util.Date(metrics.getTimestamp()));

            tableModel.addRow(new Object[]{
                    metrics.getClientId(),
                    metrics.getHostname(),
                    String.format("%.2f", metrics.getCpuUsage()),
                    String.format("%.2f", metrics.getRamUsage()),
                    ramDisplay,
                    timestamp
            });
        }

        // Update status label
        int clientCount = activeRegistry.size();
        statusLabel.setText("Connected Clients: " + clientCount);
    }

    /**
     * Refreshes the dashboard display.
     * Call this method to manually update the table.
     */
    public void refresh() {
        updateMetricsDisplay();
    }
}
