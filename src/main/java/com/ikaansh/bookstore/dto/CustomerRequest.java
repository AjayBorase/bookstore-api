package com.ikaansh.bookstore.dto;

import com.ikaansh.bookstore.entity.Role;
import jakarta.validation.constraints.*;

/** password is required on create, optional on update (null = keep the current one). */
public record CustomerRequest(
        @NotBlank(message = "name is required") @Size(max = 255) String name,
        @NotBlank(message = "email is required") @Email(message = "email is not valid") @Size(max = 255) String email,
        @Size(min = 6, max = 100, message = "password must be 6-100 characters") String password,
        Role role) { }
