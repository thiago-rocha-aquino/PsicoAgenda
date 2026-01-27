package com.psicoagenda.application.dto.response;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    UserInfo user
) {
    public record UserInfo(
        String id,
        String email,
        String name
    ) {}
}
