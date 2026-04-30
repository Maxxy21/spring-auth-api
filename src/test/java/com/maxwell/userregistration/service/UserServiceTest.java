package com.maxwell.userregistration.service;

import com.maxwell.userregistration.dto.LoginRequest;
import com.maxwell.userregistration.dto.RegisterRequest;
import com.maxwell.userregistration.exception.AccountLockedException;
import com.maxwell.userregistration.exception.EmailAlreadyExistsException;
import com.maxwell.userregistration.exception.EmailNotVerifiedException;
import com.maxwell.userregistration.model.User;
import com.maxwell.userregistration.repository.EmailVerificationTokenRepository;
import com.maxwell.userregistration.repository.RefreshTokenRepository;
import com.maxwell.userregistration.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmailVerificationTokenRepository evtRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private EmailService emailService;
    @Mock private CaptchaService captchaService;
    @Mock private BruteForceProtectionService bruteForceService;
    @Mock private MfaService mfaService;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "verificationExpiryHours", 24);
        ReflectionTestUtils.setField(userService, "refreshTokenExpiration", 604_800_000L);
    }

    // --- Registration ---

    @Test
    void register_success() {
        RegisterRequest req = new RegisterRequest("John", "Doe", "john@example.com", "password123", "token");
        when(userRepository.existsByEmail(req.email())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        userService.register(req);

        verify(userRepository).save(any(User.class));
        verify(evtRepository).save(any());
        verify(emailService).sendVerificationEmail(any(), any());
    }

    @Test
    void register_duplicateEmail_throwsEmailAlreadyExistsException() {
        RegisterRequest req = new RegisterRequest("John", "Doe", "john@example.com", "password123", "token");
        when(userRepository.existsByEmail(req.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    // --- Login ---

    @Test
    void login_userNotFound_throwsBadCredentials() {
        LoginRequest req = new LoginRequest("unknown@example.com", "password");
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_wrongPassword_throwsBadCredentials() {
        User user = enabledUser();
        LoginRequest req = new LoginRequest(user.getEmail(), "wrongpassword");
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.password(), user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> userService.login(req))
                .isInstanceOf(BadCredentialsException.class);

        verify(bruteForceService).recordFailedAttempt(req.email());
    }

    @Test
    void login_emailNotVerified_throwsEmailNotVerifiedException() {
        User user = User.builder()
                .email("john@example.com")
                .password("hashed")
                .enabled(false)
                .locked(false)
                .build();
        LoginRequest req = new LoginRequest(user.getEmail(), "password");
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> userService.login(req))
                .isInstanceOf(EmailNotVerifiedException.class);
    }

    @Test
    void login_lockedAccount_throwsAccountLockedException() {
        User user = User.builder()
                .email("john@example.com")
                .password("hashed")
                .enabled(true)
                .locked(true)
                .build();
        LoginRequest req = new LoginRequest(user.getEmail(), "password");
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> userService.login(req))
                .isInstanceOf(AccountLockedException.class);
    }

    @Test
    void login_success_returnsAuthResponse() {
        User user = enabledUser();
        LoginRequest req = new LoginRequest(user.getEmail(), "password");
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(jwtService.generateAccessToken(user.getEmail())).thenReturn("access-token");
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = userService.login(req);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.mfaRequired()).isFalse();
        verify(bruteForceService).resetAttempts(req.email());
    }

    // --- Helpers ---

    private User enabledUser() {
        return User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("hashed")
                .enabled(true)
                .locked(false)
                .mfaEnabled(false)
                .build();
    }
}
