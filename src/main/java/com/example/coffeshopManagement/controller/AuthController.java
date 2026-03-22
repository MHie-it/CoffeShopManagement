package com.example.coffeshopManagement.controller;

import com.example.coffeshopManagement.entity.Role;
import com.example.coffeshopManagement.entity.User;
import com.example.coffeshopManagement.repository.RoleRepository;
import com.example.coffeshopManagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.transaction.annotation.Transactional;



@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    @Transactional
    public String processRegister(@ModelAttribute("user") User user, Model model) {
        if (userRepository.existsByUsername(user.getUsername())) {
            model.addAttribute("error", "Username is already taken!");
            return "register";
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            model.addAttribute("error", "Email is already taken!");
            return "register";
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setActive(true);

        // Assign default role. We assign ROLE_STAFF for newly registered users.
        Role staffRole = roleRepository.findByName("ROLE_STAFF").orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName("ROLE_STAFF");
            return roleRepository.save(newRole);
        });

        java.util.Set<Role> assignedRoles = new java.util.HashSet<>();
        assignedRoles.add(staffRole);
        user.setRoles(assignedRoles);
        userRepository.save(user);

        return "redirect:/login?registered";
    }
}
