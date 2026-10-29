package com.ikaansh.bookstore.repository;

import com.ikaansh.bookstore.entity.Book;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Builds the WHERE clause for GET /api/books from whichever filters were supplied. */
public final class BookSpecifications {

    private BookSpecifications() { }

    public static Specification<Book> filter(String title, String author, String category,
                                             BigDecimal minPrice, BigDecimal maxPrice, Boolean inStock) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(title)) {
                predicates.add(cb.like(cb.lower(root.<String>get("title")), "%" + title.trim().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(author)) {
                predicates.add(cb.like(cb.lower(root.<String>get("author")), "%" + author.trim().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(category)) {
                predicates.add(cb.equal(cb.lower(root.<String>get("category")), category.trim().toLowerCase()));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("price"), maxPrice));
            }
            if (inStock != null) {
                predicates.add(inStock
                        ? cb.greaterThan(root.<Integer>get("stock"), 0)
                        : cb.equal(root.<Integer>get("stock"), 0));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
