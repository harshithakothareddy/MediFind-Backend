package com.medifind.security;

import com.medifind.entity.User;
import com.medifind.enums.Role;
import com.medifind.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private UserDetailsServiceImpl userDetailsService;

    @Test
    void mapsDatabaseRoleToSpringSecurityAuthority() {
        User admin = User.builder().email("admin@example.com").password("hash")
                .role(Role.ADMIN).enabled(true).build();
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));

        var details = userDetailsService.loadUserByUsername(admin.getEmail());

        assertTrue(details.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(details.isEnabled());
    }

    @Test
    void rejectsUnknownEmail() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("missing@example.com"));
    }
}
