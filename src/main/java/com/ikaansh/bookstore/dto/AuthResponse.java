package com.ikaansh.bookstore.dto;

import com.ikaansh.bookstore.entity.Role;

public record AuthResponse(String token, String tokenType, Long customerId, String email, Role role) { }
