# Sistema de Solicitudes de Compra — Arquitectura y Diseño

**Empresa:** Juarez de Oriente S.A. de C.V.
**Stack:** Java 21 · Spring Boot 3.3 · PostgreSQL 16 · Maven
**Versión API:** v1 (`/api/v1/`)
**Última actualización:** 2026-06-24

---

## Índice

1. [Resumen ejecutivo](#1-resumen-ejecutivo)
2. [Arquitectura del sistema](#2-arquitectura-del-sistema)
3. [Estructura de módulos](#3-estructura-de-módulos)
4. [Modelo de datos](#4-modelo-de-datos)
5. [Seguridad](#5-seguridad)
6. [API Reference](#6-api-reference)
7. [Cómo desarrollar](#7-cómo-desarrollar)
8. [Cómo debuggear](#8-cómo-debuggear)
9. [Testing](#9-testing)
10. [Deployment](#10-deployment)
11. [Decisiones de diseño](#11-decisiones-de-diseño)
12. [Roadmap](#12-roadmap)

---

## 1. Resumen ejecutivo

Sistema web que reemplaza una aplicación VB6/Access para gestionar solicitudes de compra (pedidos a proveedores). Migra ~700 solicitudes históricas y opera para ~50 usuarios concurrentes en red interna.

**Problema resuelto:** el sistema legacy almacenaba contraseñas en texto plano, sin relaciones en BD, folio susceptible a concurrencia, sin auditoría y sin acceso remoto.

---

## 2. Arquitectura del sistema

### Vista de componentes

```
Navegador/Postman
      │
      ▼
┌─────────────────────────────────────────────────────────────┐
│  Reverse Proxy (Nginx/Caddy) — HTTPS, headers de seguridad  │
└───────────────────────────┬─────────────────────────────────┘
                            │
              ┌─────────────▼──────────────┐
              │   Spring Boot (puerto 8081)  │
              │                              │
              │  ┌────────────────────────┐  │
              │  │  Filtros HTTP           │  │
              │  │  CorrelationIdFilter    │  │
              │  │  RequestLoggingFilter   │  │
              │  │  JwtAuthenticationFilter│  │
              │  └──────────┬─────────────┘  │
              │             │                 │
              │  ┌──────────▼─────────────┐  │
              │  │  Controllers (REST)     │  │
              │  │  /api/v1/auth           │  │
              │  │  /api/v1/users          │  │
              │  │  /api/v1/suppliers      │  │
              │  │  /api/v1/workers        │  │
              │  │  /api/v1/requests       │  │
              │  │  /api/v1/audit-events   │  │
              │  └──────────┬─────────────┘  │
              │             │                 │
              │  ┌──────────▼─────────────┐  │
              │  │  Application (Services) │  │
              │  │  Domain Events          │  │
              │  │  Use Cases              │  │
              │  └──────────┬─────────────┘  │
              │             │                 │
              │  ┌──────────▼─────────────┐  │
              │  │  Domain                 │  │
              │  │  Aggregates + Entities  │  │
              │  │  Domain Events          │  │
              │  └──────────┬─────────────┘  │
              │             │                 │
              │  ┌──────────▼─────────────┐  │
              │  │  Infrastructure         │  │
              │  │  JPA / Repositories     │  │
              │  │  Caffeine Cache         │  │
              │  └──────────┬─────────────┘  │
              └─────────────┼────────────────┘
                            │
              ┌─────────────▼──────────────┐
              │     PostgreSQL 16            │
              │     (solicitudes DB)         │
              └──────────────────────────────┘
```

### Patrones aplicados

| Patrón | Dónde | Por qué |
|---|---|---|
| **Clean Architecture (Hexagonal)** | Todos los módulos | Dominio independiente de Spring/JPA; testeable sin BD |
| **Ports & Adapters** | Repositories | `domain/port/XxxRepository` define el contrato; JPA lo implementa |
| **Domain Events (Observer)** | Aggregates → AuditListener | Los servicios publican eventos sin conocer a los listeners |
| **tokenVersion** | User → JWT → Filter | Invalidación inmediata de sesiones sin blacklist en memoria |
| **Refresh Token Rotation** | RefreshTokenService | Un token solo se usa una vez; el reúso detecta robo y cierra todo |

---

## 3. Estructura de módulos

```
src/main/java/mx/juarezdeoriente/solicitudes/
│
├── shared/                    ← Clases base sin dependencias de negocio
│   ├── domain/model/
│   │   ├── AggregateRoot.java     # Acumula domain events
│   │   ├── DomainEvent.java       # Base de todos los eventos
│   │   └── PageResult.java        # Paginación independiente de Spring
│   ├── domain/exception/
│   │   ├── DomainException.java   → 422
│   │   ├── NotFoundException.java → 404
│   │   └── ConflictException.java → 409
│   └── infrastructure/web/
│       ├── ApiResponse.java       # Envoltorio { data, error, details }
│       ├── GlobalExceptionHandler # Manejo centralizado de errores
│       └── PageableDefaults       # Cap máximo de paginación
│
├── auth/                      ← Autenticación, usuarios y tokens
├── suppliers/                 ← Catálogo de proveedores
├── workers/                   ← Catálogo de trabajadores
├── requests/                  ← Solicitudes de compra (núcleo)
├── documents/                 ← Generación de PDF
├── audit/                     ← Bitácora inmutable
│
└── config/                    ← Configuraciones transversales
    ├── AsyncConfig.java       # @EnableAsync + @EnableScheduling
    ├── CacheConfig.java       # @EnableCaching (Caffeine)
    ├── CorsConfig.java        # CORS configurado por entorno
    ├── CorrelationIdFilter    # X-Correlation-ID en cada petición
    ├── DataSeeder.java        # Seeds de desarrollo (primer arranque)
    └── RequestLoggingFilter   # Log de método/URI/status/duración
```

### Estructura interna de cada módulo

```
{modulo}/
├── domain/
│   ├── model/        ← Aggregate root, value objects, enums
│   ├── event/        ← Domain events (extienden DomainEvent)
│   └── port/         ← Interfaces de repositorio (output ports)
├── application/
│   ├── port/in/      ← Interfaces de casos de uso (input ports)
│   └── service/      ← Implementaciones de casos de uso
└── infrastructure/
    ├── persistence/  ← JPA entities + repositories + adapters
    └── web/
        ├── dto/      ← Request/Response records
        └── XxxController.java
```

---

## 4. Modelo de datos

### Diagrama ER simplificado

```
users (1) ──────── (N) user_roles
  │
  ├── (1) ──────── (N) requests ──── (N) request_items ──── (1) workers
  │                       │
  │                       └── (N) ──── (1) suppliers
  │
  └── (1) ──────── (N) refresh_tokens
  └── (1) ──────── (N) audit_events

suppliers (1) ──── (N) requests
workers   (1) ──── (N) request_items
```

### Tablas y responsabilidades

| Tabla | Descripción | Campos clave |
|---|---|---|
| `users` | Usuarios del sistema | `token_version` para invalidar JWT |
| `user_roles` | Roles asignados | `ADMIN, CAPTURISTA, AUTORIZADOR, AUDITOR` |
| `refresh_tokens` | Tokens de refresco | `token_hash` (SHA-256, nunca el token en claro) |
| `suppliers` | Catálogo de proveedores | `code` UNIQUE, `version` para optimistic locking |
| `workers` | Catálogo de trabajadores | `worker_type`: `SOCIO` o `EVENTUAL` |
| `requests` | Solicitudes de compra | `folio` de secuencia atómica, `status` BORRADOR/EMITIDA/CANCELADA |
| `request_items` | Renglones de solicitud | máx. 8 por solicitud, `total = qty * unit_cost` en servidor |
| `audit_events` | Bitácora inmutable | Solo INSERT, nunca UPDATE ni DELETE |

### Migraciones Flyway

| Versión | Descripción |
|---|---|
| V1 | Esquema inicial (tablas, índices, secuencia `folio_seq`) |
| V2 | Elimina el admin placeholder del SQL (el DataSeeder lo crea correctamente) |
| V3 | Agrega `version` (optimistic locking) y `updated_at` a entidades mutables |
| V4 | Agrega `token_version` a `users` y tabla `refresh_tokens` |

> **Regla:** nunca modificar una migración ya aplicada en producción. Crear V_N+1.

### Estados de una solicitud

```
BORRADOR ──► EMITIDA ──► CANCELADA
   │
   └── (folio = null)   (folio asignado atómicamente por folio_seq)
```

---

## 5. Seguridad

### Modelo de autenticación

```
Login → [access_token 15min] + [refresh_token 7días]
                │                        │
                ▼                        ▼
        Bearer en header         /api/v1/auth/refresh
                │                        │
                ▼                        ▼
        JwtAuthenticationFilter   validateAndRotate()
                │                        │
                ▼                        ▼
        Verificar tokenVersion    Revocar token usado
        (invalida si cambió)      → nuevo par de tokens
```

### Roles y permisos

| Rol | Puede hacer |
|---|---|
| `ADMIN` | Todo: usuarios, catálogos, solicitudes, auditoría |
| `CAPTURISTA` | Crear/consultar/emitir/cancelar solicitudes; gestionar catálogos |
| `AUTORIZADOR` | Consultar solicitudes asignadas (Segunda etapa) |
| `AUDITOR` | Solo lectura: solicitudes y bitácora |

### Mecanismos de seguridad implementados

- **Argon2id** para hashes de contraseña
- **JWT stateless** con claims: `iss`, `aud`, `jti`, `tv` (tokenVersion), `type`
- **tokenVersion** en User: incrementa en cambio de contraseña y desactivación → tokens anteriores inválidos
- **Refresh token rotation**: un refresh token solo se usa una vez; reúso detectado revoca TODAS las sesiones
- **Refresh tokens en BD**: hash SHA-256 almacenado, permite revocación en logout
- **CORS** configurado por entorno (`CORS_ALLOWED_ORIGINS`)
- **Security headers**: CSP, X-Frame-Options, X-Content-Type-Options, Referrer-Policy
- **Bloqueo temporal**: tras N intentos fallidos (configurable)
- **Optimistic locking** (`@Version`) en todas las entidades mutables

### Variables de entorno de seguridad

```bash
JWT_SECRET=<base64 de 256+ bits aleatorios>
JWT_EXPIRATION_MS=900000        # 15 min en producción
JWT_REFRESH_EXPIRATION_MS=604800000  # 7 días
JWT_ISSUER=solicitudes-api
JWT_AUDIENCE=solicitudes-client
CORS_ALLOWED_ORIGINS=https://solicitudes.empresa.local
```

---

## 6. API Reference

Base URL: `http://localhost:8081/api/v1`
Documentación interactiva: `http://localhost:8081/swagger-ui.html`

### Auth

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/auth/login` | Retorna access_token + refresh_token |
| POST | `/auth/refresh` | Rota el refresh token, retorna nuevo par |
| POST | `/auth/logout` | Revoca todos los refresh tokens del usuario |
| GET  | `/auth/me` | Datos del usuario autenticado |
| POST | `/auth/change-password` | Cambia contraseña + revoca refresh tokens |

### Respuesta estándar

```json
// Éxito
{ "data": { ... } }

// Error de validación
{ "error": "Hay campos requeridos...", "details": ["campo1 es obligatorio", ...] }

// Error de negocio
{ "error": "La solicitud debe tener al menos un renglón antes de emitirse" }
```

### Códigos HTTP utilizados

| Código | Cuándo |
|---|---|
| 200 | OK |
| 201 | Recurso creado |
| 400 | Validación fallida o parámetro inválido |
| 401 | Sin token / token expirado / credenciales incorrectas |
| 403 | Autenticado pero sin el rol necesario |
| 404 | Recurso no encontrado |
| 409 | Conflicto (clave duplicada, modificación concurrente) |
| 422 | Regla de negocio violada |
| 500 | Error interno (ver logs con correlationId) |

---

## 7. Cómo desarrollar

### Requisitos

- Java 21 (`JAVA_HOME` apuntando a `jdk-21.0.10`)
- Maven 3.9+
- PostgreSQL 16+ corriendo localmente

### Arranque

```bash
# 1. Crear archivo de credenciales locales (una sola vez)
# src/main/resources/application-local.yml
spring:
  datasource:
    password: tu_password_local
app:
  jwt:
    expiration-ms: 3600000  # 1h en local, 15min en producción

# 2. Ejecutar
mvn spring-boot:run
```

### Añadir un nuevo módulo

Sigue exactamente esta estructura (ejemplo: módulo `approvals`):

```
src/main/java/.../approvals/
├── domain/
│   ├── model/Approval.java          # extends AggregateRoot
│   ├── event/ApprovalCreatedEvent.java  # extends DomainEvent
│   └── port/ApprovalRepository.java
├── application/
│   ├── port/in/CreateApprovalUseCase.java
│   └── service/ApprovalService.java
└── infrastructure/
    ├── persistence/
    │   ├── ApprovalJpaEntity.java
    │   ├── ApprovalJpaRepository.java
    │   └── ApprovalRepositoryAdapter.java  # @Component, package-private
    └── web/
        ├── dto/ApprovalRequest.java
        ├── dto/ApprovalResponse.java
        └── ApprovalController.java
```

**Checklist al crear un módulo:**

- [ ] `AggregateRoot` extendido en el modelo de dominio
- [ ] `DomainEvent` registrado con `registerEvent(...)` en cada operación
- [ ] Puerto de repositorio como interfaz en `domain/port/`
- [ ] Adaptador JPA en `infrastructure/persistence/` con visibilidad de paquete
- [ ] Migración Flyway `V_N__descripcion.sql` para nuevas tablas
- [ ] `@PreAuthorize` en cada endpoint del controller
- [ ] Listener en `DomainEventAuditListener` para auditar el nuevo evento
- [ ] Tests unitarios del dominio en `src/test/`

### Añadir un campo a una entidad existente

1. Crear nueva migración: `V_N__add_campo_a_tabla.sql`
2. Agregar campo en `XxxJpaEntity.java` (getter + setter)
3. Agregar campo en el modelo de dominio (si aplica)
4. Actualizar `toDomain()` y `save()` en el adapter
5. Actualizar DTO de response si debe exponerse

---

## 8. Cómo debuggear

### Identificar una petición en los logs

Cada petición tiene un `correlationId` único en los logs y en el header `X-Correlation-ID` de la respuesta:

```
2026-06-24 15:32:11 [A3F9B2C1] INFO  ...RequestLoggingFilter - POST /api/v1/requests -> 422 (45ms)
2026-06-24 15:32:11 [A3F9B2C1] ERROR ...GlobalExceptionHandler - La solicitud debe tener al menos un renglón
```

### Errores comunes

| Síntoma | Causa probable | Solución |
|---|---|---|
| `401` en todos los endpoints | Token expirado (15 min en prod) | Usar `/auth/refresh` o volver a hacer login |
| `401` después de cambiar contraseña | `tokenVersion` incrementó | Volver a hacer login — comportamiento correcto |
| `409` en update | Otro usuario modificó el registro | Leer de nuevo y reintentar |
| `422` al emitir | Sin renglones o en estado incorrecto | Verificar estado y agregar items |
| `500` con `lower(bytea)` | Query JPQL con parámetro null de UUID | Usar `Specification` en vez de `@Query` |
| `Flyway checksum mismatch` | Se modificó una migración ya aplicada | Crear V_N+1, NUNCA editar migraciones aplicadas |
| Seeds no crean usuarios | `admin` ya existía en BD | Verificar tabla `users`; borrar si es placeholder |

### Activar más logs en local

En `application-local.yml`:
```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.orm.jdbc.bind: TRACE
    org.springframework.security: DEBUG
```

### Consultar auditoría directamente

```sql
-- Últimas 20 acciones
SELECT actor_id, action, entity_type, entity_id, changes, occurred_at
FROM audit_events
ORDER BY occurred_at DESC
LIMIT 20;

-- Acciones de un usuario específico
SELECT * FROM audit_events
WHERE actor_id = 'uuid-del-usuario'
ORDER BY occurred_at DESC;

-- Historial de una solicitud
SELECT * FROM audit_events
WHERE entity_type = 'Request' AND entity_id = 'uuid-de-la-solicitud';
```

---

## 9. Testing

### Estrategia de pruebas

```
         ╔══════════════╗
         ║  E2E (Playwright — Segunda etapa)  ║  ← Flujo completo en navegador
         ╚══════════════╝
     ╔═══════════════════════╗
     ║  Integración (Testcontainers)  ║  ← BD real, servicios reales
     ╚═══════════════════════╝
 ╔══════════════════════════════════╗
 ║  Controller (@WebMvcTest)        ║  ← HTTP layer + validaciones + seguridad
 ╚══════════════════════════════════╝
╔════════════════════════════════════════╗
║  Unitarias (JUnit 5 + AssertJ)         ║  ← Dominio puro, sin Spring
╚════════════════════════════════════════╝
```

### Comandos

```bash
# Todos los tests (excepto integración que requiere Docker)
mvn test

# Solo tests de dominio
mvn test -Dtest=UserTest,RequestTest,RequestItemTest

# Solo tests de controller
mvn test -Dtest=AuthControllerTest,SupplierControllerTest,RequestControllerEdgeCasesTest,ApiEdgeCasesTest

# Tests de integración (requiere Docker)
mvn test -Dintegration.tests=true
```

### Cómo escribir un nuevo test de controller

```java
@WebMvcTest(MiNuevoController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, CorsConfig.class })
class MiNuevoControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean MiServicio miServicio;
    @MockBean JwtService jwtService;
    @MockBean AppUserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")      // simula usuario autenticado
    void mi_endpoint_retorna_200() throws Exception {
        when(miServicio.hacer(any())).thenReturn(resultadoMock());
        mockMvc.perform(get("/api/v1/mi-recurso"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.data.campo").value("valor"));
    }
}
```

---

## 10. Deployment

### Configuración por entorno

| Variable | Desarrollo | Producción |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/solicitudes` | URL del servidor |
| `DB_USER` | `postgres` | Usuario dedicado (no superusuario) |
| `DB_PASSWORD` | en `application-local.yml` | Secreto del gestor de secretos |
| `JWT_SECRET` | valor por defecto (advertencia en log) | 256+ bits aleatorios |
| `JWT_EXPIRATION_MS` | `3600000` (1h) | `900000` (15 min) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,...` | `https://solicitudes.empresa.local` |
| `SESSION_SECURE` | `false` | `true` |

### Servidor Windows (producción sin Docker)

```cmd
REM 1. Compilar
mvn clean package -DskipTests

REM 2. Ejecutar como servicio (NSSM o WinSW)
java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -jar target/solicitudes-backend-0.0.1-SNAPSHOT.jar

REM Variables de entorno del sistema Windows:
REM DB_PASSWORD, JWT_SECRET, CORS_ALLOWED_ORIGINS
```

### Health check

```
GET /actuator/health
→ { "status": "UP" }

GET /actuator/info
→ { "app": { "name": "Sistema de Solicitudes", "version": "1.0.0" } }
```

### Checklist de producción

- [ ] `JWT_SECRET` generado aleatoriamente (no el valor por defecto)
- [ ] `SESSION_SECURE=true`
- [ ] PostgreSQL: usuario dedicado sin superusuario
- [ ] Reverse proxy con HTTPS (Nginx/Caddy/IIS)
- [ ] Backup automático de PostgreSQL configurado y probado
- [ ] `CORS_ALLOWED_ORIGINS` apuntando solo al dominio real
- [ ] Logs rotados y monitoreados
- [ ] Firewall: solo exponer puerto 443 (o 80 → redirige a 443)

---

## 11. Decisiones de diseño

### ¿Por qué Clean Architecture?

El dominio (reglas de negocio) no depende de Spring, JPA ni HTTP. Se puede probar con JUnit puro sin levantar el contexto de Spring, lo que hace las pruebas de dominio instantáneas.

### ¿Por qué Domain Events en lugar de llamadas directas?

El módulo de solicitudes no necesita conocer que existe una bitácora de auditoría. Cuando se emite una solicitud, publica `RequestIssuedEvent`. El `DomainEventAuditListener` lo escucha de forma desacoplada. Agregar nuevos comportamientos al emitir (notificaciones, estadísticas) no requiere modificar el agregado `Request`.

### ¿Por qué tokenVersion en lugar de blacklist?

Una blacklist requiere Redis o una tabla consultada en cada petición. `tokenVersion` es una columna en `users` que ya se lee en el filtro JWT. El costo es cero en infraestructura adicional.

### ¿Por qué Specifications en lugar de @Query para búsquedas dinámicas?

`@Query` con parámetros nulos en PostgreSQL + Hibernate 6 causa errores de tipo (`bytea`, `lower(bytea)`). `Specification` construye el predicado en Java y solo agrega las condiciones de los parámetros no nulos.

### ¿Por qué folio en secuencia PostgreSQL y no en aplicación?

Una secuencia de BD es atómica por definición. Dos peticiones concurrentes no pueden recibir el mismo folio. Implementarlo en Java requeriría bloqueos distribuidos.

### ¿Por qué Argon2id y no bcrypt?

Argon2id es resistente a ataques de GPU y ASIC. BCrypt es seguro pero no tiene resistencia a memoria paralela. Spring Security 6 incluye `Argon2PasswordEncoder` sin dependencias adicionales.

---

## 12. Roadmap

### Segunda etapa (sugerida)

| Feature | Descripción | Complejidad |
|---|---|---|
| **Flujo de aprobación** | Módulo `approvals`: PENDIENTE → APROBADA / RECHAZADA | Media |
| **Adjuntos** | Subir cotizaciones, facturas o fotos a la solicitud (S3/filesystem) | Media |
| **Notificaciones por correo** | JavaMailSender al emitir/aprobar/cancelar | Baja |
| **Dashboard** | Solicitudes por período, proveedor y estado | Baja |
| **Exportación Excel/CSV** | Apache POI o OpenCSV | Baja |
| **Login corporativo** | Microsoft Entra ID (Azure AD) con OAuth2/OIDC | Alta |
| **Frontend React** | React 18 + Vite + Material UI + TanStack Query | Alta |
| **Migración Access** | ETL: staging → transformación → carga con validación | Media |

### Mejoras técnicas pendientes

| Mejora | Descripción |
|---|---|
| **Rate limiting** | ✅ Implementado — Bucket4j en `/auth/login` (5/min), `/auth/refresh` (10/min), POST general (60/min) |
| **RS256** | Migrar de HS256 a RSA para arquitecturas con múltiples servicios |
| **Redis** | Session store compartido si se despliegan múltiples instancias |
| **Playwright E2E** | Pruebas de flujo completo en navegador |
| **Idempotency keys** | ✅ Implementado — Header `Idempotency-Key` en POST /requests e /issue. Respuesta cacheada 24h en BD. |
| **Plantilla PDF versionada** | ✅ Implementado — Tabla `request_documents`: versión, checksum SHA-256, tamaño. GET /requests/:id/documents. |

---

## Glosario

| Término | Definición |
|---|---|
| **Folio** | Número consecutivo único asignado al emitir una solicitud. Nunca se reutiliza. |
| **Borrador** | Estado inicial de una solicitud. Se puede modificar libremente. |
| **Emitida** | Solicitud con folio asignado. Solo se puede cancelar. |
| **tokenVersion** | Contador en el usuario que invalida todos los JWT anteriores al incrementarse. |
| **Refresh Token Rotation** | Cada vez que se usa un refresh token, se revoca y se emite uno nuevo. |
| **Domain Event** | Notificación que el agregado publica cuando algo importante ocurrió en el dominio. |
| **Correlation ID** | ID único por petición HTTP para rastrear logs de una misma operación. |
| **Optimistic Locking** | Mecanismo que detecta si alguien más modificó un registro antes de guardar. |
