package com.example.coffeshopManagement.controller.api;

import com.example.coffeshopManagement.dto.report.SalesSummaryResponse;
import com.example.coffeshopManagement.dto.report.TopMenuItemResponse;
import com.example.coffeshopManagement.exception.BadRequestException;
import com.example.coffeshopManagement.service.ReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
public class ReportApiController {
    private final ReportService reportService;

    public ReportApiController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/api/admin/reports/sales/daily")
    public ResponseEntity<SalesSummaryResponse> dailySales(@RequestParam LocalDate date) {
        return ResponseEntity.ok(reportService.summaryByDate(date));
    }

    @GetMapping("/api/admin/reports/sales/monthly")
    public ResponseEntity<SalesSummaryResponse> monthlySales(
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(reportService.summaryByMonth(year, month));
    }

    @GetMapping("/api/admin/reports/top-menu-items")
    public ResponseEntity<List<TopMenuItemResponse>> topMenuItems(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate,
            @RequestParam(defaultValue = "10") int limit) {
        if (fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate must be before or equal to toDate");
        }
        Instant from = fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant to = toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);
        return ResponseEntity.ok(reportService.topMenuItems(from, to, limit));
    }

    @GetMapping("/api/admin/reports/export/sales.csv")
    public ResponseEntity<byte[]> exportSalesCsv(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate) {
        if (fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate must be before or equal to toDate");
        }
        Instant from = fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant to = toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);
        byte[] body = reportService.exportSalesCsv(from, to).getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("sales-report.csv").build());
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
