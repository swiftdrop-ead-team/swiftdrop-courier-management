package com.swiftdrop.aiassistant.controller;

import com.swiftdrop.aiassistant.entity.DailyDeliveryStats;
import com.swiftdrop.aiassistant.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/daily")
    public ResponseEntity<List<DailyDeliveryStats>> getDailyDeliveryReports() {
        return ResponseEntity.ok(reportService.getAllDailyReports());
    }
}