package com.monitor.server.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitor.shared.SystemMetrics;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP server for web dashboard.
 * Provides RESTful endpoints for metrics visualization.
 * Runs on separate thread from main server.
 */
public class HttpDashboardServer implements Runnable {
    private final int port;
    private final ConcurrentHashMap<String, SystemMetrics> activeRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile boolean running = true;

    /**
     * Constructs HTTP dashboard server.
     * @param port HTTP server port
     * @param activeRegistry Shared registry of active clients
     */
    public HttpDashboardServer(int port, ConcurrentHashMap<String, SystemMetrics> activeRegistry) {
        this.port = port;
        this.activeRegistry = activeRegistry;
    }

    /**
     * Starts HTTP server listening for requests.
     */
    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[INFO] HTTP Dashboard Server started on port " + port);
            System.out.println("[INFO] Access at http://localhost:" + port + "/dashboard");

            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> handleHttpRequest(clientSocket)).start();
                } catch (Exception e) {
                    if (running) {
                        System.err.println("[ERROR] Error accepting HTTP connection: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] HTTP Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles individual HTTP requests.
     * Supports /dashboard endpoint for HTML and /metrics for JSON.
     */
    private void handleHttpRequest(Socket socket) {
        try (InputStreamReader reader = new InputStreamReader(socket.getInputStream());
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            // Read HTTP request
            StringBuilder requestBuilder = new StringBuilder();
            char[] buffer = new char[1024];
            int charsRead = reader.read(buffer);
            if (charsRead > 0) {
                requestBuilder.append(new String(buffer, 0, charsRead));
            }

            String request = requestBuilder.toString();
            String[] lines = request.split("\r\n");
            if (lines.length == 0) {
                return;
            }

            String requestLine = lines[0];
            String[] parts = requestLine.split(" ");
            String path = parts.length > 1 ? parts[1] : "/";

            // Route requests
            if (path.startsWith("/metrics")) {
                sendMetricsResponse(writer);
            } else if (path.startsWith("/dashboard") || path.equals("/")) {
                sendDashboardResponse(writer);
            } else {
                send404Response(writer);
            }

        } catch (Exception e) {
            System.err.println("[ERROR] Error handling HTTP request: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    /**
     * Sends JSON metrics response.
     */
    private void sendMetricsResponse(BufferedWriter writer) throws Exception {
        String json = objectMapper.writeValueAsString(activeRegistry);
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: " + json.length() + "\r\n" +
                "\r\n" +
                json;
        writer.write(response);
        writer.flush();
    }

    /**
     * Sends HTML dashboard response.
     */
    private void sendDashboardResponse(BufferedWriter writer) throws Exception {
        String html = buildDashboardHtml();
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: " + html.length() + "\r\n" +
                "\r\n" +
                html;
        writer.write(response);
        writer.flush();
    }

    /**
     * Sends 404 Not Found response.
     */
    private void send404Response(BufferedWriter writer) throws Exception {
        String response = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: 13\r\n" +
                "\r\n" +
                "404 Not Found";
        writer.write(response);
        writer.flush();
    }

    /**
     * Builds HTML dashboard with client metrics.
     */
    private String buildDashboardHtml() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("<title>System Monitor Dashboard</title>\n");
        html.append("<style>\n");
        html.append("  body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }\n");
        html.append("  .container { max-width: 1200px; margin: 0 auto; }\n");
        html.append("  .client-card { background: white; padding: 15px; margin: 10px 0; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
        html.append("  .metric { display: inline-block; margin-right: 20px; }\n");
        html.append("  .metric-value { font-size: 24px; font-weight: bold; color: #333; }\n");
        html.append("  .metric-label { font-size: 12px; color: #666; }\n");
        html.append("  .cpu-high { color: #d9534f; }\n");
        html.append("  .cpu-medium { color: #f0ad4e; }\n");
        html.append("  .cpu-low { color: #5cb85c; }\n");
        html.append("  h1 { color: #333; }\n");
        html.append("  .timestamp { color: #999; font-size: 12px; }\n");
        html.append("</style>\n");
        html.append("<script>\n");
        html.append("  function refreshMetrics() {\n");
        html.append("    fetch('/metrics')\n");
        html.append("      .then(r => r.json())\n");
        html.append("      .then(data => {\n");
        html.append("        const container = document.getElementById('metrics');\n");
        html.append("        container.innerHTML = '';\n");
        html.append("        Object.entries(data).forEach(([id, m]) => {\n");
        html.append("          const cpuClass = m.cpuUsage > 80 ? 'cpu-high' : m.cpuUsage > 50 ? 'cpu-medium' : 'cpu-low';\n");
        html.append("          const card = `\n");
        html.append("            <div class='client-card'>\n");
        html.append("              <h3>${m.hostname} (${m.clientId})</h3>\n");
        html.append("              <div class='metric'>\n");
        html.append("                <div class='metric-label'>CPU Usage</div>\n");
        html.append("                <div class='metric-value ${cpuClass}'>${m.cpuUsage.toFixed(2)}%</div>\n");
        html.append("              </div>\n");
        html.append("              <div class='metric'>\n");
        html.append("                <div class='metric-label'>RAM Usage</div>\n");
        html.append("                <div class='metric-value'>${m.ramUsage.toFixed(2)}%</div>\n");
        html.append("              </div>\n");
        html.append("              <div class='metric'>\n");
        html.append("                <div class='metric-label'>RAM (Used/Total)</div>\n");
        html.append("                <div class='metric-value'>${(m.ramUsed/1024/1024/1024).toFixed(2)}GB / ${(m.ramTotal/1024/1024/1024).toFixed(2)}GB</div>\n");
        html.append("              </div>\n");
        html.append("              <p class='timestamp'>Last updated: ${new Date(m.timestamp).toLocaleString()}</p>\n");
        html.append("            </div>\n");
        html.append("          `;\n");
        html.append("          container.innerHTML += card;\n");
        html.append("        });\n");
        html.append("      })\n");
        html.append("      .catch(e => console.error('Error:', e));\n");
        html.append("  }\n");
        html.append("  setInterval(refreshMetrics, 5000);\n");
        html.append("  refreshMetrics();\n");
        html.append("</script>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<div class='container'>\n");
        html.append("  <h1>System Monitor Dashboard</h1>\n");
        html.append("  <p>Real-time system metrics from connected clients (auto-refreshes every 5 seconds)</p>\n");
        html.append("  <div id='metrics'></div>\n");
        html.append("</div>\n");
        html.append("</body>\n");
        html.append("</html>\n");
        return html.toString();
    }

    /**
     * Stops the HTTP server.
     */
    public void stop() {
        running = false;
    }
}
