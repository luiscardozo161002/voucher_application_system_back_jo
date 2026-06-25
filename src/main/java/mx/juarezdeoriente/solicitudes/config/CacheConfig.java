package mx.juarezdeoriente.solicitudes.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Activa el cache en memoria (Caffeine) para catálogos de lectura frecuente.
 *
 * Caches configurados:
 * - suppliers      → resultados de búsqueda de proveedores (TTL 5 min)
 * - workers        → resultados de búsqueda de trabajadores (TTL 5 min)
 *
 * Se invalidan automáticamente al crear, actualizar o cambiar estado.
 * TTL y tamaño máximo configurables en application.yml → spring.cache.caffeine.spec
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String SUPPLIERS = "suppliers";
    public static final String WORKERS   = "workers";
}
