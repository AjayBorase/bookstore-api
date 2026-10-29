package com.ikaansh.bookstore.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(
        @NotBlank(message = "email is required") String email,
        @NotBlank(message = "password is required") String password) { }
