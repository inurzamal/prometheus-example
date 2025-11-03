package com.nur.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MonitoringService {

    @Autowired
    private MeterRegistry meterRegistry;

    public Map<String, Object> collectJvmMetrics() {
        Map<String, Object> result = new HashMap<>();

        // --- Micrometer metrics ---
        result.put("system.cpu.usage", getGauge("system.cpu.usage"));
        result.put("process.uptime.seconds", getGauge("process.uptime"));
        result.put("system.load.average.1m", getGauge("system.load.average.1m"));
        result.put("jvm.memory.used.bytes", getGauge("jvm.memory.used"));
        result.put("jvm.memory.max.bytes", getGauge("jvm.memory.max"));
        result.put("jvm.threads.live", getGauge("jvm.threads.live"));
        result.put("jvm.threads.daemon", getGauge("jvm.threads.daemon"));
        result.put("jvm.gc.pause.seconds", getGauge("jvm.gc.pause"));

        // --- Heap & Non-Heap ---
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();

        double heapUsedPercent = (heap.getMax() > 0)
                ? ((double) heap.getUsed() / heap.getMax()) * 100
                : 0;

        Map<String, Object> heapSummary = new HashMap<>();
        heapSummary.put("usedMB", heap.getUsed() / (1024 * 1024));
        heapSummary.put("committedMB", heap.getCommitted() / (1024 * 1024));
        heapSummary.put("maxMB", heap.getMax() / (1024 * 1024));
        heapSummary.put("usedPercent", Math.round(heapUsedPercent * 100.0) / 100.0);

        Map<String, Object> nonHeapSummary = new HashMap<>();
        nonHeapSummary.put("usedMB", nonHeap.getUsed() / (1024 * 1024));
        nonHeapSummary.put("committedMB", nonHeap.getCommitted() / (1024 * 1024));
        nonHeapSummary.put("maxMB", nonHeap.getMax() / (1024 * 1024));

        result.put("heapMemory", heapSummary);
        result.put("nonHeapMemory", nonHeapSummary);

        // --- Disk space ---
        File root = new File("/");
        long total = root.getTotalSpace();
        long free = root.getFreeSpace();
        long used = total - free;

        Map<String, Object> diskInfo = new HashMap<>();
        diskInfo.put("totalGB", total / (1024 * 1024 * 1024));
        diskInfo.put("usedGB", used / (1024 * 1024 * 1024));
        diskInfo.put("freeGB", free / (1024 * 1024 * 1024));
        diskInfo.put("usagePercent", (total > 0) ? (used * 100.0) / total : 0);
        result.put("disk", diskInfo);

        // --- Threads ---
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        Map<String, Object> threadInfo = new HashMap<>();
        threadInfo.put("live", threadMXBean.getThreadCount());
        threadInfo.put("daemon", threadMXBean.getDaemonThreadCount());
        threadInfo.put("peak", threadMXBean.getPeakThreadCount());
        threadInfo.put("totalStarted", threadMXBean.getTotalStartedThreadCount());
        result.put("threads", threadInfo);

        // --- Garbage Collectors ---
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        Map<String, Object> gcDetails = new HashMap<>();
        for (GarbageCollectorMXBean gc : gcBeans) {
            gcDetails.put(gc.getName(), Map.of(
                    "collectionCount", gc.getCollectionCount(),
                    "collectionTimeMS", gc.getCollectionTime()
            ));
        }
        result.put("garbageCollectors", gcDetails);

        // --- Class Loading ---
        ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
        Map<String, Object> classInfo = new HashMap<>();
        classInfo.put("loadedClassCount", classLoadingMXBean.getLoadedClassCount());
        classInfo.put("totalLoadedClassCount", classLoadingMXBean.getTotalLoadedClassCount());
        classInfo.put("unloadedClassCount", classLoadingMXBean.getUnloadedClassCount());
        result.put("classLoading", classInfo);

        // --- OS info ---
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        result.put("systemLoadAverage", osBean.getSystemLoadAverage());
        result.put("availableProcessors", osBean.getAvailableProcessors());
        result.put("osArch", osBean.getArch());
        result.put("osVersion", osBean.getVersion());
        result.put("osName", osBean.getName());

        // --- Runtime info ---
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        result.put("jvmName", runtimeMXBean.getVmName());
        result.put("jvmVendor", runtimeMXBean.getVmVendor());
        result.put("javaVersion", System.getProperty("java.version"));
        result.put("startTime", runtimeMXBean.getStartTime());
        result.put("uptimeFormatted", formatUptime(runtimeMXBean.getUptime() / 1000.0));

        return result;
    }

    private Double getGauge(String name) {
        try {
            var gauge = Search.in(meterRegistry).name(name).gauge();
            return (gauge != null) ? gauge.value() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String formatUptime(double seconds) {
        long totalSec = (long) seconds;
        long hours = totalSec / 3600;
        long minutes = (totalSec % 3600) / 60;
        long secs = totalSec % 60;
        return String.format("%02dh %02dm %02ds", hours, minutes, secs);
    }
}
