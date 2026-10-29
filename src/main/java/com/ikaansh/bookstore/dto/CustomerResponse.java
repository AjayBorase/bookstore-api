package com.ikaansh.bookstore.dto;

import com.ikaansh.bookstore.entity.Role;
import java.time.Instant;

public record CustomerResponse(Long id, String name, String email, Role role, Instant createdAt) { }
