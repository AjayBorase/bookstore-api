package com.ikaansh.bookstore.repository;

import com.ikaansh.bookstore.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    boolean existsByBookId(Long bookId);
}
