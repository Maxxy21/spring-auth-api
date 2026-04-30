package com.maxwell.userregistration.service;

import com.maxwell.userregistration.dto.*;
import com.maxwell.userregistration.exception.*;
import com.maxwell.userregistration.model.EmailVerificationToken;
import com.maxwell.userregistration.model.RefreshToken;
import com.maxwell.userregistration.model.User;
import com.maxwell.userregistration.repository.EmailVerificationTokenRepository;
import com.maxwell.userregistration.repository.RefreshTokenRepository;
import com.maxwell.userregistration.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository evtRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final CaptchaService captchaService;
    private final BruteForceProtectionService bruteForceService;
    private final MfaService mfaService;

    @Value("${app.email.verification-expiry-hours}")
    private int verificationExpiryHours;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Transactional
    public void register(RegisterRequest request) {
        captchaService.verifyCaptcha(request.captchaToken());

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .build();

        userRepository.save(user);

        String token = createEmailVerificationToken(user);
        emailService.sendVerificationEmail(user, token);

        log.info("User registered: {}", user.getEmail());
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken evt = evtRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Verification token not found or already used"));

        if (evt.isConfirmed()) {
            throw new InvalidTokenException("Email address has already been verified");
        }
        if (evt.isExpired()) {
            throw new InvalidTokenException("Verification token has expired. Request a new one.");
        }

        evt.setConfirmed(true);
        evtRepository.save(evt);

        User user = evt.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        log.info("Email verified for user: {}", user.getEmail());
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        // Silent return — never reveal whether an email is registered or already verified
        userRepository.findByEmail(email)
                .filter(u -> !u.isEnabled())
                .ifPresent(user -> {
                    evtRepository.deleteByUser(user);
                    String token = createEmailVerificationToken(user);
                    emailService.sendVerificationEmail(user, token);
                });
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        bruteForceService.checkIfBlocked(request.email());

        User user = userRepository.findByEmail(request.email())
                .orElseGet(() -> {
                    // Record attempt even for non-existent accounts to prevent user enumeration timing attacks
                    bruteForceService.recordFailedAttempt(request.email());
                    throw new BadCredentialsException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            bruteForceService.recordFailedAttempt(request.email());
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw new EmailNotVerifiedException("Please verify your email before logging in");
        }

        if (user.isLocked()) {
            throw new AccountLockedException("Your account has been locked. Contact support.");
        }

        bruteForceService.resetAttempts(request.email());

        if (user.isMfaEnabled()) {
            String tempToken = jwtService.generateMfaPendingToken(user.getEmail());
            return AuthResponse.mfaPending(tempToken, user.getEmail());
        }

        return issueTokenPair(user);
    }

    @Transactional
    public AuthResponse validateMfa(MfaVerifyRequest request) {
        if (!jwtService.validateToken(request.tempToken())) {
            throw new InvalidTokenException("Invalid or expired MFA session token");
        }
        if (!jwtService.isMfaPending(request.tempToken())) {
            throw new InvalidTokenException("Token is not a pending MFA token");
        }

        String email = jwtService.extractEmail(request.tempToken());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidTokenException("User not found"));

        if (!mfaService.verifyCode(user.getMfaSecret(), Integer.parseInt(request.totpCode()))) {
            throw new InvalidTokenException("Invalid TOTP code");
        }

        return issueTokenPair(user);
    }

    @Transactional(noRollbackFor = InvalidTokenException.class)
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (stored.isRevoked()) {
            // Revoke all tokens for this user — possible token theft
            refreshTokenRepository.revokeAllUserTokens(stored.getUser());
            throw new InvalidTokenException("Refresh token has been revoked");
        }
        if (stored.isExpired()) {
            throw new InvalidTokenException("Refresh token has expired. Please log in again.");
        }

        // Token rotation: revoke current, issue new pair
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueTokenPair(stored.getUser());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByToken(rawRefreshToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public MfaSetupResponse setupMfa(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        if (user.isMfaEnabled()) {
            throw new InvalidTokenException("MFA is already enabled. Disable it first.");
        }

        String secret = mfaService.generateSecret();
        user.setMfaSecret(secret);
        userRepository.save(user);

        String qrUrl = mfaService.generateQrCodeUrl(email, secret);
        return new MfaSetupResponse(secret, qrUrl);
    }

    @Transactional
    public void enableMfa(String email, String totpCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        if (user.getMfaSecret() == null) {
            throw new InvalidTokenException("Call /api/users/mfa/setup first");
        }
        if (!mfaService.verifyCode(user.getMfaSecret(), Integer.parseInt(totpCode))) {
            throw new InvalidTokenException("Invalid TOTP code");
        }

        user.setMfaEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    public void disableMfa(String email, String totpCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        if (!user.isMfaEnabled()) {
            throw new InvalidTokenException("MFA is not enabled");
        }
        if (!mfaService.verifyCode(user.getMfaSecret(), Integer.parseInt(totpCode))) {
            throw new InvalidTokenException("Invalid TOTP code");
        }

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
    }

    public UserProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);
        return UserProfileResponse.from(user);
    }

    // --- helpers ---

    private String createEmailVerificationToken(User user) {
        String token = UUID.randomUUID().toString();
        EmailVerificationToken evt = EmailVerificationToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(verificationExpiryHours))
                .build();
        evtRepository.save(evt);
        return token;
    }

    private AuthResponse issueTokenPair(User user) {
        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = UUID.randomUUID().toString();

        RefreshToken stored = RefreshToken.builder()
                .token(refreshToken)
                .user(user)
                .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .build();
        refreshTokenRepository.save(stored);

        return AuthResponse.authenticated(accessToken, refreshToken, user.getEmail());
    }
}
