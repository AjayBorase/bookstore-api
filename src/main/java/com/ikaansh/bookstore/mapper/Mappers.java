package com.ikaansh.bookstore.mapper;

import com.ikaansh.bookstore.dto.*;
import com.ikaansh.bookstore.entity.Book;
import com.ikaansh.bookstore.entity.Customer;
import com.ikaansh.bookstore.entity.CustomerOrder;

import java.math.BigDecimal;
import java.util.List;

/** Entity -> DTO conversion. Call inside a transaction (order mapping touches lazy associations). */
public final class Mappers {

    private Mappers() { }

    public static BookResponse toResponse(Book b) {
        return new BookResponse(b.getId(), b.getTitle(), b.getAuthor(), b.getIsbn(),
                b.getPrice(), b.getStock(), b.getCategory());
    }

    public static CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(c.getId(), c.getName(), c.getEmail(), c.getRole(), c.getCreatedAt());
    }

    public static OrderResponse toResponse(CustomerOrder o) {
        List<OrderItemResponse> items = o.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getBook().getId(),
                        i.getBook().getTitle(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))))
                .toList();
        return new OrderResponse(o.getId(), o.getCustomer().getId(), o.getCustomer().getName(),
                o.getStatus(), o.getTotalAmount(), o.getCreatedAt(), items);
    }
}
