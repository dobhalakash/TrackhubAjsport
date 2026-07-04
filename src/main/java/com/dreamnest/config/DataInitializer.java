package com.dreamnest.config;

import com.dreamnest.entity.Role;
import com.dreamnest.entity.User;
import com.dreamnest.enums.RoleName;
import com.dreamnest.repository.RoleRepository;
import com.dreamnest.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds default roles, and a default super admin account, at application startup.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final String DEFAULT_ADMIN_EMAIL = "admin@trackhub.com";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@123";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        for (RoleName roleName : RoleName.values()) {
            roleRepository.findByName(roleName)
                    .orElseGet(() -> roleRepository.save(new Role(roleName)));
        }

        if (!userRepository.existsByEmail(DEFAULT_ADMIN_EMAIL)) {
            Role superAdminRole = roleRepository.findByName(RoleName.SUPER_ADMIN)
                    .orElseThrow(() -> new IllegalStateException("SUPER_ADMIN role not found"));

            User admin = new User(
                    "TrackHub",
                    "Admin",
                    DEFAULT_ADMIN_EMAIL,
                    "9999999999",
                    passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD),
                    superAdminRole
            );
            userRepository.save(admin);
        }
    }
}
