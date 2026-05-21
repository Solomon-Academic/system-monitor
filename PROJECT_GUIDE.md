# Remote System Performance Monitor - Project Guide

A comprehensive guide to understanding, building, and operating the Remote System Performance Monitoring application.

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture & Design](#architecture--design)
3. [Getting Started](#getting-started)
4. [Building the Project](#building-the-project)
5. [Running the Application](#running-the-application)
6. [Understanding the Codebase](#understanding-the-codebase)
7. [Configuration & Customization](#configuration--customization)
8. [Database Management](#database-management)
9. [Deployment Guide](#deployment-guide)
10. [Troubleshooting](#troubleshooting)
11. [Development Workflow](#development-workflow)

---

## Project Overview

### What is this application?

The Remote System Performance Monitor is a distributed monitoring solution that:
- Collects system metrics from multiple remote machines
- Aggregates data in a centralized server
- Displays metrics through GUI and web dashboards
- Persists historical data in a local SQLite database

### Who uses it?

- **System Administrators**: Monitor multiple servers from one location
- **DevOps Teams**: Track system health across infrastructure
- **Developers**: Debug performance issues on remote systems
- **Operations**: Keep historical records of system performance

### What does it monitor?

Each client agent collects and transmits:
- **CPU Usage**: Current CPU utilization percentage
- **RAM Usage**: Memory consumption (used vs total)
- **Running Processes**: List of active processes with details
- **System Metadata**: Hostname, client ID, collection timestamp

---

## Architecture & Design

### Component Overview

```
┌──────────────────┐
│  Client Agents   │  (Multiple machines)
│  • MetricsCollector
│  • ScheduledTask │
└────────┬─────────┘
         │ TCP Socket Connection
         │ JSON Payload every 5 seconds
         ▼
┌──────────────────────┐
│   Server (Central)   │
│ • TCP Listener       │
│ • HTTP Server        │
│ • SQLite DB          │
│ • GUI Dashboard      │
│ • Active Registry    │
└─────────┬──────────┬─┘
          │          │
          ▼          ▼
┌──────────────┐  ┌──────────────┐
│ GUI Dashboard│  │ Web Dashboard│
│ (Swing/FlatLaf) │  │ (Port 8080) │
└──────────────┘  └──────────────┘
```

### Design Patterns Used

**1. Producer-Consumer Pattern**
- Clients produce metrics continuously
- Server consumes and processes metrics
- Active registry acts as shared state

**2. Thread-Per-Connection Pattern**
- Server spawns new thread per client connection
- Enables concurrent metric processing
- Daemon threads for clean shutdown

**3. Scheduler Pattern**
- Client uses ScheduledExecutorService
- Fixed-rate metric collection (5 seconds)
- Automatic reconnection on failures

**4. Registry Pattern**
- ConcurrentHashMap stores active clients
- Enables fast metric lookup
- Thread-safe for multi-client scenarios

### Data Flow

```
1. Client starts → Reads system metrics → Serializes to JSON
2. Client connects to server via TCP
3. Server accepts connection → Spawns ClientHandler thread
4. ClientHandler reads JSON metrics → Stores in DB → Updates active registry
5. Dashboards query active registry → Display current/historical data
6. On disconnect → Update registry → Retry connection on client
```

---

## Getting Started

### Prerequisites

**System Requirements**
- Java 21 JDK (not JRE)
- Maven 3.6 or higher
- 100MB free disk space
- Network connectivity between client and server

**Installation Steps**

1. **Install Java 21**
   ```bash
   # Ubuntu/Debian
   sudo apt-get install openjdk-21-jdk
   
   # macOS
   brew install openjdk@21
   
   # Verify installation
   java -version
   ```

2. **Install Maven**
   ```bash
   # Ubuntu/Debian
   sudo apt-get install maven
   
   # macOS
   brew install maven
   
   # Verify installation
   mvn -version
   ```

3. **Clone/Get Project**
   ```bash
   cd /path/to/project
   ls -la src/main/java/com/monitor/
   ```

### Project Layout Understanding

**Main Source Files** (`src/main/java/com/monitor/`)

**Client Module** (`client/`)
- `ClientApp.java` - Entry point, scheduler, socket management
- `MetricsCollector.java` - Gathers CPU/RAM/process info
- `ClientLogger.java` - Logging utilities

**Server Module** (`server/`)
- `ServerApp.java` - Main server, TCP listener setup
- `ClientHandler.java` - Handles individual client connections
- `db/DatabaseManager.java` - SQLite operations
- `web/HttpDashboardServer.java` - HTTP server for web dashboard
- `ui/LoginDialog.java` - Authentication dialog
- `ui/DashboardFrame.java` - Swing GUI components

**Shared Module** (`shared/`)
- `SystemMetrics.java` - Data class for metrics payload
- `ProcessInfo.java` - Data class for process information

**Build Artifacts** (`target/`)
- Generated `.class` files
- Maven metadata
- Build logs

---

## Building the Project

### Clean Build

Removes all previous builds and compiles from scratch:

```bash
mvn clean install
```

**What happens:**
1. `clean` - Deletes `target/` directory
2. `install` - Compiles Java source → Runs tests → Packages JAR

**Expected output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXs
```

### Incremental Build

Faster - only recompiles changed files:

```bash
mvn compile
```

### Compile Only

Just compile without packaging:

```bash
mvn compile
```

### Verify Build

Check if everything compiles and runs tests:

```bash
mvn verify
```

### Troubleshooting Build Issues

**Issue: "Java version not compatible"**
```bash
# Check Maven is using Java 21
mvn -version
# Output should show Java version 21.x.x
```

**Issue: "Cannot find project artifact"**
```bash
# Clear Maven cache and rebuild
rm -rf ~/.m2/repository
mvn clean install
```

**Issue: "Compilation errors"**
```bash
# Check if all dependencies downloaded
mvn dependency:resolve
# Check for syntax errors
mvn compile -X
```

---

## Running the Application

### Server Configuration

**Basic Server Start**
```bash
mvn exec:java@run-server
```
- Starts on TCP port 5000
- Starts HTTP server on port 8080
- Launches GUI (if display available)
- Creates `monitor.db` if not exists

**Headless Server** (for remote servers)
```bash
mvn exec:java@run-server -- --headless
```
- No GUI launched
- Runs in background
- Good for 24/7 monitoring
- Logs to console

**Custom Ports**
```bash
mvn exec:java@run-server -- --tcp=5555 --http=8888
```
- TCP listener on port 5555
- HTTP server on port 8888
- Useful for multiple server instances

**Combined Options**
```bash
mvn exec:java@run-server -- --tcp=5000 --http=8080 --headless
```

### Client Configuration

**Connect to Local Server**
```bash
mvn exec:java@run-client
```
- Connects to `localhost:5000`
- Collects metrics every 5 seconds
- Logs to `client.log`

**Connect to Remote Server**
```bash
mvn exec:java@run-client -- 192.168.1.100 5000
```
- Connects to `192.168.1.100:5000`
- First argument: server hostname/IP
- Second argument: server port

**Examples:**
```bash
# Connect to server on different machine
mvn exec:java@run-client -- server.example.com 5000

# Connect to non-standard port
mvn exec:java@run-client -- localhost 5555

# Multiple clients (each in separate terminal)
Terminal 1: mvn exec:java@run-client -- server 5000
Terminal 2: mvn exec:java@run-client -- server 5000
Terminal 3: mvn exec:java@run-client -- server 5000
```

### Typical Workflow

**Terminal 1 - Start Server**
```bash
cd /home/skymoon/Documents/java_Code/project
mvn clean install
mvn exec:java@run-server
# Wait for "Waiting for client agents to connect..."
```

**Terminal 2 - Start Client 1**
```bash
cd /home/skymoon/Documents/java_Code/project
mvn exec:java@run-client
# Watch logs: "Metrics snapshot sent..."
```

**Terminal 3 - Start Client 2**
```bash
cd /home/skymoon/Documents/java_Code/project
mvn exec:java@run-client -- localhost 5000
```

**Browser - View Web Dashboard**
```
http://localhost:8080/dashboard
```

**Server GUI - View Metrics**
```
Login credentials: (check LoginDialog.java)
View live metrics and historical graphs
```

### Stopping the Application

**Graceful Shutdown**
```bash
# Press Ctrl+C in terminal where application is running
# Server will clean up resources:
# - Close database connections
# - Stop HTTP server
# - Clear active registry
# - Close socket listeners
```

**Force Kill** (last resort)
```bash
# Find process
ps aux | grep java | grep ServerApp

# Kill process
kill -9 <PID>

# Check for locks
lsof | grep monitor.db
```

---

## Understanding the Codebase

### Key Classes & Their Responsibilities

**ClientApp.java**
- Entry point for client agent
- Manages socket connection
- Schedules metrics collection
- Handles reconnection logic

Key methods:
- `main(String[] args)` - Parse arguments, start scheduler
- `start()` - Begin metrics collection cycle
- `sendMetricsCycle()` - Collect metrics and send to server
- `cleanupConnection()` - Close sockets gracefully

**MetricsCollector.java**
- Collects CPU, RAM, process information
- Uses Java platform APIs
- Returns SystemMetrics object

Key methods:
- `collectMetrics()` - Main collection method
- `getCpuUsage()` - CPU percentage
- `getRamUsage()` - Memory consumption
- `getRunningProcesses()` - Active process list

**ServerApp.java**
- Main server application
- Listens for client connections
- Manages HTTP server
- Initializes database

Key methods:
- `start()` - Start all server components
- `shutdown()` - Graceful shutdown
- `getActiveRegistry()` - Get current metrics

**ClientHandler.java**
- Handles individual client connections
- Reads incoming metrics
- Updates active registry
- Stores metrics in database

**HttpDashboardServer.java**
- Lightweight HTTP server
- Serves dashboard endpoint
- Returns metrics as JSON/HTML

**DatabaseManager.java**
- SQLite database management
- Stores historical metrics
- Provides data persistence

**LoginDialog.java**
- Swing authentication UI
- User credential validation
- Access control to server GUI

**DashboardFrame.java**
- Main Swing GUI window
- Displays live metrics
- Shows charts/graphs (JFreeChart)

### Code Flow Examples

**Example 1: Client Sends Metrics**
```
ClientApp.sendMetricsCycle()
  ├─ Check socket connection
  ├─ MetricsCollector.collectMetrics()
  │   ├─ Get CPU usage
  │   ├─ Get RAM usage
  │   └─ Get process list
  ├─ ObjectMapper.writeValueAsString() [JSON]
  ├─ BufferedWriter.write(json)
  ├─ BufferedWriter.flush()
  └─ ClientLogger.info("Metrics sent")
```

**Example 2: Server Receives Metrics**
```
ServerApp.start()
  └─ ServerSocket.accept()
     ├─ New ClientHandler thread spawned
     └─ ClientHandler.run()
        ├─ Read JSON from socket
        ├─ ObjectMapper.readValue() [parse JSON]
        ├─ Update activeRegistry
        ├─ DatabaseManager.saveMetrics()
        └─ Loop for next metrics
```

**Example 3: Dashboard Display**
```
DashboardFrame initialization
  ├─ Query activeRegistry
  ├─ Extract latest metrics per client
  ├─ Create charts (JFreeChart)
  ├─ Update UI components
  └─ Refresh every N seconds
```

---

## Configuration & Customization

### Basic Configuration Changes

**Change Metrics Collection Interval**

In `ClientApp.java`, line ~45:
```java
// Current: 5 seconds
scheduler.scheduleAtFixedRate(this::sendMetricsCycle, 0, 5, TimeUnit.SECONDS);

// Change to 10 seconds
scheduler.scheduleAtFixedRate(this::sendMetricsCycle, 0, 10, TimeUnit.SECONDS);
```

**Change Default Ports**

In `ServerApp.java`, line ~131:
```java
// Current defaults
int tcpPort = 5000;
int httpPort = 8080;

// Change to custom
int tcpPort = 6000;
int httpPort = 9000;
```

**Change Socket Connection Timeout**

In `ClientApp.java`, line ~62:
```java
// Current: 3 seconds
socket.connect(new InetSocketAddress(host, port), 3000);

// Change to 5 seconds
socket.connect(new InetSocketAddress(host, port), 5000);
```

### Advanced Configuration

**Add SSL/TLS Support**
- Modify ClientApp and ServerApp for SSLSocket
- Generate self-signed certificate
- Pass truststore to JVM

**Add Authentication to Server**
- Modify ClientHandler to validate client credentials
- Add client ID validation
- Implement token-based auth

**Add Database Replication**
- Implement DatabaseManager to sync to remote DB
- Add backup mechanism
- Configure retention policy

**Add Metrics Export**
- Implement CSV/JSON export
- Add scheduling for periodic exports
- Integrate with monitoring tools (Prometheus, etc.)

### Environment Variables

Set before running:

```bash
# Increase Java heap size
export JAVA_OPTS="-Xmx2g -Xms1g"

# Enable debugging
export DEBUG=true

# Custom database path
export DB_PATH="/var/lib/monitor/monitor.db"

# Then run application
mvn exec:java@run-server
```

---

## Database Management

### Database Structure

**Location:** `monitor.db` (SQLite)

**Auto-created tables** (via DatabaseManager):
- metrics_history - Historical metrics records
- process_snapshots - Process information archives
- client_metadata - Client information

### Accessing Database

**Using SQLite CLI**
```bash
# Install SQLite
sudo apt-get install sqlite3

# Open database
sqlite3 monitor.db

# Common queries
.tables                          # List tables
SELECT * FROM metrics_history;   # View metrics
SELECT COUNT(*) FROM metrics_history;  # Count records
.schema metrics_history          # View table structure
```

**Backup Database**
```bash
# Simple backup
cp monitor.db monitor.db.backup.$(date +%s)

# Automated backup (cron job)
0 2 * * * cp /path/to/monitor.db /backups/monitor.db.$(date +\%Y\%m\%d)
```

**Clear Database**
```bash
# WARNING: This deletes all metrics!
rm monitor.db

# Or in SQLite
sqlite3 monitor.db
DELETE FROM metrics_history;
DELETE FROM process_snapshots;
.quit
```

### Database Optimization

**Vacuum Database** (reclaim space)
```bash
sqlite3 monitor.db "VACUUM;"
```

**Add Indexes** (improve query speed)
```bash
sqlite3 monitor.db "CREATE INDEX idx_metrics_timestamp ON metrics_history(timestamp);"
```

**Archive Old Data**
```bash
# Move old records to archive
sqlite3 monitor.db "
INSERT INTO metrics_archive SELECT * FROM metrics_history 
WHERE timestamp < datetime('now', '-30 days');
DELETE FROM metrics_history 
WHERE timestamp < datetime('now', '-30 days');
"
```

---

## Deployment Guide

### Single Machine Setup

For testing/development:

```bash
# 1. Build
mvn clean install

# 2. Start server in background
nohup mvn exec:java@run-server > server.log 2>&1 &

# 3. Start client in background
nohup mvn exec:java@run-client > client.log 2>&1 &

# 4. Check logs
tail -f server.log
tail -f client.log

# 5. Access dashboard
# GUI: Server window
# Web: http://localhost:8080/dashboard
```

### Multi-Machine Deployment

**Server Machine:**
```bash
# Start server in headless mode (permanent)
nohup mvn exec:java@run-server -- --headless > /var/log/monitor-server.log 2>&1 &

# Add to crontab for auto-restart
@reboot cd /path/to/project && nohup mvn exec:java@run-server -- --headless > /var/log/monitor-server.log 2>&1 &
```

**Client Machines:**
```bash
# On each client machine
SERVER_IP="192.168.1.100"  # Your server IP
nohup mvn exec:java@run-client -- $SERVER_IP 5000 > /var/log/monitor-client.log 2>&1 &

# Or use systemd service (see below)
```

### Systemd Service (Linux)

**Create server service** (`/etc/systemd/system/monitor-server.service`):
```ini
[Unit]
Description=Remote Monitor Server
After=network.target

[Service]
Type=simple
User=monitor
WorkingDirectory=/opt/monitor
ExecStart=/usr/bin/mvn exec:java@run-server -- --tcp=5000 --http=8080 --headless
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

**Create client service** (`/etc/systemd/system/monitor-client.service`):
```ini
[Unit]
Description=Remote Monitor Client
After=network.target

[Service]
Type=simple
User=monitor
WorkingDirectory=/opt/monitor
ExecStart=/usr/bin/mvn exec:java@run-client -- 192.168.1.100 5000
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

**Enable services:**
```bash
sudo systemctl daemon-reload
sudo systemctl enable monitor-server
sudo systemctl enable monitor-client
sudo systemctl start monitor-server
sudo systemctl start monitor-client

# Monitor
sudo systemctl status monitor-server
journalctl -u monitor-server -f
```

### Docker Deployment

**Create Dockerfile:**
```dockerfile
FROM openjdk:21-slim
RUN apt-get update && apt-get install -y maven
WORKDIR /app
COPY . .
RUN mvn clean install

# For server
EXPOSE 5000 8080
CMD ["mvn", "exec:java@run-server", "--", "--headless"]

# For client - override CMD
# CMD ["mvn", "exec:java@run-client", "--", "server", "5000"]
```

**Build and run:**
```bash
# Build image
docker build -t monitor:latest .

# Run server
docker run -d -p 5000:5000 -p 8080:8080 --name monitor-server monitor:latest

# Run client
docker run -d --name monitor-client monitor:latest mvn exec:java@run-client -- monitor-server 5000
```

---

## Troubleshooting

### Common Issues & Solutions

**Issue 1: "Port already in use"**
```bash
# Check what's using the port
lsof -i :5000
netstat -tuln | grep 5000

# Kill existing process
kill -9 <PID>

# Or use different port
mvn exec:java@run-server -- --tcp=5001 --http=8081
```

**Issue 2: "Cannot connect to server"**
```bash
# 1. Verify server is running
ps aux | grep ServerApp

# 2. Check server is listening
netstat -tuln | grep 5000

# 3. Check firewall
sudo iptables -L | grep 5000

# 4. Add firewall rule (if needed)
sudo ufw allow 5000/tcp
```

**Issue 3: "Client connecting but not sending data"**
```bash
# 1. Check logs
tail -f client.log

# 2. Verify network connectivity
ping <server-ip>

# 3. Check port accessibility
telnet <server-ip> 5000

# 4. Verify server is accepting connections
tail -f server.log | grep "client"
```

**Issue 4: "Database locked"**
```bash
# 1. Check for multiple server instances
ps aux | grep ServerApp | grep -v grep | wc -l

# 2. Force unlock (dangerous - data loss risk)
rm monitor.db

# 3. Find lock file
lsof | grep monitor.db

# 4. Kill process holding lock
kill -9 <PID>
```

**Issue 5: "GUI not loading"**
```bash
# 1. Check display
echo $DISPLAY

# 2. Use headless mode instead
mvn exec:java@run-server -- --headless

# 3. Forward X11
ssh -X user@server
mvn exec:java@run-server
```

**Issue 6: "Out of Memory"**
```bash
# Increase heap size
export JAVA_OPTS="-Xmx4g -Xms2g"
mvn exec:java@run-server

# Or modify pom.xml
<argLine>-Xmx4g -Xms2g</argLine>
```

### Debug Logs

**Enable Debug Logging:**

In `ServerApp.java` or `ClientApp.java`:
```java
// Add debug prints
System.out.println("[DEBUG] " + message);

// Or use proper logging framework
Logger.debug("Message");
```

**View Logs:**
```bash
# Server logs (console)
tail -f /dev/stdout | grep ServerApp

# Client logs
tail -f client.log

# Combined
tail -f server.log client.log
```

---

## Development Workflow

### Setting Up Development Environment

**IDE Setup (IntelliJ IDEA)**
1. Open project: `File > Open > /path/to/project`
2. Maven automatically detected
3. Right-click `pom.xml` > "Run Maven Goal" > `clean install`
4. Create run configurations:
   - Server: Main class = `com.monitor.server.ServerApp`, VM args = `-Xmx1g`
   - Client: Main class = `com.monitor.client.ClientApp`, Arguments = `localhost 5000`

**IDE Setup (VS Code)**
1. Install Java Extension Pack
2. Open project folder
3. Maven explorer auto-detects pom.xml
4. Debug using CodeLens or Debug menu

### Local Development Testing

```bash
# Terminal 1: Start server with debug info
mvn exec:java@run-server -Ddebug=true

# Terminal 2: Start client
mvn exec:java@run-client

# Terminal 3: Monitor with curl
while true; do
  curl -s http://localhost:8080/metrics | jq '.'
  sleep 5
done

# Terminal 4: Watch database changes
watch -n 1 'sqlite3 monitor.db "SELECT COUNT(*) FROM metrics_history;"'
```

### Adding New Features

**Example: Add Temperature Monitoring**

1. Add to `SystemMetrics.java`:
```java
private double cpuTemperature;

public double getCpuTemperature() { return cpuTemperature; }
public void setCpuTemperature(double temp) { this.cpuTemperature = temp; }
```

2. Add to `MetricsCollector.java`:
```java
public static double getCpuTemperature() {
    // Read from /sys/class/thermal/thermal_zone0/temp (Linux)
    // Or use platform MBeans (cross-platform)
    // Implementation here
}
```

3. Update `ClientApp.java` metrics collection:
```java
SystemMetrics metrics = MetricsCollector.collectMetrics();
metrics.setCpuTemperature(MetricsCollector.getCpuTemperature());
```

4. Update dashboards to display temperature

### Testing Approach

**Manual Testing:**
1. Start server and clients
2. Observe metrics in GUI
3. Check web dashboard
4. Verify database persistence
5. Test graceful shutdown

**Automated Testing:**
```bash
# Create simple test script
#!/bin/bash
echo "Test 1: Start server..."
mvn exec:java@run-server &
SERVER_PID=$!

sleep 2

echo "Test 2: Connect client..."
mvn exec:java@run-client &
CLIENT_PID=$!

sleep 10

echo "Test 3: Check database..."
sqlite3 monitor.db "SELECT COUNT(*) FROM metrics_history;" | grep -q "[0-9]" && echo "PASS" || echo "FAIL"

kill $CLIENT_PID $SERVER_PID
```

### Version Control Workflow

```bash
# 1. Create feature branch
git checkout -b feature/temperature-monitoring

# 2. Make changes
vim src/main/java/com/monitor/shared/SystemMetrics.java
# ... more changes

# 3. Test locally
mvn clean install

# 4. Commit
git add .
git commit -m "Add temperature monitoring"

# 5. Push
git push origin feature/temperature-monitoring

# 6. Create pull request
```

---

## Summary Checklist

### Before First Run
- [ ] Java 21 installed
- [ ] Maven installed
- [ ] Project directory accessible
- [ ] No process using port 5000 or 8080

### Building
- [ ] Run `mvn clean install`
- [ ] No compilation errors
- [ ] Build artifacts in `target/`

### Running
- [ ] Start server: `mvn exec:java@run-server`
- [ ] Start client: `mvn exec:java@run-client`
- [ ] Check GUI login
- [ ] Verify web dashboard loads

### Monitoring
- [ ] Client logs show metrics sent
- [ ] Server shows client connected
- [ ] Database has records
- [ ] Dashboards updating

### Maintenance
- [ ] Regular backups of monitor.db
- [ ] Check server uptime
- [ ] Monitor disk usage
- [ ] Archive old metrics periodically

---

## Quick Reference Commands

```bash
# Build
mvn clean install

# Start server
mvn exec:java@run-server

# Start client  
mvn exec:java@run-client

# Access dashboard
http://localhost:8080/dashboard

# Check process
ps aux | grep java

# View logs
tail -f client.log
tail -f server.log

# Database query
sqlite3 monitor.db "SELECT * FROM metrics_history LIMIT 10;"

# Backup
cp monitor.db monitor.db.backup

# Stop (press Ctrl+C)
# Or kill gracefully
kill -TERM <PID>
```

---

## Additional Resources

- Java 21 Documentation: https://docs.oracle.com/en/java/javase/21/
- Maven Guide: https://maven.apache.org/guides/
- SQLite Documentation: https://www.sqlite.org/docs.html
- Swing Tutorial: https://docs.oracle.com/javase/tutorial/uiswing/
- JFreeChart: https://www.jfree.org/jfreechart/

---

**Document Version:** 1.0  
**Last Updated:** 2026-05-22  
**Maintained By:** Development Team
