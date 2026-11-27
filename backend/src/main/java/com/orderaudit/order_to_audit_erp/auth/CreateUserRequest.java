package com.orderaudit.order_to_audit_erp.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9_.-]+$",
                 message = "may contain only letters, digits, underscore, dot and hyphen")
        String username,

        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        @NotNull
        Role role) {
}