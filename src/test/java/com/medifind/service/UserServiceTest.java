package com.medifind.service;

import com.medifind.entity.User;
import com.medifind.enums.Role;
import com.medifind.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private UserService userService;

    @Test
    void repeatedActivationKeepsUserEnabled() {
        User user = User.builder().id(9L).email("user@example.com").role(Role.USER).enabled(true).build();
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.setUserEnabled(9L, true);
        userService.setUserEnabled(9L, true);

        assertTrue(user.getEnabled());
        verify(userRepository, times(2)).save(user);
    }
}
