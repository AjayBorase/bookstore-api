package com.ikaansh.bookstore.dto;

import com.ikaansh.bookstore.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id, Long customerId, String customerName, OrderStatus status,
                            BigDecimal totalAmount, Instant createdAt, List<OrderItemResponse> items) { }
