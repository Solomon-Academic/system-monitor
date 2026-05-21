# Remote System Performance Monitor

A distributed system monitoring application that collects real-time system metrics from multiple client machines and aggregates them in a centralized server with both GUI and web-based dashboards.

## Overview

This Java application consists of three main components:
- **Server**: Central hub that receives metrics from clients, stores them in a database, and provides visualization through GUI and web dashboards
- **Client Agents**: Lightweight agents deployed on remote systems that periodically collect and transmit system metrics
- **Dashboards**: Both GUI (Swing) and web-based interfaces to view real-time and historical metrics

## Key Features

- **Real-time Metrics Collection**: CPU usage, RAM consumption, running processes
- **Multi-client Support**: Monitor multiple remote systems from a single server
- **Persistent Storage**: SQLite database for historical metric data
- **Dual Dashboards**: 
  - GUI Dashboard with charts and graphs (FlatLaf theme, JFreeChart)
  - Web Dashboard accessible via HTTP
- **Authentication**: Login dialog for secure access to the server dashboard
- **Configurable Ports**: Customize TCP and HTTP ports as needed
- **Headless Mode**: Run server without GUI in server environments

## System Requirements

- Java 21 or later
- Maven 3.6+
- SQLite (included via JDBC driver)

## Quick Start

### Build the Project

```bash
mvn clean install
```

### Start the Server

```bash
# Standard (with GUI dashboard)
mvn exec:java@run-server

# Headless mode (no GUI)
mvn exec:java@run-server -- --headless

# Custom TCP and HTTP ports
mvn exec:java@run-server -- --tcp=5000 --http=8080
```

### Start Client Agent(s)

```bash
# Connect to local server
mvn exec:java@run-client

# Connect to remote server
mvn exec:java@run-client -- <server-host> <server-port>
```

## Project Structure

```
project/
├── pom.xml                          # Maven configuration
├── src/main/java/com/monitor/
│   ├── client/                      # Client agent components
│   │   ├── ClientApp.java           # Main client application
│   │   ├── MetricsCollector.java    # System metrics gathering
│   │   └── ClientLogger.java        # Client-side logging
│   ├── server/                      # Server components
│   │   ├── ServerApp.java           # Main server application
│   │   ├── ClientHandler.java       # Handles client connections
│   │   ├── db/
│   │   │   └── DatabaseManager.java # SQLite database operations
│   │   ├── web/
│   │   │   └── HttpDashboardServer.java # HTTP web server
│   │   └── ui/
│   │       ├── LoginDialog.java     # Authentication UI
│   │       └── DashboardFrame.java  # Swing GUI dashboard
│   └── shared/                      # Shared data classes
│       ├── SystemMetrics.java       # Metrics data model
│       └── ProcessInfo.java         # Process information model
├── target/                          # Build output
├── monitor.db                       # SQLite database (auto-created)
└── client.log                       # Client logs
```

## Architecture

### Server Architecture
1. **TCP Socket Server** (Port 5000): Accepts connections from client agents
2. **HTTP Server** (Port 8080): Serves web dashboard and handles HTTP requests
3. **Database Layer**: SQLite for metrics persistence
4. **Active Registry**: In-memory map of connected clients and their latest metrics
5. **Swing GUI**: Administrative dashboard with authentication

### Client Architecture
1. **Metrics Collector**: Gathers system information (CPU, RAM, processes)
2. **Scheduler**: Runs metrics collection every 5 seconds
3. **Socket Client**: Transmits JSON-encoded metrics to server
4. **Auto-reconnect**: Automatically reconnects if connection drops

### Communication Protocol
- **Format**: JSON over TCP sockets
- **Frequency**: Every 5 seconds per client
- **Data**: System metrics payload serialized via Jackson

## Configuration

### Server Configuration
Edit `ServerApp.java` main method or pass command-line arguments:
- `--tcp=<port>`: TCP listener port (default: 5000)
- `--http=<port>`: HTTP server port (default: 8080)
- `--headless`: Run without GUI (useful for servers)

### Client Configuration
Edit `ClientApp.java` main method or pass arguments:
- `<server-host>`: Target server hostname/IP (default: localhost)
- `<server-port>`: Target server port (default: 5000)
- Metrics collection interval: Fixed at 5 seconds (configurable in code)

## Development

### Build Commands
```bash
mvn clean                          # Clean build artifacts
mvn compile                        # Compile source
mvn test                          # Run tests (if configured)
mvn package                       # Create JAR/WAR
```

### Running Individual Components

**Server**:
```bash
mvn exec:java@run-server -- --headless
```

**Client**:
```bash
mvn exec:java@run-client -- localhost 5000
```

## Dependencies

- **SQLite JDBC** (3.45.1.0): Database driver
- **FlatLaf** (3.3): Modern Swing look and feel
- **JFreeChart** (1.5.3): Charting library for graphs
- **Jackson** (2.16.1): JSON serialization/deserialization

## Data Storage

- **monitor.db**: SQLite database file (auto-created on first run)
- **client.log**: Client agent logs
- Server logs: Console output with timestamps

## Known Limitations

- Client agents connect with 3-second timeout
- Headless environment detection may not work on all systems
- GUI requires display environment (Swing limitation)
- Single database file per server instance

## Troubleshooting

### Client cannot connect to server
- Check server is running: `netstat -an | grep 5000`
- Verify firewall allows port 5000
- Check host/port configuration

### GUI not loading on headless server
- Use `--headless` flag or `DISPLAY` environment variable
- GUI requires X11 forwarding or local display

### Database locked
- Ensure only one server instance is running
- Check for stale processes: `lsof | grep monitor.db`

## License

Internal Use

## Contributors

Development Team
