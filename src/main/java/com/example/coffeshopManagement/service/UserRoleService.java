package com.example.coffeshopManagement.service;

import com.example.coffeshopManagement.dto.user.RoleCreateRequest;
import com.example.coffeshopManagement.dto.user.RoleResponse;
import com.example.coffeshopManagement.dto.user.UserResponse;
import com.example.coffeshopManagement.entity.Role;
import com.example.coffeshopManagement.entity.User;
import com.example.coffeshopManagement.exception.BadRequestException;
import com.example.coffeshopManagement.exception.ResourceNotFoundException;
import com.example.coffeshopManagement.repository.RoleRepository;
import com.example.coffeshopManagement.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
public class UserRoleService {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public UserRoleService(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    public List<RoleResponse> getRoles() {
        return roleRepository.findAll().stream().map(this::toRoleResponse).toList();
    }

    public RoleResponse createRole(RoleCreateRequest request) {
        if (roleRepository.findByName(request.getName()).isPresent()) {
            throw new BadRequestException("Role already exists");
        }
        Role role = new Role();
        role.setName(request.getName());
        return toRoleResponse(roleRepository.save(role));
    }

    @Transactional
    public UserResponse assignRole(Integer userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }
        user.getRoles().add(role);
        return toUserResponse(userRepository.save(user));
    }

    public List<UserResponse> getUsers() {
        return userRepository.findAll().stream().map(this::toUserResponse).toList();
    }

    private RoleResponse toRoleResponse(Role role) {
        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setName(role.getName());
        return response;
    }

    private UserResponse toUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setActive(user.isActive());
        response.setRoles(user.getRoles().stream().map(Role::getName).toList());
        return response;
    }
}
