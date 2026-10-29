package com.ikaansh.bookstore.controller;

import com.ikaansh.bookstore.dto.OrderRequest;
import com.ikaansh.bookstore.dto.OrderResponse;
import com.ikaansh.bookstore.dto.PageResponse;
import com.ikaansh.bookstore.entity.OrderStatus;
import com.ikaansh.bookstore.security.CurrentUser;
import com.ikaansh.bookstore.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final CurrentUser currentUser;

    public OrderController(OrderService orderService, CurrentUser currentUser) {
        this.orderService = orderService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse place(@Valid @RequestBody OrderRequest request, Authentication auth) {
        return orderService.placeOrder(request, currentUser.requesterId(auth));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id, Authentication auth) {
        return orderService.getOrder(id, currentUser.requesterId(auth));
    }

    /** GET /api/orders?customerId=1&status=PLACED&page=0&size=10&sort=createdAt,desc (customers only ever see their own) */
    @GetMapping
    public PageResponse<OrderResponse> list(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication auth) {
        return PageResponse.of(orderService.listOrders(customerId, status, currentUser.requesterId(auth), pageable));
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id, Authentication auth) {
        return orderService.cancelOrder(id, currentUser.requesterId(auth));
    }
}
