package com.monitor.shared;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data class representing a process snapshot.
 * Contains basic process information for display in dashboards.
 */
public class ProcessInfo {
    @JsonProperty("pid")
    private long pid;

    @JsonProperty("name")
    private String name;

    @JsonProperty("cpuUsage")
    private double cpuUsage;

    @JsonProperty("memoryUsage")
    private long memoryUsage;

    public ProcessInfo() {}

    public ProcessInfo(long pid, String name, double cpuUsage, long memoryUsage) {
        this.pid = pid;
        this.name = name;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
    }

    // Getters and setters
    public long getPid() { return pid; }
    public void setPid(long pid) { this.pid = pid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }

    public long getMemoryUsage() { return memoryUsage; }
    public void setMemoryUsage(long memoryUsage) { this.memoryUsage = memoryUsage; }

    @Override
    public String toString() {
        return "ProcessInfo{" +
                "pid=" + pid +
                ", name='" + name + '\'' +
                ", cpuUsage=" + cpuUsage +
                ", memoryUsage=" + memoryUsage +
                '}';
    }
}
