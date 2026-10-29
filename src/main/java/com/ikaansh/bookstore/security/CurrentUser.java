package com.ikaansh.bookstore.security;

import com.ikaansh.bookstore.exception.ForbiddenException;
import com.ikaansh.bookstore.repository.CustomerRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Small helper to answer "who is calling?" inside controllers. */
@Component
public class CurrentUser {

    private final CustomerRepository customerRepository;

    public CurrentUser(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Returns null for admins (no restriction) or the caller's own customer id for customers.
     * Services use this "requesterId" to enforce "customers only see their own orders".
     */
    public Long requesterId(Authentication auth) {
        if (isAdmin(auth)) {
            return null;
        }
        return customerRepository.findByEmail(auth.getName())
                .map(c -> c.getId())
                .orElseThrow(() -> new ForbiddenException("Account no longer exists"));
    }
}
