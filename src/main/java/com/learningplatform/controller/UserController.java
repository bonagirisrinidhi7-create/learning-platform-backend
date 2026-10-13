
package com.learningplatform.controller;

import com.learningplatform.dto.LoginRequest;
import com.learningplatform.dto.UpdateRequest;
import com.learningplatform.entity.User;
import com.learningplatform.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:3000",
        "https://learning-platform-frontend-beta.vercel.app"
})
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {

        if (user.getUsername() == null || user.getUsername().isBlank()
                || user.getPassword() == null || user.getPassword().isBlank()
                || user.getEmail() == null || user.getEmail().isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Username, email and password are required");
        }

        user.setUsername(user.getUsername().trim());
        user.setEmail(user.getEmail().trim());

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("LEARNER");
        } else {
            user.setRole(user.getRole().trim().toUpperCase());
        }

        User savedUser = userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Registration successful");
        response.put("id", savedUser.getId());
        response.put("username", savedUser.getUsername());
        response.put("email", savedUser.getEmail());
        response.put("role", savedUser.getRole());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        if (request.getUsername() == null
                || request.getUsername().isBlank()
                || request.getPassword() == null
                || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Username and password are required");
        }

        User user = userRepository
                .findByUsername(request.getUsername().trim())
                .orElse(null);

        if (user == null || user.getPassword() == null
                || !user.getPassword().equals(request.getPassword())) {
            return ResponseEntity.status(401)
                    .body("Invalid username or password");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login successful");
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/getuser")
    public ResponseEntity<?> getUser(@RequestParam String username) {

        User user = userRepository
                .findByUsername(username.trim())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(404).body("User not found");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/getalldata")
    public ResponseEntity<?> getAllUsers() {

        List<User> users = userRepository.findAll();
        List<Map<String, Object>> response = new ArrayList<>();

        for (User user : users) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("username", user.getUsername());
            data.put("email", user.getEmail());
            data.put("role", user.getRole());
            response.add(data);
        }

        return ResponseEntity.ok(response);
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateUser(@RequestBody UpdateRequest request) {

        if (request.getCurrentUsername() == null
                || request.getCurrentUsername().isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Current username is required");
        }

        User user = userRepository
                .findByUsername(request.getCurrentUsername().trim())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(404).body("User not found");
        }

        if (request.getUsername() != null
                && !request.getUsername().isBlank()) {

            String newUsername = request.getUsername().trim();

            if (!newUsername.equals(user.getUsername())) {
                if (userRepository.findByUsername(newUsername).isPresent()) {
                    return ResponseEntity.badRequest()
                            .body("Username already exists");
                }

                user.setUsername(newUsername);
            }
        }

        if (request.getEmail() != null
                && !request.getEmail().isBlank()) {

            String newEmail = request.getEmail().trim();

            if (!newEmail.equals(user.getEmail())) {
                if (userRepository.findByEmail(newEmail).isPresent()) {
                    return ResponseEntity.badRequest()
                            .body("Email already exists");
                }

                user.setEmail(newEmail);
            }
        }

        if (request.getPassword() != null
                && !request.getPassword().isBlank()) {
            user.setPassword(request.getPassword());
        }

        User updatedUser = userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User updated successfully");
        response.put("id", updatedUser.getId());
        response.put("username", updatedUser.getUsername());
        response.put("email", updatedUser.getEmail());
        response.put("role", updatedUser.getRole());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUser(@RequestParam String username) {

        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Username is required");
        }

        User user = userRepository
                .findByUsername(username.trim())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(404).body("User not found");
        }

        userRepository.delete(user);

        return ResponseEntity.ok("User deleted successfully");
    }
}
