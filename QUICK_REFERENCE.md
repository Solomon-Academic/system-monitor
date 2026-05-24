# Quick Reference Guide

Fast lookup for commonly needed information about the Remote System Performance Monitor project.

## Quick Commands

### Build
```bash
mvn clean install
```

### Run Server
```bash
mvn exec:java@run-server                              # With GUI
mvn exec:java@run-server -- --headless                # Headless mode
mvn exec:java@run-server -- --tcp=6000 --http=9000   # Custom ports
```

### Run Client
```bash
mvn exec:java@run-client                              # Local server
mvn exec:java@run-client -- 192.168.1.100 5000       # Remote server
```

### Access Dashboards
```
Web Dashboard: http://localhost:8080/dashboard
Web Metrics API: http://localhost:8080/metrics
GUI Dashboard: Login with admin/admin
```

### Database Queries
```bash
# Connect
sqlite3 monitor.db

# Count metrics
SELECT COUNT(*) FROM metrics_history;

# View latest metrics
SELECT * FROM metrics_history ORDER BY timestamp DESC LIMIT 5;

# View connected clients
SELECT DISTINCT client_id, hostname FROM client_metadata;

# Export to CSV
.mode csv
.output metrics.csv
SELECT * FROM metrics_history;
.output stdout
```

---

## File Structure Quick Reference

```
Key Files:
├── pom.xml                    → Maven configuration + dependencies
├── README.md                  → Quick start (read first!)
├── CODING_STANDARDS.md        → Best practices reference
├── IMPLEMENTATION_SUMMARY.md  → What was built + presentation guide
├── PROJECT_GUIDE.md           → Comprehensive documentation
└── QUICK_REFERENCE.md         → This file

Java Files:
├── com.monitor.client.ClientApp         → Main client entry point
├── com.monitor.client.MetricsCollector  → System metrics gathering
├── com.monitor.server.ServerApp         → Main server entry point
├── com.monitor.server.ClientHandler     → Per-client connection handler
├── com.monitor.server.db.DatabaseManager→ SQLite operations
├── com.monitor.server.web.HttpDashboardServer → Web API
├── com.monitor.server.ui.DashboardFrame → Swing GUI
├── com.monitor.server.ui.LoginDialog    → Login authentication
├── com.monitor.shared.SystemMetrics     → Metrics data model
└── com.monitor.shared.ProcessInfo       → Process data model
```

---

## Configuration

### Server Ports
| Service | Default | Environment | Override |
|---------|---------|-------------|----------|
| TCP Listener | 5000 | `MONITOR_TCP_PORT` | `--tcp=PORT` |
| HTTP Dashboard | 8080 | `MONITOR_HTTP_PORT` | `--http=PORT` |

### Client Configuration
| Setting | Default | Override |
|---------|---------|----------|
| Server Host | localhost | Arg 1: `mvn exec:java@run-client -- HOST` |
| Server Port | 5000 | Arg 2: `mvn exec:java@run-client -- HOST PORT` |
| Metrics Interval | 5 seconds | Edit ClientApp.java line ~45 |
| Connection Timeout | 3 seconds | Edit ClientApp.java line ~25 |

### Database
| Setting | Value |
|---------|-------|
| File | `monitor.db` (SQLite) |
| Location | Project root directory |
| Backup | `cp monitor.db monitor.db.backup` |
| Clear | `rm monitor.db` (will recreate on next run) |

---

## Authentication

### GUI Login
```
Default Credentials:
Username: admin
Password: admin
```

**To change**: Edit `LoginDialog.java` line ~50, method `handleLogin()`

---

## Troubleshooting

### Port Already in Use
```bash
# Check what's using port 5000
lsof -i :5000
netstat -tuln | grep 5000

# Kill process using port
kill -9 <PID>

# Use different port
mvn exec:java@run-server -- --tcp=5001 --http=8081
```

### Client Can't Connect
```bash
# Verify server is running
ps aux | grep ServerApp

# Check firewall
sudo ufw allow 5000/tcp

# Test connection
telnet localhost 5000
```

### Database Locked
```bash
# Check lock
lsof | grep monitor.db

# Close hanging process
kill -9 <PID>

# Reset database
rm monitor.db
```

### GUI Not Loading
```bash
# Use headless mode instead
mvn exec:java@run-server -- --headless

# Check DISPLAY variable
echo $DISPLAY

# Forward X11 (if remote)
ssh -X user@server
mvn exec:java@run-server
```

---

## Code Comments Guide

### Well-Commented Examples

**Class Documentation**:
```java
/**
 * Handles individual client connections on the server.
 * Runs in separate thread for each connected client.
 * Reads incoming metrics, persists to database, and updates active registry.
 */
public class ClientHandler implements Runnable {
```

