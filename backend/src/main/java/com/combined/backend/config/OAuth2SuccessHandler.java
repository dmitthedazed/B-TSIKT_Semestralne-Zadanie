package com.combined.backend.config;

import com.combined.backend.model.User;
import com.combined.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OAuth2SuccessHandler(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User principal = (OAuth2User) oauthToken.getPrincipal();
        Map<String, Object> attrs = principal.getAttributes();

        String email = stringAttr(attrs, "email");
        if (email == null || email.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not provided by OAuth provider.");
            return;
        }
        String givenName = firstNonBlank(
                stringAttr(attrs, "given_name"),
                stringAttr(attrs, "name"),
                email.split("@")[0]
        );
        String familyName = firstNonBlank(
                stringAttr(attrs, "family_name"),
                ""
        );

        boolean newUser = false;
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setFirstName(capitalize(givenName));
            u.setLastName(capitalize(familyName));
            u.setNickname(generateUniqueNickname(email, givenName));
            u.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            return u;
        });
        if (user.getUserId() == null) {
            user = userRepository.save(user);
            newUser = true;
        }

        String jwt = jwtUtil.generateToken(user);

        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "token", jwt,
                "email", user.getEmail(),
                "nickname", user.getNickname(),
                "firstName", user.getFirstName(),
                "lastName", Optional.ofNullable(user.getLastName()).orElse(""),
                "newUser", newUser
        )));
    }

    private String stringAttr(Map<String, Object> attrs, String key) {
        Object val = attrs.get(key);
        return val == null ? null : val.toString();
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return "";
    }

    private String capitalize(String input) {
        if (input == null || input.isBlank()) return input;
        String trimmed = input.trim();
        return trimmed.substring(0, 1).toUpperCase(Locale.ROOT) + trimmed.substring(1);
    }

    private String generateUniqueNickname(String email, String base) {
        String prefix = !base.isBlank() ? base : email.split("@")[0];
        String candidate = prefix.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        int suffix = 1;
        String nick = candidate;
        while (userRepository.existsByNickname(nick)) {
            nick = candidate + suffix;
            suffix++;
        }
        return nick;
    }
}
