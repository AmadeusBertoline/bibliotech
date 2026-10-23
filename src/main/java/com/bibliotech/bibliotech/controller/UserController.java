package com.bibliotech.bibliotech.controller;

import com.bibliotech.bibliotech.dto.CreateUserRequest;
import com.bibliotech.bibliotech.dto.UserResponse;
import com.bibliotech.bibliotech.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Usuários", description = "Apenas ADMIN")
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Cria um usuário (ADMIN ou LIBRARIAN)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }
}