package com.example.coffeshopManagement.controller;

import com.example.coffeshopManagement.entity.Category;
import com.example.coffeshopManagement.entity.MenuItem;
import com.example.coffeshopManagement.repository.CategoryRepository;
import com.example.coffeshopManagement.repository.MenuItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@Controller
@RequestMapping("/manager/menu")
public class MenuController {

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping
    public String showMenuManagement(Model model) {
        List<MenuItem> menuItems = menuItemRepository.findAll();
        List<Category> categories = categoryRepository.findAll();
        model.addAttribute("menuItems", menuItems);
        model.addAttribute("categories", categories);
        model.addAttribute("newMenuItem", new MenuItem());
        model.addAttribute("newCategory", new Category());
        return "manager/menu-management";
    }

    @PostMapping("/add")
    public String addMenuItem(@ModelAttribute MenuItem newMenuItem) {
        if(newMenuItem.getCreatedAt() == null) {
            newMenuItem.setCreatedAt(Instant.now());
        }
        menuItemRepository.save(newMenuItem);
        return "redirect:/manager/menu";
    }

    @PostMapping("/category/add")
    public String addCategory(@ModelAttribute Category newCategory) {
        newCategory.setIsActive(true);
        if(newCategory.getSortOrder() == null) newCategory.setSortOrder(0);
        categoryRepository.save(newCategory);
        return "redirect:/manager/menu";
    }

    @PostMapping("/delete/{id}")
    public String deleteMenuItem(@PathVariable Integer id) {
        menuItemRepository.deleteById(id);
        return "redirect:/manager/menu";
    }
}
