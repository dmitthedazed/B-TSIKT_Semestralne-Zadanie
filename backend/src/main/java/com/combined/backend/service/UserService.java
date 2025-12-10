package com.combined.backend.service;

import com.combined.backend.config.CustomUserDetails;
import com.combined.backend.dto.UserDto;
import com.combined.backend.model.User;
import com.combined.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserDto register(UserDto userDto) {
        System.out.println("UserService: Starting registration for email: " + userDto.getEmail());

        if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User with email " + userDto.getEmail() + " already exists");
        }
        if (userRepository.existsByNickname(userDto.getNickname())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User with nickname " + userDto.getNickname() + " already exists");
        }

        System.out.println("UserService: Creating new User entity...");
        User user = new User();
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setNickname(userDto.getNickname());
        user.setEmail(userDto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(userDto.getPassword()));

        User savedUser;
        try {
            System.out.println("UserService: Saving user to repository...");
            savedUser = userRepository.save(user);
            System.out.println("UserService: User saved with ID: " + savedUser.getUserId());
        } catch (Exception e) {
            System.err.println("UserService: Error saving user: " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error saving user: " + e.getMessage(), e);
        }

        System.out.println("UserService: Converting saved user to DTO...");
        UserDto resultDto = toDto(savedUser);
        System.out.println("UserService: Registration process completed for email: " + userDto.getEmail());
        return resultDto;
    }

    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    public User findUserByEmailOrNickname(String identifier) {
        return userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByNickname(identifier))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email/username: " + identifier));
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        System.out.println("UserService: Attempting to load user by identifier (email or username): " + email);
        User user = findUserByEmailOrNickname(email);
        System.out.println("UserService: User found: " + user.getEmail() + ", ID: " + user.getUserId());
        return new CustomUserDetails(user.getUserId(), user.getEmail(), user.getPasswordHash(), user.getNickname());
    }

    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    }

    @Transactional
    public UserDto updateUser(Long userId, UserDto userDto) {
        User user = findUserById(userId);
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setEmail(userDto.getEmail());
        User updatedUser = userRepository.save(user);
        return toDto(updatedUser);
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getUserId(), user.getFirstName(), user.getLastName(), user.getNickname(), user.getEmail());
    }
}
