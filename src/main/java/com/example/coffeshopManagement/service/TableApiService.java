package com.example.coffeshopManagement.service;

import com.example.coffeshopManagement.dto.table.TableCreateRequest;
import com.example.coffeshopManagement.dto.table.TableResponse;
import com.example.coffeshopManagement.dto.table.TableStatusUpdateRequest;
import com.example.coffeshopManagement.entity.TableEntity;
import com.example.coffeshopManagement.entity.TableStatus;
import com.example.coffeshopManagement.exception.ResourceNotFoundException;
import com.example.coffeshopManagement.repository.TableEntityRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TableApiService {
    private final TableEntityRepository tableRepository;

    public TableApiService(TableEntityRepository tableRepository) {
        this.tableRepository = tableRepository;
    }

    public List<TableResponse> getAllTables() {
        return tableRepository.findAll().stream().map(this::toResponse).toList();
    }

    public TableResponse createTable(TableCreateRequest request) {
        TableEntity table = new TableEntity();
        table.setName(request.getName());
        table.setCapacity(request.getCapacity() == null ? 4 : request.getCapacity());
        table.setStatus(request.getStatus() == null ? TableStatus.empty : request.getStatus());
        table.setNote(request.getNote());
        return toResponse(tableRepository.save(table));
    }

    public TableResponse updateStatus(Integer id, TableStatusUpdateRequest request) {
        TableEntity table = tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found"));
        table.setStatus(request.getStatus());
        return toResponse(tableRepository.save(table));
    }

    public void deleteTable(Integer id) {
        if (!tableRepository.existsById(id)) {
            throw new ResourceNotFoundException("Table not found");
        }
        tableRepository.deleteById(id);
    }

    private TableResponse toResponse(TableEntity table) {
        TableResponse response = new TableResponse();
        response.setId(table.getId());
        response.setName(table.getName());
        response.setCapacity(table.getCapacity());
        response.setStatus(table.getStatus());
        response.setNote(table.getNote());
        return response;
    }
}
