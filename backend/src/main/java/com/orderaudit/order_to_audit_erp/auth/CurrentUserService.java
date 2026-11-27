package com.orderaudit.order_to_audit_erp.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** The logged-in user, used to stamp created_by on ledger rows and documents. */
    public User requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user in the security context");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found: " + authentication.getName()));
    }
}
