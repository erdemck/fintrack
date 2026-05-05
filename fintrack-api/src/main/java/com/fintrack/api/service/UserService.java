package com.fintrack.api.service;

import com.fintrack.core.dto.AuthResponse;
import com.fintrack.core.dto.LoginRequest;
import com.fintrack.core.dto.RegisterRequest;
import com.fintrack.core.security.JwtService;
import com.fintrack.persistence.entity.UserEntity;
import com.fintrack.persistence.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("This email is already registered: " + request.getEmail());
        }

        UserEntity user = new UserEntity();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        UserEntity savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser.getId(), savedUser.getEmail());
        return new AuthResponse(token, savedUser.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getEmail());
    }
}
