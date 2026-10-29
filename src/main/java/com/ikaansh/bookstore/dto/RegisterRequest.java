package com.ikaansh.bookstore.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank(message = "name is required") @Size(max = 255) String name,
        @NotBlank(message = "email is required") @Email(message = "email is not valid") String email,
        @NotBlank(message = "password is required") @Size(min = 6, max = 100, message = "password must be 6-100 characters") String password) { }
