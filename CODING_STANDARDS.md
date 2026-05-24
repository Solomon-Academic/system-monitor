# Coding Standards & Best Practices

This document outlines the Java best practices and coding standards implemented throughout the Remote System Performance Monitor project.

## Code Organization & Structure

### Package Organization
- **`com.monitor.shared`**: Shared data models (SystemMetrics, ProcessInfo)
- **`com.monitor.client`**: Client-side components (ClientApp, MetricsCollector, ClientLogger)
- **`com.monitor.server`**: Server-side main components (ServerApp, ClientHandler)
- **`com.monitor.server.db`**: Database layer (DatabaseManager)
- **`com.monitor.server.web`**: Web components (HttpDashboardServer)
- **`com.monitor.server.ui`**: GUI components (DashboardFrame, LoginDialog)

**Rationale**: Logical separation of concerns makes the codebase maintainable and scalable.

---

## Naming Conventions

### Classes
- **PascalCase** with meaningful names (e.g., `ClientHandler`, `DatabaseManager`)
- Nouns that describe the entity (e.g., `SystemMetrics`, `ProcessInfo`)
- Avoid abbreviations except for well-known terms (e.g., `HttpDashboardServer`, not `HDS`)

### Methods
- **camelCase** starting with verb (e.g., `collectMetrics()`, `handleClientDisconnection()`)
- Clear intent: `get*()`, `set*()`, `is*()`, `handle*()` conventions
- One method = one responsibility

### Variables
- **camelCase** for local and instance variables (e.g., `activeRegistry`, `metricsCollector`)
- **UPPERCASE_WITH_UNDERSCORES** for constants (e.g., `DEFAULT_SERVER_PORT`, `CONNECTION_TIMEOUT`)
- Descriptive names: `serverPort` not `port`, `connectionTimeout` not `timeout`

### Constants
- Defined at class level as `private static final`
- Grouped by functionality with comments
- Immutable and thread-safe

---

## Object-Oriented Design Principles

### Single Responsibility Principle (SRP)
Each class has ONE reason to change:
- `MetricsCollector` → Only collects metrics
- `DatabaseManager` → Only handles database operations
- `ClientHandler` → Only handles a single client connection

### Dependency Injection
- Components receive dependencies through constructor
- Example: `ClientHandler(Socket socket, ConcurrentHashMap<String, SystemMetrics> activeRegistry)`
- Promotes testability and loose coupling

### Immutability
- Use `private final` for immutable fields
- Defensive copying where needed
- No unnecessary setters

---

## Comments & Documentation

### Class Documentation
```java
/**
 * Brief description of class purpose (one line).
 * Longer explanation if needed.
 */
public class ClassName {
```

### Method Documentation
```java
/**
 * What this method does (action verb).
 * @param paramName Description of parameter
 * @return Description of return value
 * @throws ExceptionType When/why this exception is thrown
 */
public ReturnType methodName(String paramName) throws ExceptionType {
```

### Inline Comments
- Only for non-obvious WHY, not WHAT
- Code should be self-explanatory through naming
- Example: "Reconnect if socket is closed" (explains the why)

### No Javadoc for Getters/Setters
- Self-explanatory through naming conventions
- Reduces code clutter

---

## Error Handling

### Exception Strategy
1. **Fail-Fast**: Detect problems early
2. **Specific Exceptions**: Catch specific exceptions, not generic `Exception`
3. **Meaningful Messages**: Include context in error messages
4. **Logging**: Always log errors for debugging

### Pattern: Try-with-Resources
```java
try (Connection conn = getConnection(); 
     PreparedStatement pstmt = conn.prepareStatement(query)) {
    // Code here
} catch (SQLException e) {
    // Handle error
}
// Automatic resource cleanup
```

### Graceful Degradation
- Client reconnects on connection failure
- Server continues operating if one client disconnects
- Web dashboard returns JSON even if GUI fails

---

## Concurrency & Thread Safety

### Thread-Safe Collections
- `ConcurrentHashMap` for shared metrics registry
- Avoids synchronization bottlenecks
- Lock-free reads for better performance

### Immutable Data Transfer
- `SystemMetrics` objects created fresh, not modified in-place
- Reduces threading issues

### Daemon Threads
- Background threads marked as daemon
- Graceful shutdown doesn't hang on daemon threads
- Main threads prevent application exit

### Shutdown Hooks
```java
Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
```
- Cleanup on application exit (Ctrl+C)
- Close connections, save data, release resources

---

## Database Best Practices

### Prepared Statements
- Prevents SQL injection
- Better performance through query caching
- Example: `PreparedStatement pstmt = conn.prepareStatement(query);`

### Connection Pooling
- Reuse connections via `DriverManager` (lightweight)
- Each operation gets fresh connection for thread-safety

### Transactions
- Auto-commit for each statement (appropriate for this use case)
- Can extend for complex operations requiring atomicity

### Indexes
- Created for frequently queried columns (client_id, timestamp)
- Improves historical data queries

---

## Constants & Configuration

