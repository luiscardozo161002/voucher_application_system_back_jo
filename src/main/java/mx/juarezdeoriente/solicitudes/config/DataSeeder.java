package mx.juarezdeoriente.solicitudes.config;
import mx.juarezdeoriente.solicitudes.modules.users.application.UserEvents;
import mx.juarezdeoriente.solicitudes.modules.users.domain.Role;
import mx.juarezdeoriente.solicitudes.modules.users.domain.User;
import mx.juarezdeoriente.solicitudes.modules.users.infrastructure.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@Profile("local")
@Order(1)
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Value("${app.seeds.admin-password:12345678}")
    private String adminPassword;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public DataSeeder(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher  = eventPublisher;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.findByUsername("admin").isPresent()) {
            log.info("Seeds ya aplicados — omitiendo.");
            return;
        }

        log.info("Aplicando seed de admin local...");
        User admin = User.create(
                "admin",
                passwordEncoder.encode(adminPassword),
                "Administrador del Sistema",
                "773-785-0497",
                Set.of(Role.ADMIN)
        );
        userRepository.save(admin);
        eventPublisher.publishEvent(new UserEvents.Created(admin.getId(), admin.getUsername()));
        log.info("  Usuario admin creado.");
    }
}
