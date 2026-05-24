# How to Run & Test the Project

## Prerequisites Check

Before starting, verify you have everything installed:

```bash
# Check Java version (need Java 21+)
java -version

# Check Maven version (need Maven 3.6+)
mvn -version

# Expected output:
# Apache Maven 3.x.x
# Maven home: /path/to/maven
# Java version: 21.x.x
```

---

## Part 1: Build the Project

### Step 1: Clean Build

```bash
cd /home/skymoon/Documents/java_Code/system-monitor

# Clean and build
mvn clean install
```

**Expected Output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXXs
```

### Step 2: Verify Build Artifacts

```bash
# Check if compilation succeeded
ls -la target/classes/com/monitor/*/

# Should show .class files for all components
```

---

## Part 2: Run the Application

### Option A: Run Everything Locally (Recommended for Testing)

**Terminal 1 - Start Server:**
```bash
cd /home/skymoon/Documents/java_Code/system-monitor
mvn exec:java@run-server
```

**Expected Output:**
```
[INFO] TCP Listener started on port 5000
[INFO] HTTP Dashboard Server started on port 8080
[INFO] Waiting for client agents to connect...
[GUI Window] Login dialog appears
```

> Login with: `admin` / `admin`

---

**Terminal 2 - Start First Client:**
```bash
cd /home/skymoon/Documents/java_Code/system-monitor
mvn exec:java@run-client
```

**Expected Output:**
```
[INFO] Starting client application...
[INFO] Connecting to server at localhost:5000
[INFO] Successfully connected to server at localhost:5000
[INFO] Collected metrics: CPU=XX.XX% RAM=YY.YY%
[INFO] Metrics snapshot sent to server
```

---

**Terminal 3 - Start Second Client (to test multi-client):**
```bash
cd /home/skymoon/Documents/java_Code/system-monitor
mvn exec:java@run-client
```

**Expected Output:** (Same as Terminal 2)
```
[INFO] Starting client application...
[INFO] Successfully connected to server at localhost:5000
[INFO] Collected metrics: CPU=XX.XX% RAM=YY.YY%
```

---

### Option B: Run Server Headless (No GUI)

If you don't want the GUI or are on a remote machine:

```bash
mvn exec:java@run-server -- --headless
```

**Expected Output:**
```
[INFO] TCP Listener started on port 5000
[INFO] HTTP Dashboard Server started on port 8080
[INFO] No display available, running in headless mode
[INFO] Waiting for client agents to connect...
```

---

### Option C: Custom Ports

If ports 5000 or 8080 are already in use:

```bash
# Server with custom ports
mvn exec:java@run-server -- --tcp=6000 --http=9000

# Client connecting to custom port
mvn exec:java@run-client -- localhost 6000
```

---

## Part 3: Access the Dashboards

### Web Dashboard (Auto-Updates Every 5 Seconds)

Open your browser and go to:
```
http://localhost:8080/dashboard
```

**You should see:**
- Connected clients listed
- Real-time CPU usage (%)
- Real-time RAM usage (%)
- RAM used/total in GB
- Last update timestamp
- Auto-refresh every 5 seconds

---

### GUI Dashboard

In the server terminal, a Swing window should appear:
- **Login:** Click the window or it appears on startup
- **Username:** `admin`
- **Password:** `admin`
- **View:** Real-time table with all connected clients and their metrics

---

### JSON API Endpoint

For debugging, check raw metrics JSON:

```bash
# In another terminal, view JSON API
curl http://localhost:8080/metrics

# Expected output:
# {
#   "client_xxxxxxxx": {
#     "clientId": "client_xxxxxxxx",
#     "hostname": "your-hostname",
#     "timestamp": 1234567890,
#     "cpuUsage": 25.50,
#     "ramUsage": 45.30,
#     ...
#   }
# }
```

---

## Part 4: Verify Database

### Check SQLite Database

```bash
# Install sqlite3 if needed
sudo apt-get install sqlite3  # Ubuntu/Debian
brew install sqlite3          # macOS

# Open database
sqlite3 monitor.db

# Inside sqlite3 shell:
.tables
# Output: client_metadata  metrics_history

# View table structure
.schema metrics_history

# Count total metrics records
SELECT COUNT(*) FROM metrics_history;

# View recent metrics
SELECT client_id, hostname, cpu_usage, ram_usage, timestamp 
FROM metrics_history 
ORDER BY timestamp DESC 
LIMIT 5;

# Exit
.quit
```

---

## Part 5: Complete Test Workflow

### Full Test Scenario (10 Minutes)

```bash
# STEP 1: Terminal 1 - Start Server
mvn exec:java@run-server

# Wait for: "Waiting for client agents to connect..."
# In GUI: Login with admin/admin

# STEP 2: Terminal 2 - Start Client 1
mvn exec:java@run-client

# Wait for: "Metrics snapshot sent to server" (repeating every 5 seconds)

# STEP 3: Terminal 3 - Start Client 2
mvn exec:java@run-client

# Wait for: Both clients sending metrics

# STEP 4: Terminal 4 - Open Web Dashboard
curl http://localhost:8080/dashboard
# OR open browser: http://localhost:8080/dashboard

# STEP 5: Terminal 5 - Check Database
sqlite3 monitor.db "SELECT COUNT(*) FROM metrics_history;"
# Should show increasing numbers as metrics accumulate

# STEP 6: Let it run for 30+ seconds
# Watch metrics accumulate in:
# - Server GUI
# - Web dashboard
# - Database

# STEP 7: Graceful Shutdown
# In each terminal, press: Ctrl+C
# Server: "Shutting down server..."
# Clients: "Shutting down client application..."
```

---

## Part 6: Troubleshooting

### Issue 1: "Port already in use"

```bash
# Find what's using port 5000
lsof -i :5000
netstat -tuln | grep 5000

# Kill the process
kill -9 <PID>

# Or use different port
mvn exec:java@run-server -- --tcp=6000 --http=9000
```

### Issue 2: "Cannot connect to server"

```bash
# Check if server is running
ps aux | grep java | grep ServerApp

# Check if port is listening
netstat -tuln | grep 5000

# Test connectivity
telnet localhost 5000
# If connected, press Ctrl+] then quit

# Check firewall
sudo ufw status
sudo ufw allow 5000/tcp
```

### Issue 3: "Database locked"

```bash
# Check if another process has the database
lsof | grep monitor.db

# Kill the process
kill -9 <PID>

# Or reset database
rm monitor.db
# It will be recreated on next run
```

### Issue 4: "GUI not appearing"

```bash
# Run in headless mode instead
mvn exec:java@run-server -- --headless

# Or set display
export DISPLAY=:0
mvn exec:java@run-server

# On Windows or Mac, GUI should appear automatically
```

### Issue 5: "No metrics in database"

```bash
# Check if clients are actually sending data
# Look for in client terminal:
# [INFO] Collected metrics: CPU=XX.XX% RAM=YY.YY%
# [INFO] Metrics snapshot sent to server

# If not seeing this, clients aren't connecting
# Check server terminal for:
# [INFO] Client connected from 127.0.0.1
```

---

## Part 7: Performance Testing

### Test with Multiple Clients

```bash
# Start server
mvn exec:java@run-server -- --headless &
SERVER_PID=$!

sleep 2

# Start 5 clients
for i in {1..5}; do
  mvn exec:java@run-client &
done

# Wait 30 seconds to accumulate data
sleep 30

# Check database
sqlite3 monitor.db "SELECT COUNT(*) FROM metrics_history;"

# Kill all processes
kill $SERVER_PID
pkill -f "java.*ServerApp"
pkill -f "java.*ClientApp"
```

---

## Part 8: Monitoring & Debugging

### Watch Metrics in Real-Time

```bash
# Continuously check database
watch -n 1 'sqlite3 monitor.db "SELECT COUNT(*) FROM metrics_history; SELECT * FROM metrics_history ORDER BY timestamp DESC LIMIT 1;"'

# Auto-refresh web dashboard
# Browser: http://localhost:8080/dashboard (already auto-refreshes every 5 seconds)

# Watch client logs
tail -f client.log
```

### Enable Debug Logging

The application already includes INFO, DEBUG, WARN, ERROR levels.

In client terminal, you'll see:
```
[INFO] Starting client application...
[DEBUG] Metrics snapshot sent to server
[INFO] Collected metrics: CPU=25.50% RAM=45.30%
```

---

## Part 9: Clean Shutdown Procedure

### Graceful Shutdown

```bash
# In each terminal running Java, press:
Ctrl+C

# You should see:
# [INFO] Shutting down server...
# [INFO] Server shutdown complete

# OR

# Kill gracefully from another terminal
kill -TERM <PID>
```

### Force Kill (Last Resort)

```bash
pkill -f "java.*ServerApp"
pkill -f "java.*ClientApp"

# Check if all Java processes are gone
ps aux | grep java
```

---

## Part 10: Cleanup & Reset

### Clean Up Database

```bash
# Back up current database
cp monitor.db monitor.db.backup.$(date +%s)

# Delete database (will be recreated)
rm monitor.db

# Start fresh
mvn exec:java@run-server
```

### Clean Up Logs

```bash
rm client.log
rm -rf logs/
```

### Full Clean Build

```bash
mvn clean
mvn clean install
```

---

## Part 11: Verify Files NOT Pushed to Git

After running the tests, check what files are created but ignored:

```bash
# Should exist locally but NOT be pushed
ls -la | grep -E "monitor.db|client.log|target"

# These should appear in git status as untracked
git status

# They should NOT appear when you do
git ls-files

# To confirm, check .gitignore
cat .gitignore
# Should contain: *.db, *.log, target/, etc.
```

---

## Part 12: Push to Git (When Ready)

```bash
# Check what will be committed
git status

# Should show only:
# - src/ (Java files)
# - pom.xml
# - .gitignore
# - *.md files
# - .git/

# NOT:
# - target/ (build artifacts)
# - *.db (database)
# - *.log (logs)
# - *.class (compiled classes)

# Add all (ignored files won't be added)
git add .

# Commit
git commit -m "Initial commit: Remote System Performance Monitor

- 11 Java source files with comprehensive documentation
- Maven build system with all dependencies
- Client agent for metrics collection
- Server with TCP listener, HTTP dashboard, and Swing GUI
- SQLite database for metrics persistence
- Complete documentation and best practices"

# Push
git push origin main
```

---

## Quick Reference Commands

```bash
# Build
mvn clean install

# Run server with GUI
mvn exec:java@run-server

# Run server headless
mvn exec:java@run-server -- --headless

# Run client (local)
mvn exec:java@run-client

# Run client (remote)
mvn exec:java@run-client -- 192.168.1.100 5000

# Access web dashboard
http://localhost:8080/dashboard

# Check database
sqlite3 monitor.db "SELECT COUNT(*) FROM metrics_history;"

# View logs
tail -f client.log

# Stop all Java processes
pkill -f java.*

# Git push (when ready)
git add . && git commit -m "message" && git push
```

---

## Expected Results After Testing

✅ Server starts without errors  
✅ Clients connect and send metrics  
✅ Web dashboard updates every 5 seconds  
✅ GUI shows connected clients  
✅ Database has metrics records  
✅ Graceful shutdown works  
✅ All ignored files are NOT in git  

---

**You're now ready to run, test, and deploy!** 🚀

