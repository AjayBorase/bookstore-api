package com.ikaansh.bookstore.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record BookRequest(
        @NotBlank(message = "title is required") @Size(max = 255) String title,
        @NotBlank(message = "author is required") @Size(max = 255) String author,
        @NotBlank(message = "isbn is required") @Size(max = 20) String isbn,
        @NotNull(message = "price is required") @DecimalMin(value = "0.01", message = "price must be at least 0.01") @Digits(integer = 8, fraction = 2) BigDecimal price,
        @NotNull(message = "stock is required") @Min(value = 0, message = "stock cannot be negative") Integer stock,
        @Size(max = 100) String category) { }
