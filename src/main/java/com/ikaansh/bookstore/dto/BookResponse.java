package com.ikaansh.bookstore.dto;

import java.math.BigDecimal;

public record BookResponse(Long id, String title, String author, String isbn,
                           BigDecimal price, int stock, String category) { }
