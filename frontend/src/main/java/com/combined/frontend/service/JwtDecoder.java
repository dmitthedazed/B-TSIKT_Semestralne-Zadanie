package com.combined.frontend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Map;

public class JwtDecoder {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Long getUserIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid JWT token format.");
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            Map<String, Object> claims = objectMapper.readValue(payload, Map.class);
            return ((Number) claims.get("userId")).longValue();
        } catch (Exception e) {
            System.err.println("Error decoding JWT userId: " + e.getMessage());
            return null;
        }
    }

    public static String getNicknameFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid JWT token format.");
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            Map<String, Object> claims = objectMapper.readValue(payload, Map.class);
            return (String) claims.get("nickname");
        } catch (Exception e) {
            System.err.println("Error decoding JWT nickname: " + e.getMessage());
            return null;
        }
    }
}
