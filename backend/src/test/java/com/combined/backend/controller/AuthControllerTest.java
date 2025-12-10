package com.combined.backend.controller;

import com.combined.backend.config.JwtUtil;
import com.combined.backend.dto.AuthRequest;
import com.combined.backend.dto.AuthResponse;
import com.combined.backend.dto.UserDto;
import com.combined.backend.model.User;
import com.combined.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock UserService userService;
    @Mock JwtUtil jwtUtil;

    @InjectMocks
    AuthController authController;

    @Test
    void registerReturnsUserDto() {
        UserDto dto = new UserDto(1L, "A", "B", "nick", "a@b.com");
        when(userService.register(any())).thenReturn(dto);
        ResponseEntity<UserDto> res = authController.register(dto);
        assertThat(res.getBody().getEmail()).isEqualTo("a@b.com");
    }

    @Test
    void loginReturnsToken() {
        AuthRequest req = new AuthRequest();
        req.setEmail("a@b.com");
        req.setPassword("secret");
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        UserDetails ud = org.springframework.security.core.userdetails.User.withUsername("a@b.com").password("p").authorities("USER").build();
        when(auth.getPrincipal()).thenReturn(ud);
        User user = new User(); user.setUserId(1L); user.setEmail("a@b.com"); user.setNickname("nick");
        when(userService.findUserByEmailOrNickname("a@b.com")).thenReturn(user);
        when(jwtUtil.generateToken(user)).thenReturn("jwt");
        ResponseEntity<AuthResponse> res = authController.login(req);
        assertThat(res.getBody().getToken()).isEqualTo("jwt");
    }
}