**Method Documentation**:
```java
/**
 * Saves metrics snapshot to database.
 * @param metrics SystemMetrics object to persist
 * @throws SQLException if database operation fails
 */
public static void saveMetrics(SystemMetrics metrics) throws SQLException {
```

**Inline Comments** (rare, only when WHY is non-obvious):
```java
// Reconnect if socket is closed (handles network interruptions)
if (socket == null || socket.isClosed()) {
    connectToServer();
}
```

### What NOT to Comment
```java
// BAD - comment just repeats code
int count = query.count();  // Get the count

// GOOD - no comment needed, method name is clear
String hostname = getHostname();
```

---

## Performance Tips

### Optimize for Scale
1. **Increase Thread Pool**: `Executors.newFixedThreadPool(100)` for 100+ clients
2. **Database Indexing**: Already implemented on client_id + timestamp
3. **Archive Old Data**: Delete metrics older than 30 days
4. **Batch Inserts**: Modify DatabaseManager for bulk operations

### Monitor Performance
```bash
# Check CPU/Memory
top -b -n 1 | head -20

# Check database size
ls -lh monitor.db

# Count database records
sqlite3 monitor.db "SELECT COUNT(*) FROM metrics_history;"

# Check network connections
netstat -tuln | grep 5000
```

---

## Best Practices in Code

### Thread Safety
- ✅ `ConcurrentHashMap` for shared data
- ✅ Immutable data objects
- ✅ Try-with-resources for connection cleanup
- ❌ Never use `synchronized HashMap`

### Error Handling
- ✅ Specific exceptions (not generic `Exception`)
- ✅ Meaningful error messages with context
- ✅ Always log exceptions
- ✅ Graceful degradation (keep running if possible)
- ❌ Never ignore exceptions silently

### Security
- ✅ Prepared statements (SQL injection prevention)
- ✅ Password field as char array (not String)
- ✅ Input validation
- ❌ Never hardcode credentials in production

### Performance
- ✅ Connection pooling
- ✅ Indexed database queries
- ✅ Batch operations
- ✅ Limit data: top 20 processes
- ❌ Never full table scans without index

---

## Development Workflow

### Making Changes
1. Edit Java file
2. Run: `mvn compile` to check for errors
3. Test locally: Start server and client
4. Verify: Check GUI/web dashboard and database
5. Commit: `git add . && git commit -m "message"`

### Adding New Feature Example
**Goal**: Add disk usage monitoring

1. Add field to `SystemMetrics.java`:
   ```java
   @JsonProperty("diskUsage")
   private double diskUsage;
   ```

2. Add getter/setter

3. Add collection to `MetricsCollector.java`:
   ```java
   metrics.setDiskUsage(getDiskUsage());
   ```

4. Add method:
   ```java
   private static double getDiskUsage() {
       File root = new File("/");
       return ((double) (root.getTotalSpace() - root.getFreeSpace()) 
               / root.getTotalSpace()) * 100;
   }
   ```

5. Update dashboard display in `DashboardFrame.java`

6. Build and test: `mvn clean compile`

---

## Presentation Talking Points

### Architecture
- "Distributed monitoring using client-server architecture"
- "TCP sockets for reliable metrics transmission"
- "SQLite for persistent historical data"
- "Dual dashboards: GUI for admins, web for remote access"

### Code Quality
- "Every class and method is documented"
- "Thread-safe concurrent data structures prevent race conditions"
- "Prepared statements prevent SQL injection"
- "Try-with-resources ensure proper resource cleanup"

### Scalability
- "Thread pool handles concurrent client connections"
- "Can scale to hundreds of clients with proper configuration"
- "Database indexes enable fast historical queries"

### Best Practices
- "SOLID principles: Single Responsibility, Open/Closed, etc."
- "Design patterns: Producer-Consumer, Registry, Scheduler"
- "Proper error handling with meaningful messages"
- "Graceful shutdown saves data and closes connections"

---

## Emergency Procedures

### Hard Reset
```bash
# Kill everything
pkill -f "java.*ServerApp"
pkill -f "java.*ClientApp"

# Reset database
rm monitor.db

# Check all cleaned up
ps aux | grep java
lsof | grep 5000
```

### Restart Server Cleanly
```bash
# Stop server (Ctrl+C)
# Check no processes remain
ps aux | grep java

# Start fresh
mvn clean install
mvn exec:java@run-server
```

### Export Data Before Reset
```bash
sqlite3 monitor.db
.mode csv
.output metrics_backup.csv
SELECT * FROM metrics_history;
.output stdout
.quit
```

---

## Useful Links

- Java 21 Docs: https://docs.oracle.com/en/java/javase/21/
- SQLite: https://www.sqlite.org/docs.html
- Maven: https://maven.apache.org/guides/
- Jackson (JSON): https://github.com/FasterXML/jackson
- FlatLaf (UI): https://www.formdev.com/flatlaf/

---

**Last Updated**: 2026-05-24
**Version**: 1.0.0

