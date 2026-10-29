package com.ikaansh.bookstore.service;

import com.ikaansh.bookstore.dto.CustomerRequest;
import com.ikaansh.bookstore.dto.CustomerResponse;
import com.ikaansh.bookstore.entity.Customer;
import com.ikaansh.bookstore.entity.Role;
import com.ikaansh.bookstore.exception.BadRequestException;
import com.ikaansh.bookstore.exception.ConflictException;
import com.ikaansh.bookstore.exception.DuplicateResourceException;
import com.ikaansh.bookstore.exception.ResourceNotFoundException;
import com.ikaansh.bookstore.mapper.Mappers;
import com.ikaansh.bookstore.repository.CustomerRepository;
import com.ikaansh.bookstore.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepository customerRepository, OrderRepository orderRepository,
                           PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CustomerResponse create(CustomerRequest req) {
        if (req.password() == null || req.password().isBlank()) {
            throw new BadRequestException("password is required when creating a customer");
        }
        String email = normalize(req.email());
        if (customerRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("A customer with email " + email + " already exists");
        }
        Customer c = new Customer();
        c.setName(req.name().trim());
        c.setEmail(email);
        c.setPassword(passwordEncoder.encode(req.password()));
        c.setRole(req.role() == null ? Role.CUSTOMER : req.role());
        return Mappers.toResponse(customerRepository.save(c));
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(Long id) {
        return Mappers.toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> list(Pageable pageable) {
        return customerRepository.findAll(pageable).map(x -> Mappers.toResponse(x));
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest req) {
        Customer c = find(id);
        String email = normalize(req.email());
        if (customerRepository.existsByEmailAndIdNot(email, id)) {
            throw new DuplicateResourceException("A customer with email " + email + " already exists");
        }
        c.setName(req.name().trim());
        c.setEmail(email);
        if (req.password() != null && !req.password().isBlank()) {
            c.setPassword(passwordEncoder.encode(req.password()));
        }
        if (req.role() != null) {
            c.setRole(req.role());
        }
        return Mappers.toResponse(customerRepository.save(c));
    }

    @Transactional
    public void delete(Long id) {
        Customer c = find(id);
        if (orderRepository.existsByCustomerId(id)) {
            throw new ConflictException("Customer " + id + " has orders and cannot be deleted");
        }
        customerRepository.delete(c);
    }

    private Customer find(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
