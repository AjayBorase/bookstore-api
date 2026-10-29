package com.ikaansh.bookstore.config;

import com.ikaansh.bookstore.entity.Book;
import com.ikaansh.bookstore.entity.Customer;
import com.ikaansh.bookstore.entity.Role;
import com.ikaansh.bookstore.repository.BookRepository;
import com.ikaansh.bookstore.repository.CustomerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeds demo data so the API is usable immediately after start-up.
 *   admin@bookstore.com / Admin@123      (ADMIN)
 *   john@example.com    / Customer@123   (CUSTOMER)
 * Disable with app.seed.enabled=false.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final BookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(CustomerRepository customerRepository, BookRepository bookRepository,
                           PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.bookRepository = bookRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!customerRepository.existsByEmail("admin@bookstore.com")) {
            customerRepository.save(user("Store Admin", "admin@bookstore.com", "Admin@123", Role.ADMIN));
        }
        if (!customerRepository.existsByEmail("john@example.com")) {
            customerRepository.save(user("John Doe", "john@example.com", "Customer@123", Role.CUSTOMER));
        }
        if (bookRepository.count() == 0) {
            bookRepository.save(book("Effective Java", "Joshua Bloch", "9780134685991", "45.00", 20, "Programming"));
            bookRepository.save(book("Clean Code", "Robert C. Martin", "9780132350884", "40.00", 15, "Programming"));
            bookRepository.save(book("Spring in Action", "Craig Walls", "9781617297571", "42.50", 10, "Programming"));
            bookRepository.save(book("The Pragmatic Programmer", "Andrew Hunt", "9780135957059", "39.99", 8, "Programming"));
            bookRepository.save(book("Sapiens", "Yuval Noah Harari", "9780062316097", "18.00", 25, "History"));
            bookRepository.save(book("Atomic Habits", "James Clear", "9780735211292", "16.50", 0, "Self-help"));
        }
    }

    private Customer user(String name, String email, String rawPassword, Role role) {
        Customer c = new Customer();
        c.setName(name);
        c.setEmail(email);
        c.setPassword(passwordEncoder.encode(rawPassword));
        c.setRole(role);
        return c;
    }

    private Book book(String title, String author, String isbn, String price, int stock, String category) {
        Book b = new Book();
        b.setTitle(title);
        b.setAuthor(author);
        b.setIsbn(isbn);
        b.setPrice(new BigDecimal(price));
        b.setStock(stock);
        b.setCategory(category);
        return b;
    }
}
