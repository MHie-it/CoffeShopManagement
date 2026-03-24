package com.example.coffeshopManagement.service;

import com.example.coffeshopManagement.dto.menu.CategoryCreateRequest;
import com.example.coffeshopManagement.dto.menu.CategoryResponse;
import com.example.coffeshopManagement.dto.menu.MenuItemCreateRequest;
import com.example.coffeshopManagement.dto.menu.MenuItemResponse;
import com.example.coffeshopManagement.entity.Category;
import com.example.coffeshopManagement.entity.MenuItem;
import com.example.coffeshopManagement.exception.ResourceNotFoundException;
import com.example.coffeshopManagement.repository.CategoryRepository;
import com.example.coffeshopManagement.repository.MenuItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MenuApiService {
    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;

    public MenuApiService(MenuItemRepository menuItemRepository, CategoryRepository categoryRepository) {
        this.menuItemRepository = menuItemRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> getAllMenuItems() {
        return menuItemRepository.findAll().stream().map(this::toMenuItemResponse).toList();
    }

    @Transactional
    public MenuItemResponse createMenuItem(MenuItemCreateRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        MenuItem item = new MenuItem();
        item.setCategory(category);
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setImageUrl(request.getImageUrl());
        item.setIsAvailable(request.getIsAvailable() == null ? Boolean.TRUE : request.getIsAvailable());
        return toMenuItemResponse(menuItemRepository.save(item));
    }

    @Transactional
    public MenuItemResponse updateMenuItem(Integer id, MenuItemCreateRequest request) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        item.setCategory(category);
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setImageUrl(request.getImageUrl());
        item.setIsAvailable(request.getIsAvailable() == null ? Boolean.TRUE : request.getIsAvailable());

        return toMenuItemResponse(menuItemRepository.save(item));
    }

    @Transactional
    public void deleteMenuItem(Integer id) {
        if (!menuItemRepository.existsById(id)) {
            throw new ResourceNotFoundException("Menu item not found");
        }
        menuItemRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream().map(this::toCategoryResponse).toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        Category category = new Category();
        category.setName(request.getName());
        category.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        category.setIsActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive());
        return toCategoryResponse(categoryRepository.save(category));
    }

    private MenuItemResponse toMenuItemResponse(MenuItem item) {
        MenuItemResponse response = new MenuItemResponse();
        response.setId(item.getId());
        response.setName(item.getName());
        response.setDescription(item.getDescription());
        response.setPrice(item.getPrice());
        response.setImageUrl(item.getImageUrl());
        response.setIsAvailable(item.getIsAvailable());
        response.setCreatedAt(item.getCreatedAt());
        response.setCategory(toCategoryResponse(item.getCategory()));
        return response;
    }

    private CategoryResponse toCategoryResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setSortOrder(category.getSortOrder());
        response.setIsActive(category.getIsActive());
        return response;
    }
}
