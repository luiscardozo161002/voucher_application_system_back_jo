package mx.juarezdeoriente.solicitudes.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita @Async (listeners de auditoría) y @Scheduled (limpieza de tokens expirados).
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {}
