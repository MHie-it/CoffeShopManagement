package com.example.coffeshopManagement.controller.api;

import com.example.coffeshopManagement.dto.common.ApiMessageResponse;
import com.example.coffeshopManagement.dto.menu.CategoryCreateRequest;
import com.example.coffeshopManagement.dto.menu.CategoryResponse;
import com.example.coffeshopManagement.dto.menu.MenuItemCreateRequest;
import com.example.coffeshopManagement.dto.menu.MenuItemResponse;
import com.example.coffeshopManagement.service.MenuApiService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MenuApiController {
    private final MenuApiService menuApiService;

    public MenuApiController(MenuApiService menuApiService) {
        this.menuApiService = menuApiService;
    }

    @GetMapping("/api/staff/menu-items")
    public ResponseEntity<List<MenuItemResponse>> getMenuItems() {
        return ResponseEntity.ok(menuApiService.getAllMenuItems());
    }

    @GetMapping("/api/manager/menu-items")
    public ResponseEntity<List<MenuItemResponse>> getMenuItemsForManager() {
        return ResponseEntity.ok(menuApiService.getAllMenuItems());
    }

    @PostMapping("/api/manager/menu-items")
    public ResponseEntity<MenuItemResponse> createMenuItem(@Valid @RequestBody MenuItemCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(menuApiService.createMenuItem(request));
    }

    @PutMapping("/api/manager/menu-items/{id}")
    public ResponseEntity<MenuItemResponse> updateMenuItem(
            @PathVariable Integer id,
            @Valid @RequestBody MenuItemCreateRequest request) {
        return ResponseEntity.ok(menuApiService.updateMenuItem(id, request));
    }

    @DeleteMapping("/api/manager/menu-items/{id}")
    public ResponseEntity<ApiMessageResponse> deleteMenuItem(@PathVariable Integer id) {
        menuApiService.deleteMenuItem(id);
        return ResponseEntity.ok(new ApiMessageResponse("Menu item deleted"));
    }

    @GetMapping("/api/staff/categories")
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        return ResponseEntity.ok(menuApiService.getAllCategories());
    }

    @GetMapping("/api/manager/categories")
    public ResponseEntity<List<CategoryResponse>> getCategoriesForManager() {
        return ResponseEntity.ok(menuApiService.getAllCategories());
    }

    @PostMapping("/api/manager/categories")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(menuApiService.createCategory(request));
    }
}
