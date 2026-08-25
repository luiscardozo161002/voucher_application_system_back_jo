package mx.juarezdeoriente.solicitudes.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Carga los datos históricos migrados desde el sistema legado (Access).
 * Se ejecuta automáticamente en el primer arranque, DESPUÉS del DataSeeder
 * (que garantiza que el usuario admin ya existe).
 *
 * Detecta si el legado ya fue importado contando los proveedores:
 * - Si hay más de 10, asume que los datos ya están cargados y omite.
 * - Si hay 10 o menos, ejecuta los tres scripts SQL del classpath.
 */
@Component
@Profile("local")
@Order(2)
public class LegacyDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyDataSeeder.class);
    private static final int    LEGACY_THRESHOLD = 10;

    @Value("${app.seeds.demo-data:true}")
    private boolean demoData;

    private final DataSource dataSource;

    public LegacyDataSeeder(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        try (Connection conn = dataSource.getConnection()) {

            long supplierCount = count(conn, "suppliers");
            if (supplierCount > LEGACY_THRESHOLD) {
                log.info("Datos del legado ya cargados ({} proveedores) — omitiendo.", supplierCount);
                return;
            }

            log.info("Cargando datos historicos del sistema legado... (demo-data={})", demoData);
            executeSql(conn, "db/legacy/01_proveedores.sql");
            log.info("  Proveedores importados.");
            executeSql(conn, "db/legacy/02_trabajadores.sql");
            log.info("  Trabajadores importados.");
            if (demoData) {
                executeSql(conn, "db/legacy/03_solicitudes.sql");
                log.info("  Solicitudes historicas importadas.");
            } else {
                log.info("  Modo produccion: solicitudes del legado omitidas.");
            }
            log.info("Migracion del legado completada.");
        }
    }

    private void executeSql(Connection conn, String classpathLocation) throws Exception {
        ClassPathResource resource = new ClassPathResource(classpathLocation);
        ScriptUtils.executeSqlScript(conn, resource);
    }

    private long count(Connection conn, String table) throws Exception {
        try (var stmt = conn.createStatement();
             var rs   = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }
}
