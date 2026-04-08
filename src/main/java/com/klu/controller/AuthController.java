package com.klu.controller;

import com.klu.model.User;
import com.klu.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // Signup
    @PostMapping("/signup")
    public User register(@RequestBody User user) {
        logger.info("Registration attempt for email: {}", user.getEmail());
        try {
            User registered = userService.registerUser(user);
            logger.info("Successfully registered user: {}", user.getEmail());
            return registered;
        } catch (Exception e) {
            logger.error("Registration failed for email: {}", user.getEmail(), e);
            throw e;
        }
    }

    // Login
    @PostMapping("/login")
    public User login(@RequestBody User user) {
        logger.info("Login attempt for email: {}", user.getEmail());
        try {
            User loggedIn = userService.login(user.getEmail(), user.getPassword());
            if (loggedIn != null) {
                logger.info("Successful login for user: {}", user.getEmail());
            } else {
                logger.warn("Failed login attempt for email: {}", user.getEmail());
            }
            return loggedIn;
        } catch (Exception e) {
            logger.error("Login error for email: {}", user.getEmail(), e);
            throw e;
        }
    }
}