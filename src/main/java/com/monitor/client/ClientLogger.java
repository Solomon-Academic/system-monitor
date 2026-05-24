package com.monitor.client;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple logging utility for client application.
 * Logs to both console and file (client.log).
 */
public class ClientLogger {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String LOG_FILE = "client.log";

    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    /**
     * Logs debug level message.
     */
    public static void debug(String message) {
        log(Level.DEBUG, message, null);
    }

    /**
     * Logs info level message.
     */
    public static void info(String message) {
        log(Level.INFO, message, null);
    }

    /**
     * Logs warning level message.
     */
    public static void warn(String message) {
        log(Level.WARN, message, null);
    }

    /**
     * Logs error level message with exception.
     */
    public static void error(String message, Throwable throwable) {
        log(Level.ERROR, message, throwable);
    }

    /**
     * Internal method to handle logging to console and file.
     */
    private static void log(Level level, String message, Throwable throwable) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logMessage = String.format("[%s] [%s] %s", timestamp, level, message);

        // Log to console
        System.out.println(logMessage);
        if (throwable != null) {
            throwable.printStackTrace();
        }

        // Log to file
        try (FileWriter fw = new FileWriter(LOG_FILE, true)) {
            fw.write(logMessage + "\n");
            if (throwable != null) {
                fw.write(throwable.toString() + "\n");
            }
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }
}
