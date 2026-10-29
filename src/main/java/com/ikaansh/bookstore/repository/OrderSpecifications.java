package com.ikaansh.bookstore.repository;

import com.ikaansh.bookstore.entity.CustomerOrder;
import com.ikaansh.bookstore.entity.OrderStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class OrderSpecifications {

    private OrderSpecifications() { }

    public static Specification<CustomerOrder> filter(Long customerId, OrderStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customer").<Long>get("id"), customerId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.<OrderStatus>get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
