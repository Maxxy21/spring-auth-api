package com.maxwell.userregistration.service;

import com.maxwell.userregistration.exception.AccountLockedException;
import com.maxwell.userregistration.model.LoginAttempt;
import com.maxwell.userregistration.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class BruteForceProtectionService {

    private final LoginAttemptRepository loginAttemptRepository;

    @Value("${app.brute-force.max-attempts}")
    private int maxAttempts;

    @Value("${app.brute-force.lock-duration-minutes}")
    private int lockDurationMinutes;

    @Transactional(readOnly = true)
    public void checkIfBlocked(String identifier) {
        loginAttemptRepository.findByIdentifier(identifier).ifPresent(attempt -> {
            if (attempt.isCurrentlyLocked()) {
                String until = attempt.getLockedUntil()
                        .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                throw new AccountLockedException(
                        "Account is temporarily locked due to too many failed login attempts. Try again after " + until
                );
            }
        });
    }

    @Transactional
    public void recordFailedAttempt(String identifier) {
        LoginAttempt attempt = loginAttemptRepository.findByIdentifier(identifier)
                .orElseGet(() -> LoginAttempt.builder()
                        .identifier(identifier)
                        .failedAttempts(0)
                        .build());

        attempt.setFailedAttempts(attempt.getFailedAttempts() + 1);
        attempt.setLastAttemptTime(LocalDateTime.now());

        if (attempt.getFailedAttempts() >= maxAttempts) {
            attempt.setLockedUntil(LocalDateTime.now().plusMinutes(lockDurationMinutes));
            log.warn("Account {} locked after {} failed attempts", identifier, maxAttempts);
        }

        loginAttemptRepository.save(attempt);
    }

    @Transactional
    public void resetAttempts(String identifier) {
        loginAttemptRepository.findByIdentifier(identifier).ifPresent(attempt -> {
            attempt.setFailedAttempts(0);
            attempt.setLockedUntil(null);
            loginAttemptRepository.save(attempt);
        });
    }
}
