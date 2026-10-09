package com.medifind.service;

import com.medifind.dto.auth.LoginRequest;
import com.medifind.dto.auth.RegisterRequest;
import com.medifind.entity.User;
import com.medifind.enums.Role;
import com.medifind.exception.BadRequestException;
import com.medifind.repository.UserRepository;
import com.medifind.security.JwtProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtProvider jwtProvider;
    @InjectMocks private AuthService authService;

    @Test
    void loginAuthenticatesAndRecordsLastLogin() {
        User user = User.builder().id(7L).firstName("Asha").lastName("Rao")
                .email("asha@example.com").password("encoded").role(Role.USER).enabled(true).build();
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                "asha@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(jwtProvider.generateToken(authentication)).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken(user.getEmail())).thenReturn("refresh-token");

        LoginRequest request = new LoginRequest();
        request.setEmail(user.getEmail());
        request.setPassword("secret-password");

        var response = authService.login(request);

        assertNotNull(user.getLastLoginAt());
        assertEquals("access-token", response.getAccessToken());
        assertEquals(Role.USER, response.getRole());
        verify(userRepository).save(user);
    }

    @Test
    void registrationRejectsAdminRole() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new-admin@example.com");
        request.setPassword("secret-password");
        request.setRole(Role.ADMIN);

        assertThrows(BadRequestException.class, () -> authService.register(request));
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void registrationStoresEncodedPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Asha");
        request.setLastName("Rao");
        request.setEmail("asha@example.com");
        request.setPassword("secret-password");
        request.setRole(Role.USER);
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("secret-password")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtProvider.generateTokenFromEmail(request.getEmail())).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken(request.getEmail())).thenReturn("refresh-token");

        authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("bcrypt-hash", userCaptor.getValue().getPassword());
    }
}
