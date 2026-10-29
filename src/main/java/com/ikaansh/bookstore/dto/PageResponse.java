package com.ikaansh.bookstore.dto;

import org.springframework.data.domain.Page;
import java.util.List;

/** Stable JSON shape for paged results (instead of serializing Spring's PageImpl directly). */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public static <T> PageResponse<T> of(Page<T> p) {
        return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}
