package com.ikaansh.bookstore.service;

import com.ikaansh.bookstore.dto.BookRequest;
import com.ikaansh.bookstore.dto.BookResponse;
import com.ikaansh.bookstore.entity.Book;
import com.ikaansh.bookstore.exception.ConflictException;
import com.ikaansh.bookstore.exception.DuplicateResourceException;
import com.ikaansh.bookstore.exception.ResourceNotFoundException;
import com.ikaansh.bookstore.mapper.Mappers;
import com.ikaansh.bookstore.repository.BookRepository;
import com.ikaansh.bookstore.repository.BookSpecifications;
import com.ikaansh.bookstore.repository.OrderItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final OrderItemRepository orderItemRepository;

    public BookService(BookRepository bookRepository, OrderItemRepository orderItemRepository) {
        this.bookRepository = bookRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public BookResponse create(BookRequest req) {
        String isbn = req.isbn().trim();
        if (bookRepository.existsByIsbn(isbn)) {
            throw new DuplicateResourceException("A book with ISBN " + isbn + " already exists");
        }
        Book book = new Book();
        apply(book, req);
        return Mappers.toResponse(bookRepository.save(book));
    }

    @Transactional(readOnly = true)
    public BookResponse get(Long id) {
        return Mappers.toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public Page<BookResponse> list(String title, String author, String category,
                                   BigDecimal minPrice, BigDecimal maxPrice, Boolean inStock, Pageable pageable) {
        return bookRepository
                .findAll(BookSpecifications.filter(title, author, category, minPrice, maxPrice, inStock), pageable)
                .map(x -> Mappers.toResponse(x));
    }

    @Transactional
    public BookResponse update(Long id, BookRequest req) {
        Book book = find(id);
        String isbn = req.isbn().trim();
        if (bookRepository.existsByIsbnAndIdNot(isbn, id)) {
            throw new DuplicateResourceException("A book with ISBN " + isbn + " already exists");
        }
        apply(book, req);
        return Mappers.toResponse(bookRepository.save(book));
    }

    @Transactional
    public void delete(Long id) {
        Book book = find(id);
        if (orderItemRepository.existsByBookId(id)) {
            throw new ConflictException("Book " + id + " is part of existing orders and cannot be deleted");
        }
        bookRepository.delete(book);
    }

    private Book find(Long id) {
        return bookRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Book", id));
    }

    private void apply(Book book, BookRequest req) {
        book.setTitle(req.title().trim());
        book.setAuthor(req.author().trim());
        book.setIsbn(req.isbn().trim());
        book.setPrice(req.price());
        book.setStock(req.stock());
        book.setCategory(req.category() == null ? null : req.category().trim());
    }
}
