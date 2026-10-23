package com.bibliotech.bibliotech.controller;

import com.bibliotech.bibliotech.dto.LoginRequest;
import com.bibliotech.bibliotech.dto.TokenResponse;
import com.bibliotech.bibliotech.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autenticação")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Autentica e retorna um JWT", security = {})
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}