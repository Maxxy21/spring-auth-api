package com.maxwell.userregistration.service;

import com.maxwell.userregistration.exception.AccountLockedException;
import com.maxwell.userregistration.model.LoginAttempt;
import com.maxwell.userregistration.repository.LoginAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BruteForceProtectionServiceTest {

    @Mock private LoginAttemptRepository loginAttemptRepository;

    @InjectMocks private BruteForceProtectionService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "maxAttempts", 5);
        ReflectionTestUtils.setField(service, "lockDurationMinutes", 30);
    }

    @Test
    void checkIfBlocked_noRecord_doesNotThrow() {
        when(loginAttemptRepository.findByIdentifier(any())).thenReturn(Optional.empty());
        assertThatCode(() -> service.checkIfBlocked("user@test.com")).doesNotThrowAnyException();
    }

    @Test
    void checkIfBlocked_lockedAccount_throwsAccountLockedException() {
        LoginAttempt attempt = LoginAttempt.builder()
                .identifier("user@test.com")
                .failedAttempts(5)
                .lockedUntil(LocalDateTime.now().plusMinutes(25))
                .build();
        when(loginAttemptRepository.findByIdentifier("user@test.com")).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> service.checkIfBlocked("user@test.com"))
                .isInstanceOf(AccountLockedException.class);
    }

    @Test
    void checkIfBlocked_lockExpired_doesNotThrow() {
        LoginAttempt attempt = LoginAttempt.builder()
                .identifier("user@test.com")
                .failedAttempts(5)
                .lockedUntil(LocalDateTime.now().minusMinutes(5))
                .build();
        when(loginAttemptRepository.findByIdentifier("user@test.com")).thenReturn(Optional.of(attempt));

        assertThatCode(() -> service.checkIfBlocked("user@test.com")).doesNotThrowAnyException();
    }

    @Test
    void recordFailedAttempt_belowThreshold_doesNotLock() {
        when(loginAttemptRepository.findByIdentifier(any())).thenReturn(Optional.empty());
        when(loginAttemptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.recordFailedAttempt("user@test.com");

        verify(loginAttemptRepository).save(argThat(a -> a.getLockedUntil() == null));
    }

    @Test
    void recordFailedAttempt_atThreshold_locks() {
        LoginAttempt existing = LoginAttempt.builder()
                .identifier("user@test.com")
                .failedAttempts(4)
                .build();
        when(loginAttemptRepository.findByIdentifier(any())).thenReturn(Optional.of(existing));
        when(loginAttemptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.recordFailedAttempt("user@test.com");

        verify(loginAttemptRepository).save(argThat(a -> a.getLockedUntil() != null));
    }

    @Test
    void resetAttempts_clearsLock() {
        LoginAttempt locked = LoginAttempt.builder()
                .identifier("user@test.com")
                .failedAttempts(5)
                .lockedUntil(LocalDateTime.now().plusMinutes(20))
                .build();
        when(loginAttemptRepository.findByIdentifier("user@test.com")).thenReturn(Optional.of(locked));

        service.resetAttempts("user@test.com");

        verify(loginAttemptRepository).save(argThat(a ->
                a.getFailedAttempts() == 0 && a.getLockedUntil() == null
        ));
    }
}
