package com.orderaudit.order_to_audit_erp.auth;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "test"})
public class DevUserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevUserSeeder.class);

    private static final Map<String, Role> DEV_ACCOUNTS = new LinkedHashMap<>();

    static {
        DEV_ACCOUNTS.put("admin", Role.ADMIN);
        DEV_ACCOUNTS.put("salesman_a", Role.SALESMAN);
        DEV_ACCOUNTS.put("salesman_b", Role.SALESMAN);
        DEV_ACCOUNTS.put("production", Role.PRODUCTION);
        DEV_ACCOUNTS.put("accountant", Role.ACCOUNTANT);
        DEV_ACCOUNTS.put("owner", Role.OWNER);
    }

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String devPassword;

    public DevUserSeeder(UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         @Value("${app.seed.dev-password:}") String devPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.devPassword = devPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed();
    }

    /** Creates only the missing accounts and returns how many were created. */
    public int seed() {
        if (devPassword.isBlank()) {
            log.warn("app.seed.dev-password is empty: dev accounts were NOT created. Set DEV_SEED_PASSWORD.");
            return 0;
        }
        int created = 0;
        for (Map.Entry<String, Role> account : DEV_ACCOUNTS.entrySet()) {
            if (!userRepository.existsByUsername(account.getKey())) {
                userRepository.save(new User(
                        account.getKey(), passwordEncoder.encode(devPassword), account.getValue()));
                created++;
            }
        }
        log.info("Dev seed finished: {} account(s) created", created);
        return created;
    }
}