package com.combined.backend.controller;

import com.combined.backend.config.JwtUtil;
import com.combined.backend.dto.AuthRequest;
import com.combined.backend.dto.AuthResponse;
import com.combined.backend.dto.UserDto;
import com.combined.backend.model.User;
import com.combined.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager, UserService userService, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody UserDto userDto) {
        System.out.println("AuthController: Received registration request for email: " + userDto.getEmail());
        UserDto registeredUser = userService.register(userDto);
        System.out.println("AuthController: User registered successfully: " + registeredUser.getEmail());
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest) {
        System.out.println("AuthController: Received login request for email: " + authRequest.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
        );
        System.out.println("AuthController: Authentication successful for email: " + authRequest.getEmail());

        // Get UserDetails from the Authentication object
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Load the full User object to get the userId (supports email or username)
        User user = userService.findUserByEmailOrNickname(userDetails.getUsername());
        System.out.println("AuthController: User loaded for email: " + user.getEmail() + ", ID: " + user.getUserId());

        final String jwt = jwtUtil.generateToken(user); // Pass the User object
        System.out.println("AuthController: JWT generated.");

        return ResponseEntity.ok(new AuthResponse(jwt));
    }
}
