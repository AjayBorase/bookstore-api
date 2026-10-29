package com.ikaansh.bookstore.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record OrderRequest(
        @NotNull(message = "customerId is required") Long customerId,
        @NotEmpty(message = "an order needs at least one item") List<@Valid @NotNull OrderItemRequest> items) { }
