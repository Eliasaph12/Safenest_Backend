package com.klu.service;

import com.klu.model.User;
import com.klu.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Signup
    public User registerUser(User user) {
        logger.info("Registering new user with email: {}", user.getEmail());
        // Encode password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    // Login
    public User login(String email, String password) {
        logger.debug("Login attempt for email: {}", email);
        User user = userRepository.findByEmail(email);

        if(user != null && passwordEncoder.matches(password, user.getPassword())) {
            logger.info("Successful authentication for user: {}", email);
            return user;
        }
        logger.warn("Authentication failed for email: {}", email);
        return null;
    }
}