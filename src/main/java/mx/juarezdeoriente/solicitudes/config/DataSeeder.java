package mx.juarezdeoriente.solicitudes.config;
import mx.juarezdeoriente.solicitudes.modules.users.application.UserEvents;

import mx.juarezdeoriente.solicitudes.modules.users.domain.Role;
import mx.juarezdeoriente.solicitudes.modules.users.domain.User;
import mx.juarezdeoriente.solicitudes.modules.users.infrastructure.UserRepository;
import mx.juarezdeoriente.solicitudes.modules.requests.application.RequestService;
import mx.juarezdeoriente.solicitudes.modules.suppliers.application.SupplierService;
import mx.juarezdeoriente.solicitudes.modules.suppliers.domain.Supplier;
import mx.juarezdeoriente.solicitudes.modules.workers.application.WorkerService;
import mx.juarezdeoriente.solicitudes.modules.workers.domain.Worker;
import mx.juarezdeoriente.solicitudes.modules.workers.domain.WorkerType;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Component
@Profile("!test")
@Order(1)
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Value("${app.seeds.admin-password:Admin123!}")
    private String adminPassword;

    @Value("${app.seeds.capturista-password:Capturista1!}")
    private String capturistaPassword;

    /**
     * true  → local:      usuarios + proveedores + trabajadores + solicitudes de muestra
     * false → producción: usuarios + proveedores + trabajadores (sin solicitudes)
     */
    @Value("${app.seeds.demo-data:true}")
    private boolean demoData;

    private final UserRepository             userRepository;
    private final SupplierService            supplierService;
    private final WorkerService              workerService;
    private final RequestService             requestService;
    private final PasswordEncoder            passwordEncoder;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public DataSeeder(UserRepository userRepository,
                      SupplierService supplierService,
                      WorkerService workerService,
                      RequestService requestService,
                      PasswordEncoder passwordEncoder,
                      org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.userRepository  = userRepository;
        this.supplierService = supplierService;
        this.workerService   = workerService;
        this.requestService  = requestService;
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
        User admin      = seedUsers();
        Supplier prov   = seedSuppliers();
        Worker worker   = seedWorkers();

        if (demoData) {
            seedRequests(admin, prov, worker);
        } else {
            log.info("  Modo producción: solicitudes de muestra omitidas.");
        }
        log.info("Seeds aplicados correctamente.");
    }

    private User seedUsers() {
        User admin = User.create(
                "admin",
                passwordEncoder.encode(adminPassword),
                "Administrador del Sistema",
                "773-785-0497",
                Set.of(Role.ADMIN)
        );
        userRepository.save(admin);
        eventPublisher.publishEvent(new UserEvents.Created(admin.getId(), admin.getUsername()));

        User capturista = User.create(
                "capturista",
                passwordEncoder.encode(capturistaPassword),
                "Capturista de Prueba",
                null,
                Set.of(Role.CAPTURISTA)
        );
        userRepository.save(capturista);
        eventPublisher.publishEvent(new UserEvents.Created(capturista.getId(), capturista.getUsername()));

        log.info("  Usuarios admin y capturista creados.");
        return admin;
    }

    private Supplier seedSuppliers() {
        Supplier s = supplierService.create("PROV-001", "UNION SERVICIOS PROFESIONALES",     "773-100-0001", null);
        supplierService.create("PROV-002", "MATERIALES Y SUMINISTROS DEL VALLE", "773-100-0002", null);
        supplierService.create("PROV-003", "FERRETERIA INDUSTRIAL HIDALGO",      "773-100-0003", null);
        log.info("  Proveedores creados.");
        return s;
    }

    private Worker seedWorkers() {
        Worker w = workerService.create("EMP-001", "0001", "JUAN CARLOS HERNANDEZ LOPEZ",  "773-200-0001", WorkerType.SOCIO, null);
        workerService.create("EMP-002", "0002", "MARIA GUADALUPE REYES TORRES", "773-200-0002", WorkerType.SOCIO, null);
        workerService.create("EMP-003", "0003", "PEDRO MARTINEZ SANCHEZ",       null,           WorkerType.EVENTUAL, null);
        log.info("  Trabajadores creados.");
        return w;
    }

    private void seedRequests(User admin, Supplier supplier, Worker worker) {
        requestService.create(
                supplier.getId(), worker.getId(),
                "Para que UNION SERVICIOS PROFESIONALES pueda suministrar papelería de oficina",
                "Administrador del Sistema",
                admin.getId(),
                List.of(
                        new RequestService.ItemData(worker.getId(), "Resma de papel carta", BigDecimal.valueOf(10), "PZA", BigDecimal.valueOf(85.00)),
                        new RequestService.ItemData(worker.getId(), "Bolígrafos azules caja c/12", BigDecimal.valueOf(5),  "CAJA", BigDecimal.valueOf(45.00))
                )
        );
        requestService.create(
                supplier.getId(), worker.getId(),
                "Para que UNION SERVICIOS PROFESIONALES pueda suministrar material de limpieza",
                "Administrador del Sistema",
                admin.getId(),
                List.of(
                        new RequestService.ItemData(worker.getId(), "Escoba", BigDecimal.valueOf(3), "PZA", BigDecimal.valueOf(65.00)),
                        new RequestService.ItemData(worker.getId(), "Jabón líquido 1L", BigDecimal.valueOf(6), "PZA", BigDecimal.valueOf(38.50))
                )
        );
        requestService.create(
                supplier.getId(), worker.getId(),
                "Para que UNION SERVICIOS PROFESIONALES pueda suministrar equipo de cómputo",
                "Administrador del Sistema",
                admin.getId(),
                List.of(
                        new RequestService.ItemData(worker.getId(), "Mouse inalámbrico", BigDecimal.valueOf(2), "PZA", BigDecimal.valueOf(250.00))
                )
        );
        log.info("  Solicitudes de muestra creadas.");
    }
}
