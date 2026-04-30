package com.maxwell.userregistration.repository;

import com.maxwell.userregistration.model.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    Optional<LoginAttempt> findByIdentifier(String identifier);
}
