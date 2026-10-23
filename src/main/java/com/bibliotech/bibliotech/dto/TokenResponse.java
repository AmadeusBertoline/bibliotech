package com.bibliotech.bibliotech.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}