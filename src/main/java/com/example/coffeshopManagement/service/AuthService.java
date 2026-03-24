package com.example.coffeshopManagement.service;

import com.example.coffeshopManagement.dto.auth.AuthLoginRequest;
import com.example.coffeshopManagement.dto.auth.AuthRegisterRequest;
import com.example.coffeshopManagement.dto.auth.AuthResponse;
import com.example.coffeshopManagement.entity.Role;
import com.example.coffeshopManagement.entity.User;
import com.example.coffeshopManagement.exception.BadRequestException;
import com.example.coffeshopManagement.repository.RoleRepository;
import com.example.coffeshopManagement.repository.UserRepository;
import com.example.coffeshopManagement.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(AuthRegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();
        String fullName = request.getFullName().trim();
        String phone = request.getPhone().trim();

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password confirmation does not match");
        }

        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already taken");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new BadRequestException("Phone number is already taken");
        }

        Role staffRole = roleRepository.findByName("ROLE_STAFF").orElseGet(() -> {
            Role role = new Role();
            role.setName("ROLE_STAFF");
            return roleRepository.save(role);
        });

        User user = new User();
        user.setUsername(username);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setActive(true);
        Set<Role> roles = new HashSet<>();
        roles.add(staffRole);
        user.setRoles(roles);

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(toUserDetails(saved));
        return buildAuthResponse(saved, token);
    }

    public AuthResponse login(AuthLoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword()));
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BadRequestException("User account not found"));
        String token = jwtService.generateToken(userDetails);
        return buildAuthResponse(user, token);
    }

    private UserDetails toUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRoles().stream().map(Role::getName).toArray(String[]::new))
                .disabled(!user.isActive())
                .build();
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail(), roles);
    }
}
