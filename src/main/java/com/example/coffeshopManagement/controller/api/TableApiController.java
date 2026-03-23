package com.example.coffeshopManagement.controller.api;

import com.example.coffeshopManagement.dto.common.ApiMessageResponse;
import com.example.coffeshopManagement.dto.table.TableCreateRequest;
import com.example.coffeshopManagement.dto.table.TableResponse;
import com.example.coffeshopManagement.dto.table.TableStatusUpdateRequest;
import com.example.coffeshopManagement.service.TableApiService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TableApiController {
    private final TableApiService tableApiService;

    public TableApiController(TableApiService tableApiService) {
        this.tableApiService = tableApiService;
    }

    @GetMapping("/api/staff/tables")
    public ResponseEntity<List<TableResponse>> getTables() {
        return ResponseEntity.ok(tableApiService.getAllTables());
    }

    @PostMapping("/api/manager/tables")
    public ResponseEntity<TableResponse> createTable(@Valid @RequestBody TableCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tableApiService.createTable(request));
    }

    @PatchMapping("/api/manager/tables/{id}/status")
    public ResponseEntity<TableResponse> updateStatus(
            @PathVariable Integer id,
            @Valid @RequestBody TableStatusUpdateRequest request) {
        return ResponseEntity.ok(tableApiService.updateStatus(id, request));
    }

    @DeleteMapping("/api/manager/tables/{id}")
    public ResponseEntity<ApiMessageResponse> deleteTable(@PathVariable Integer id) {
        tableApiService.deleteTable(id);
        return ResponseEntity.ok(new ApiMessageResponse("Table deleted"));
    }
}