### Port Configuration
```java
private static final int DEFAULT_TCP_PORT = 5000;
private static final int DEFAULT_HTTP_PORT = 8080;
```
- Centralized configuration
- Easy to modify without hunting through code

### Timeouts
```java
private static final int CONNECTION_TIMEOUT = 3000; // milliseconds
private static final int RECONNECTION_DELAY = 5;     // seconds
```
- Named constants prevent magic numbers
- Clear units in variable name or comment

### Collection Limits
```java
processes.stream()
    .sorted((a, b) -> Long.compare(b.getMemoryUsage(), a.getMemoryUsage()))
    .limit(20)
    .forEachOrdered(p -> p);
```
- Prevent data explosion
- Documented in code

---

## JSON Serialization

### Jackson Annotations
```java
@JsonProperty("cpuUsage")
private double cpuUsage;
```
- Explicit mapping prevents refactoring issues
- Clear contract between Java and JSON

### Serialization Format
- Snake_case in JSON (database convention)
- camelCase in Java (Java convention)
- Automatic conversion via Jackson

---

## Logging Strategy

### Log Levels
- **DEBUG**: Detailed flow information (metrics sent, connections)
- **INFO**: Important events (client connected, server started)
- **WARN**: Recoverable issues (failed connection attempt)
- **ERROR**: Critical issues (database failed, connection timeout)

### Log Format
```
[TIMESTAMP] [LEVEL] Message
[2026-05-24 17:30:15] [INFO] Client connected from 192.168.1.100
```

### File & Console Logging
- Client logs to file (`client.log`)
- Server logs to console
- Both include timestamps for correlation

---

## Performance Considerations

### Efficient Metrics Collection
- Uses JMX (Java Management Extensions) for CPU/RAM
- Cross-platform compatibility
- Minimal overhead

### Limiting Process Information
- Top 20 processes by memory (prevents data explosion)
- Sorted for easy identification of resource hogs

### Connection Pooling
- Thread pool for concurrent client handling
- `ScheduledExecutorService` for metrics collection
- Prevents thread explosion

### Web Dashboard Efficiency
- Auto-refresh via JavaScript polling (5 seconds)
- Lightweight HTTP responses (JSON)
- No unnecessary data transmission

---

## Testing Recommendations

### Unit Testing
- Test `MetricsCollector` independently with mock OS data
- Test `DatabaseManager` with in-memory SQLite
- Test `SystemMetrics` JSON serialization

### Integration Testing
- Start server and client, verify data flow
- Check database persistence
- Verify reconnection logic

### Manual Testing
- Monitor with `ps aux` for resource usage
- Check `netstat` for port listening
- Query database with `sqlite3` to verify data

---

## Security Considerations

### Authentication
- Simple hardcoded credentials for demo (`admin/admin`)
- In production: Use OAuth, LDAP, or enterprise auth
- Password field uses `JPasswordField` (secure char array)

### Network Security
- Plain text TCP (can be enhanced with SSL/TLS)
- In production: Implement certificate-based authentication

### Data Protection
- No sensitive data in logs
- Database stored locally (no remote exposure)
- Web dashboard runs on localhost by default

---

## Dependencies Management

### Minimal Dependencies
- Only essential libraries included
- No transitive dependency bloat
- Clear version pinning in `pom.xml`

### Maven Configuration
- Centralized version management
- Build plugins for compilation and execution
- Exec plugin for easy running

---

## Code Quality Metrics

| Metric | Target | Implementation |
|--------|--------|-----------------|
| Cyclomatic Complexity | < 10 | Single-purpose methods |
| Lines per Method | < 30 | Focused implementations |
| Comment-to-Code Ratio | 1:4 | Only necessary comments |
| Test Coverage | > 80% | Unit and integration tests |

---

## Deployment Best Practices

### Build Pipeline
1. `mvn clean` - Remove old artifacts
2. `mvn compile` - Compile source
3. `mvn verify` - Run tests
4. `mvn package` - Create JAR

### Runtime Configuration
- Port customization via CLI arguments
- Headless mode for servers
- Graceful shutdown via signals

### Monitoring
- Log file for debugging
- Database queries for historical analysis
- Web dashboard for real-time monitoring

---

## Future Enhancements

### Scalability
- Implement metrics aggregation service
- Add cluster support for high availability
- Implement data warehousing for long-term storage

### Security
- Add TLS/SSL encryption for network communication
- Implement OAuth 2.0 authentication
- Add role-based access control (RBAC)

### Observability
- Structured logging (JSON format)
- Metrics export to Prometheus/Grafana
- Distributed tracing support

---

## References

- [Java Code Conventions](https://www.oracle.com/java/technologies/javase/codeconventions-136091.html)
- [Effective Java](https://www.oreilly.com/library/view/effective-java-3rd/9780134685991/)
- [Clean Code](https://www.oreilly.com/library/view/clean-code-a/9780136083238/)
- [SOLID Principles](https://en.wikipedia.org/wiki/SOLID)

