package com.safenest.config;

import com.safenest.model.UserAccount;
import com.safenest.repository.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeederConfig {

    @Bean
    CommandLineRunner seedUsers(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            seedUser(userAccountRepository, passwordEncoder, "Demo Victim", "victim@example.com", "Victim", "demo123", "9000000001");
            seedUser(userAccountRepository, passwordEncoder, "Demo Counsellor", "counsellor@example.com", "Counsellor", "demo123", "9000000002");
            seedUser(userAccountRepository, passwordEncoder, "Demo Legal", "legal@example.com", "LegalAdvisor", "demo123", "9000000003");
            seedUser(userAccountRepository, passwordEncoder, "Demo Admin", "admin@example.com", "Admin", "demo123", "9000000004");
        };
    }

    private void seedUser(UserAccountRepository repository, PasswordEncoder passwordEncoder, String name, String email, String role, String password, String phoneNumber) {
        var existing = repository.findByEmailIgnoreCase(email).orElse(null);
        if (existing != null) {
            if (existing.getPhoneNumber() == null || existing.getPhoneNumber().isBlank()) {
                existing.setPhoneNumber(phoneNumber);
                existing.setPhoneVerified(true);
                repository.save(existing);
            }
            return;
        }

        UserAccount user = new UserAccount();
        user.setName(name);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setPhoneVerified(true);
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(password));
        repository.save(user);
    }
}
