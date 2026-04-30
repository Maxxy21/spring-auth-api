package com.maxwell.userregistration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maxwell.userregistration.config.SecurityConfig;
import com.maxwell.userregistration.dto.AuthResponse;
import com.maxwell.userregistration.dto.LoginRequest;
import com.maxwell.userregistration.dto.RegisterRequest;
import com.maxwell.userregistration.exception.EmailAlreadyExistsException;
import com.maxwell.userregistration.exception.EmailNotVerifiedException;
import com.maxwell.userregistration.service.CustomUserDetailsService;
import com.maxwell.userregistration.service.JwtService;
import com.maxwell.userregistration.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // The real UserService is replaced; JwtService + CustomUserDetailsService are needed
    // by JwtAuthenticationFilter which @WebMvcTest picks up as a @Component Filter.
    @MockBean UserService userService;
    @MockBean JwtService jwtService;
    @MockBean CustomUserDetailsService customUserDetailsService;

    @Test
    void register_validRequest_returns201() throws Exception {
        doNothing().when(userService).register(any());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("John", "Doe", "john@example.com", "password123", null)
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        doThrow(new EmailAlreadyExistsException("john@example.com"))
                .when(userService).register(any());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("John", "Doe", "john@example.com", "password123", null)
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Email Already Exists"));
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("John", "Doe", "not-an-email", "password123", null)
                        )))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("John", "Doe", "john@example.com", "short", null)
                        )))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_validCredentials_returns200WithTokens() throws Exception {
        when(userService.login(any()))
                .thenReturn(AuthResponse.authenticated("access-token", "refresh-token", "john@example.com"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("john@example.com", "password123")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.mfaRequired").value(false));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        when(userService.login(any())).thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("john@example.com", "wrongpass")
                        )))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unverifiedEmail_returns403() throws Exception {
        when(userService.login(any())).thenThrow(new EmailNotVerifiedException("Please verify your email"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("john@example.com", "password123")
                        )))
                .andExpect(status().isForbidden());
    }

    @Test
    void login_mfaEnabled_returnsMfaPendingResponse() throws Exception {
        when(userService.login(any()))
                .thenReturn(AuthResponse.mfaPending("temp-jwt", "john@example.com"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("john@example.com", "password123")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaRequired").value(true))
                .andExpect(jsonPath("$.tempToken").value("temp-jwt"));
    }
}
