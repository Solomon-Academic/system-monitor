package com.monitor.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Data class representing system metrics snapshot.
 * Serialized to JSON and transmitted between client and server.
 */
public class SystemMetrics {
    @JsonProperty("clientId")
    private String clientId;

    @JsonProperty("hostname")
    private String hostname;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("cpuUsage")
    private double cpuUsage;

    @JsonProperty("ramUsage")
    private double ramUsage;

    @JsonProperty("ramTotal")
    private long ramTotal;

    @JsonProperty("ramUsed")
    private long ramUsed;

    @JsonProperty("processes")
    private List<ProcessInfo> processes;

    public SystemMetrics() {}

    public SystemMetrics(String clientId, String hostname, long timestamp,
                         double cpuUsage, double ramUsage, long ramTotal,
                         long ramUsed, List<ProcessInfo> processes) {
        this.clientId = clientId;
        this.hostname = hostname;
        this.timestamp = timestamp;
        this.cpuUsage = cpuUsage;
        this.ramUsage = ramUsage;
        this.ramTotal = ramTotal;
        this.ramUsed = ramUsed;
        this.processes = processes;
    }

    // Getters and setters
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }

    public double getRamUsage() { return ramUsage; }
    public void setRamUsage(double ramUsage) { this.ramUsage = ramUsage; }

    public long getRamTotal() { return ramTotal; }
    public void setRamTotal(long ramTotal) { this.ramTotal = ramTotal; }

    public long getRamUsed() { return ramUsed; }
    public void setRamUsed(long ramUsed) { this.ramUsed = ramUsed; }

    public List<ProcessInfo> getProcesses() { return processes; }
    public void setProcesses(List<ProcessInfo> processes) { this.processes = processes; }

    @Override
    public String toString() {
        return "SystemMetrics{" +
                "clientId='" + clientId + '\'' +
                ", hostname='" + hostname + '\'' +
                ", timestamp=" + timestamp +
                ", cpuUsage=" + cpuUsage +
                ", ramUsage=" + ramUsage +
                ", processes=" + (processes != null ? processes.size() : 0) +
                '}';
    }
}
