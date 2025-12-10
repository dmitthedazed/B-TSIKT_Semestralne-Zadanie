package com.combined.backend.controller;

import com.combined.backend.config.CustomUserDetails;
import com.combined.backend.dto.PasswordChangeDto;
import com.combined.backend.dto.ProfileUpdateDto;
import com.combined.backend.dto.UserDto;
import com.combined.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(userService.toDto(userService.findUserById(userDetails.getUserId())));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> updateProfile(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                 @RequestBody ProfileUpdateDto dto) {
        return ResponseEntity.ok(userService.updateProfile(userDetails.getUserId(), dto));
    }

    @PostMapping("/me/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal CustomUserDetails userDetails,
                                               @RequestBody PasswordChangeDto dto) {
        userService.changePassword(userDetails.getUserId(), dto);
        return ResponseEntity.ok().build();
    }
}
