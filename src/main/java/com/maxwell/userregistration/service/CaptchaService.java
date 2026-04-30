package com.maxwell.userregistration.service;

import com.maxwell.userregistration.exception.InvalidCaptchaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaptchaService {

    private final RestTemplate restTemplate;

    @Value("${app.captcha.enabled}")
    private boolean captchaEnabled;

    @Value("${app.captcha.secret-key}")
    private String secretKey;

    @Value("${app.captcha.verify-url}")
    private String verifyUrl;

    public void verifyCaptcha(String token) {
        if (!captchaEnabled) {
            return;
        }
        if (token == null || token.isBlank()) {
            throw new InvalidCaptchaException("CAPTCHA token is required");
        }

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("secret", secretKey);
        params.add("response", token);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    verifyUrl, new HttpEntity<>(params), Map.class
            );
            if (response.getBody() == null || !Boolean.TRUE.equals(response.getBody().get("success"))) {
                throw new InvalidCaptchaException("CAPTCHA verification failed");
            }
        } catch (InvalidCaptchaException e) {
            throw e;
        } catch (Exception e) {
            log.error("CAPTCHA service error: {}", e.getMessage());
            throw new InvalidCaptchaException("Could not verify CAPTCHA. Please try again.");
        }
    }
}
