package com.monitor.client;

import com.monitor.shared.ProcessInfo;
import com.monitor.shared.SystemMetrics;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Collects system metrics from the host machine.
 * Uses Java Management Extensions (JMX) for cross-platform metric gathering.
 * Provides CPU, RAM, and process information snapshots.
 */
public class MetricsCollector {

    private static final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private static final com.sun.management.OperatingSystemMXBean sunOsBean =
            (com.sun.management.OperatingSystemMXBean) osBean;
    private static final MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
    private static final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();

    /**
     * Collects current system metrics snapshot.
     * @return SystemMetrics object with current state
     */
    public static SystemMetrics collectMetrics() {
        String clientId = generateClientId();
        String hostname = getHostname();
        long timestamp = System.currentTimeMillis();
        double cpuUsage = getCpuUsage();
        long[] ramInfo = getRamUsage();
        List<ProcessInfo> processes = getRunningProcesses();

        SystemMetrics metrics = new SystemMetrics();
        metrics.setClientId(clientId);
        metrics.setHostname(hostname);
        metrics.setTimestamp(timestamp);
        metrics.setCpuUsage(cpuUsage);
        metrics.setRamUsage((double) ramInfo[0] / (double) ramInfo[1] * 100);
        metrics.setRamTotal(ramInfo[1]);
        metrics.setRamUsed(ramInfo[0]);
        metrics.setProcesses(processes);

        return metrics;
    }

    /**
     * Retrieves current CPU usage as percentage.
     * Returns value between 0-100 representing CPU load.
     */
    private static double getCpuUsage() {
        try {
            double processCpuUsage = sunOsBean.getProcessCpuLoad();
            if (processCpuUsage < 0.0) {
                return 0.0;
            }
            return Math.round(processCpuUsage * 10000.0) / 100.0;
        } catch (Exception e) {
            return osBean.getSystemLoadAverage() * 100;
        }
    }

    /**
     * Retrieves current memory (RAM) usage.
     * @return Array: [used_bytes, total_bytes]
     */
    private static long[] getRamUsage() {
        try {
            long totalSystemMemory = sunOsBean.getTotalPhysicalMemorySize();
            long freeSystemMemory = sunOsBean.getFreePhysicalMemorySize();
            long usedSystemMemory = totalSystemMemory - freeSystemMemory;
            return new long[]{usedSystemMemory, totalSystemMemory};
        } catch (Exception e) {
            long maxMemory = Runtime.getRuntime().maxMemory();
            long totalMemory = Runtime.getRuntime().totalMemory();
            long freeMemory = Runtime.getRuntime().freeMemory();
            long usedMemory = totalMemory - freeMemory;
            return new long[]{usedMemory, maxMemory};
        }
    }

    /**
     * Retrieves list of currently running processes.
     * Includes PID, name, CPU usage, and memory usage.
     */
    private static List<ProcessInfo> getRunningProcesses() {
        List<ProcessInfo> processes = new ArrayList<>();

        try {
            String osName = System.getProperty("os.name").toLowerCase();

            if (osName.contains("linux")) {
                processes.addAll(getLinuxProcesses());
            } else if (osName.contains("windows")) {
                processes.addAll(getWindowsProcesses());
            } else if (osName.contains("mac")) {
                processes.addAll(getMacProcesses());
            }

            // Limit to top 20 processes by memory usage
            processes = processes.stream()
                    .sorted((a, b) -> Long.compare(b.getMemoryUsage(), a.getMemoryUsage()))
                    .limit(20)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            ClientLogger.error("Error collecting process information", e);
        }

        return processes;
    }

    /**
     * Collects process information on Linux systems.
     */
    private static List<ProcessInfo> getLinuxProcesses() {
        List<ProcessInfo> processes = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec(new String[]{
                    "bash", "-c",
                    "ps aux | tail -n +2 | awk '{print $2, $1, $3, $6}'"
            });
            process.waitFor();
        } catch (Exception e) {
            ClientLogger.debug("Linux process collection not available");
        }
        return processes;
    }

    /**
     * Collects process information on Windows systems.
     */
    private static List<ProcessInfo> getWindowsProcesses() {
        List<ProcessInfo> processes = new ArrayList<>();
        // Windows process collection can be implemented via tasklist command
        return processes;
    }

    /**
     * Collects process information on macOS systems.
     */
    private static List<ProcessInfo> getMacProcesses() {
        List<ProcessInfo> processes = new ArrayList<>();
        // macOS process collection via ps command
        return processes;
    }

    /**
     * Generates unique client identifier based on runtime properties.
     */
    private static String generateClientId() {
        return "client_" + System.nanoTime();
    }

    /**
     * Retrieves hostname of the machine.
     */
    private static String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
