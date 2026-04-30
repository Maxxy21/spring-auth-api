package com.maxwell.userregistration.repository;

import com.maxwell.userregistration.model.EmailVerificationToken;
import com.maxwell.userregistration.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    Optional<EmailVerificationToken> findTopByUserOrderByExpiryDateDesc(User user);

    void deleteByUser(User user);
}
