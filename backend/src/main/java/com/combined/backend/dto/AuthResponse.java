package com.combined.backend.dto;

public class AuthResponse {
    private String token;

    // Конструктор без аргументов для Jackson
    public AuthResponse() {
    }

    public AuthResponse(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
