package com.combined.backend.service;

import com.combined.backend.dto.PasswordChangeDto;
import com.combined.backend.dto.ProfileUpdateDto;
import com.combined.backend.dto.UserDto;
import com.combined.backend.model.User;
import com.combined.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1L);
        user.setFirstName("A");
        user.setLastName("B");
        user.setNickname("nick");
        user.setEmail("a@b.com");
        user.setPasswordHash("hash");
    }

    @Test
    void registerFailsOnDuplicateEmail() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);
        UserDto dto = new UserDto(null, "A", "B", "nick", "a@b.com");
        dto.setPassword("secret1");
        dto.setConfirmPassword("secret1");
        assertThatThrownBy(() -> userService.register(dto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.CONFLICT);
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordChecksCurrentAndLength() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "hash")).thenReturn(true);
        when(passwordEncoder.encode("newpass")).thenReturn("enc");
        PasswordChangeDto dto = new PasswordChangeDto();
        dto.setCurrentPassword("old");
        dto.setNewPassword("newpass");
        dto.setConfirmPassword("newpass");
        userService.changePassword(1L, dto);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateProfileRejectsEmptyName() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        ProfileUpdateDto dto = new ProfileUpdateDto("   ", null);
        assertThatThrownBy(() -> userService.updateProfile(1L, dto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void loadUserByUsernameThrowsIfMissing() {
        when(userRepository.findByEmail("missing")).thenReturn(Optional.empty());
        when(userRepository.findByNickname("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
