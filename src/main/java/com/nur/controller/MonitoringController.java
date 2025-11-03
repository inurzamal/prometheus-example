package com.nur.controller;

import com.nur.service.MonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class MonitoringController {

    @Autowired
    private MonitoringService monitoringService;

    @GetMapping("/api/monitor/jvm")
    public Map<String, Object> getJvmMetrics() {
        return monitoringService.collectJvmMetrics();
    }
}
