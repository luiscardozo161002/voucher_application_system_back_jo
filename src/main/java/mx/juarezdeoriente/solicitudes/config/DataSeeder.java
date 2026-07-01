package mx.juarezdeoriente.solicitudes.config;

import mx.juarezdeoriente.solicitudes.auth.domain.model.Role;
import mx.juarezdeoriente.solicitudes.auth.domain.model.User;
import mx.juarezdeoriente.solicitudes.auth.domain.port.UserRepository;
import mx.juarezdeoriente.solicitudes.suppliers.application.service.SupplierService;
import mx.juarezdeoriente.solicitudes.workers.application.service.WorkerService;
import mx.juarezdeoriente.solicitudes.workers.domain.model.WorkerType;
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

/**
 * Carga datos iniciales de desarrollo en la primera ejecución.
 * Solo se ejecuta si no existen usuarios en la base de datos.
 */
@Component
@Profile("!test")
@Order(1)
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Value("${app.seeds.admin-password:Admin123!}")
    private String adminPassword;

    @Value("${app.seeds.capturista-password:Capturista1!}")
    private String capturistaPassword;

    /** false en producción: solo crea usuarios, sin datos de muestra. */
    @Value("${app.seeds.demo-data:true}")
    private boolean demoData;

    private final UserRepository             userRepository;
    private final SupplierService            supplierService;
    private final WorkerService              workerService;
    private final PasswordEncoder            passwordEncoder;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public DataSeeder(UserRepository userRepository,
                      SupplierService supplierService,
                      WorkerService workerService,
                      PasswordEncoder passwordEncoder,
                      org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.userRepository  = userRepository;
        this.supplierService = supplierService;
        this.workerService   = workerService;
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

        log.info("Aplicando seeds iniciales... (demo-data={})", demoData);
        seedUsers();
        if (demoData) {
            seedSuppliers();
            seedWorkers();
        } else {
            log.info("  Modo producción: datos de muestra omitidos.");
        }
        log.info("Seeds aplicados correctamente.");
    }

    private void seedUsers() {
        User admin = User.create(
                "admin",
                passwordEncoder.encode(adminPassword),
                "Administrador del Sistema",
                "773-785-0497",
                Set.of(Role.ADMIN)
        );
        userRepository.save(admin);
        admin.pullDomainEvents().forEach(eventPublisher::publishEvent);

        User capturista = User.create(
                "capturista",
                passwordEncoder.encode(capturistaPassword),
                "Capturista de Prueba",
                null,
                Set.of(Role.CAPTURISTA)
        );
        userRepository.save(capturista);
        capturista.pullDomainEvents().forEach(eventPublisher::publishEvent);

        log.info("  Usuarios admin y capturista creados.");
    }

    private void seedSuppliers() {
        supplierService.create("PROV-001", "UNION SERVICIOS PROFESIONALES",    "773-100-0001");
        supplierService.create("PROV-002", "MATERIALES Y SUMINISTROS DEL VALLE","773-100-0002");
        supplierService.create("PROV-003", "FERRETERIA INDUSTRIAL HIDALGO",    "773-100-0003");
        log.info("  Proveedores de muestra creados.");
    }

    private void seedWorkers() {
        workerService.create("EMP-001", "0001", "JUAN CARLOS HERNANDEZ LOPEZ",  "773-200-0001", WorkerType.SOCIO);
        workerService.create("EMP-002", "0002", "MARIA GUADALUPE REYES TORRES", "773-200-0002", WorkerType.SOCIO);
        workerService.create("EMP-003", "0003", "PEDRO MARTINEZ SANCHEZ",       null,           WorkerType.EVENTUAL);
        log.info("  Trabajadores de muestra creados.");
    }
}
