package com.ikaansh.bookstore.service;

import com.ikaansh.bookstore.dto.AuthResponse;
import com.ikaansh.bookstore.dto.LoginRequest;
import com.ikaansh.bookstore.dto.RegisterRequest;
import com.ikaansh.bookstore.entity.Customer;
import com.ikaansh.bookstore.entity.Role;
import com.ikaansh.bookstore.exception.DuplicateResourceException;
import com.ikaansh.bookstore.repository.CustomerRepository;
import com.ikaansh.bookstore.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(CustomerRepository customerRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /** Public sign-up. Always creates a CUSTOMER; admins are created by an existing admin. */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (customerRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("A customer with email " + email + " already exists");
        }
        Customer c = new Customer();
        c.setName(req.name().trim());
        c.setEmail(email);
        c.setPassword(passwordEncoder.encode(req.password()));
        c.setRole(Role.CUSTOMER);
        c = customerRepository.save(c);
        return toAuthResponse(c);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        // Throws BadCredentialsException (-> 401) when email/password are wrong.
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.password()));
        Customer c = customerRepository.findByEmail(email).orElseThrow();
        return toAuthResponse(c);
    }

    private AuthResponse toAuthResponse(Customer c) {
        return new AuthResponse(jwtService.generateToken(c.getEmail(), c.getRole().name()),
                "Bearer", c.getId(), c.getEmail(), c.getRole());
    }
}
