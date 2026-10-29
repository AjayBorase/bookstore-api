package com.ikaansh.bookstore.dto;

import jakarta.validation.constraints.*;

public record OrderItemRequest(
        @NotNull(message = "bookId is required") Long bookId,
        @NotNull(message = "quantity is required") @Min(value = 1, message = "quantity must be at least 1") Integer quantity) { }
