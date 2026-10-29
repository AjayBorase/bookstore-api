package com.ikaansh.bookstore.security;

import com.ikaansh.bookstore.entity.Customer;
import com.ikaansh.bookstore.repository.CustomerRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Tells Spring Security how to load a user (here: a Customer row looked up by email). */
@Service
public class CustomerUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;

    public CustomerUserDetailsService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Customer c = customerRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("No user with email " + email));
        return User.withUsername(c.getEmail())
                .password(c.getPassword())
                .roles(c.getRole().name())   // becomes authority ROLE_ADMIN / ROLE_CUSTOMER
                .build();
    }
}
