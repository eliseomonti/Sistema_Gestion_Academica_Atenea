package com.utn.atenea.seguridad.dto;

public record TokenResponse(String accessToken, String refreshToken, long expiresIn) {
}
