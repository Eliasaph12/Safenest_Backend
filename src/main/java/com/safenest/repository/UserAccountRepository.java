package com.safenest.repository;

import com.safenest.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    Optional<UserAccount> findById(Long id);

    boolean existsByEmailIgnoreCase(String email);

    int countByRole(String role);

    List<UserAccount> findByRoleInOrderByNameAsc(Collection<String> roles);
}
