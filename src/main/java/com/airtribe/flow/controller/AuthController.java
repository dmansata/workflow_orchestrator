package com.airtribe.flow.controller;

import com.airtribe.flow.model.User;
import com.airtribe.flow.repository.UserRepository;
import com.airtribe.flow.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> request) {
        if (userRepository.findByUsername(request.get("username")).isPresent()) {
            return ResponseEntity.badRequest().body("Error: Username is already taken!");
        }

        User user = User.builder()
                .username(request.get("username"))
                .email(request.get("email"))
                .password(passwordEncoder.encode(request.get("password")))
                .build();

        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody Map<String, String> request) {
        Optional<User> userOpt = userRepository.findByUsername(request.get("username"));
        
        if (userOpt.isPresent() && passwordEncoder.matches(request.get("password"), userOpt.get().getPassword())) {
            User user = userOpt.get();
            String jwt = jwtUtils.generateToken(user.getUsername(), user.getId());
            return ResponseEntity.ok(Map.of("token", jwt));
        }

        return ResponseEntity.status(401).body("Error: Invalid username or password");
    }
}
