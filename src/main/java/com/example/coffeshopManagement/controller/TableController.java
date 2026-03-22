package com.example.coffeshopManagement.controller;

import com.example.coffeshopManagement.entity.TableEntity;
import com.example.coffeshopManagement.entity.TableStatus;
import com.example.coffeshopManagement.repository.TableEntityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/manager/tables")
public class TableController {

    @Autowired
    private TableEntityRepository tableRepository;

    @GetMapping
    public String showTableManagement(Model model) {
        model.addAttribute("tables", tableRepository.findAll());
        model.addAttribute("newTable", new TableEntity());
        model.addAttribute("statuses", TableStatus.values());
        return "manager/table-management";
    }

    @PostMapping("/add")
    public String addTable(@ModelAttribute TableEntity newTable) {
        if(newTable.getStatus() == null) {
            newTable.setStatus(TableStatus.empty);
        }
        tableRepository.save(newTable);
        return "redirect:/manager/tables";
    }

    @PostMapping("/delete/{id}")
    public String deleteTable(@PathVariable Integer id) {
        tableRepository.deleteById(id);
        return "redirect:/manager/tables";
    }
    
    @PostMapping("/update-status/{id}")
    public String updateStatus(@PathVariable Integer id, @RequestParam TableStatus status) {
        TableEntity table = tableRepository.findById(id).orElse(null);
        if(table != null) {
            table.setStatus(status);
            tableRepository.save(table);
        }
        return "redirect:/manager/tables";
    }
}
