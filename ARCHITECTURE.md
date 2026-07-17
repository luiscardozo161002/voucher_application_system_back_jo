# Sistema de Solicitudes de Compra — Arquitectura

**Empresa:** Juarez de Oriente S.A. de C.V.
**Stack:** Java 21 · Spring Boot 3.3 · PostgreSQL 16 · Maven
**Versión API:** v1 (`/api/v1/`)
**Última actualización:** 2026-07-16

---

## Índice

1. [Resumen del sistema](#1-resumen-del-sistema)
2. [Arquitectura](#2-arquitectura)
3. [Estructura de carpetas](#3-estructura-de-carpetas)
4. [Módulos](#4-módulos)
5. [Modelo de datos](#5-modelo-de-datos)
6. [Seguridad](#6-seguridad)
7. [API Reference](#7-api-reference)
8. [Cómo desarrollar](#8-cómo-desarrollar)
9. [Cómo debuggear](#9-cómo-debuggear)
10. [Deployment](#10-deployment)
11. [Decisiones de diseño](#11-decisiones-de-diseño)
12. [Roadmap](#12-roadmap)

---

## 1. Resumen del sistema

Sistema web que reemplaza una aplicación VB6/Access para gestionar solicitudes de compra (pedidos a proveedores). Migra ~700 solicitudes históricas y opera para ~50 usuarios concurrentes en red interna.

**Problema resuelto:** el sistema legado almacenaba contraseñas en texto plano, sin relaciones en BD, folio susceptible a concurrencia, sin auditoría y sin acceso remoto.

---

## 2. Arquitectura

### Vista de componentes

```
Navegador / Postman
      │
      ▼
┌─────────────────────────────────────────────────────────────┐
│  Reverse Proxy (Nginx/Caddy) — HTTPS, headers de seguridad  │
└───────────────────────────┬─────────────────────────────────┘
                            │
              ┌─────────────▼──────────────┐
              │   Spring Boot (puerto 8081)  │
              │                              │
              │  presentation/               │
              │  └── controllers REST        │
              │      /api/v1/auth            │
              │      /api/v1/users           │
              │      /api/v1/suppliers       │
              │      /api/v1/workers         │
              │      /api/v1/requests        │
              │      /api/v1/audit-events    │
              │                              │
              │  application/                │
              │  └── services + events       │
              │                              │
              │  domain/                     │
              │  └── entities + reglas       │
              │                              │
              │  infrastructure/             │
              │  └── JPA + Caffeine cache    │
              └─────────────┬────────────────┘
                            │
              ┌─────────────▼──────────────┐
              │     PostgreSQL 16            │
              └──────────────────────────────┘
```

### Patrones aplicados

| Patrón | Dónde | Por qué |
|---|---|---|
| **Modular Monolith** | `modules/` | Cada módulo es independiente; se puede extraer a microservicio |
| **Domain / Application / Infrastructure** | Dentro de cada módulo | Separación de responsabilidades clara por capa |
| **Domain Events** | Services → AuditListener | Los servicios publican eventos sin conocer quién los escucha |
| **tokenVersion** | User → JWT → Filter | Invalidación inmediata de sesiones sin blacklist |
| **Refresh Token Rotation** | RefreshTokenService | Un token solo se usa una vez; reúso detecta robo |
| **JpaSpecificationExecutor** | RequestRepository | Predicados dinámicos que evitan errores con parámetros null |
| **Optimistic Locking** | Todas las entidades mutables | `@Version` detecta modificaciones concurrentes |

---

## 3. Estructura de carpetas

```
src/main/java/mx/juarezdeoriente/solicitudes/
│
├── modules/                         ← Módulos de negocio
│   ├── users/                       ← Autenticación, usuarios y tokens
│   │   ├── domain/
│   │   │   ├── User.java            @Entity — tabla users
│   │   │   ├── Role.java            enum ADMIN | CAPTURISTA | AUTORIZADOR | AUDITOR
│   │   │   └── RefreshToken.java    @Entity — tabla refresh_tokens
│   │   ├── application/
│   │   │   ├── UserService.java     lógica de negocio
│   │   │   ├── UserEvents.java      eventos: Created, Updated, Deactivated
│   │   │   └── dto/
│   │   │       └── UserDto.java     records de request y response
│   │   ├── infrastructure/
│   │   │   ├── UserRepository.java          extends JpaRepository
│   │   │   └── RefreshTokenRepository.java  extends JpaRepository
│   │   └── presentation/
│   │       ├── AuthController.java   /api/v1/auth — login, refresh, logout
│   │       └── UserController.java   /api/v1/users — CRUD de usuarios
│   │
│   ├── suppliers/                   ← Catálogo de proveedores
│   │   ├── domain/
│   │   │   └── Supplier.java
│   │   ├── application/
│   │   │   ├── SupplierService.java
│   │   │   ├── SupplierEvents.java
│   │   │   └── dto/SupplierDto.java
│   │   ├── infrastructure/
│   │   │   └── SupplierRepository.java
│   │   └── presentation/
│   │       └── SupplierController.java
│   │
│   ├── workers/                     ← Catálogo de trabajadores
│   │   ├── domain/
│   │   │   ├── Worker.java
│   │   │   └── WorkerType.java      enum SOCIO | EVENTUAL
│   │   ├── application/
│   │   │   ├── WorkerService.java
│   │   │   ├── WorkerEvents.java
│   │   │   └── dto/WorkerDto.java
│   │   ├── infrastructure/
│   │   │   └── WorkerRepository.java
│   │   └── presentation/
│   │       └── WorkerController.java
│   │
│   ├── requests/                    ← Solicitudes de compra (núcleo)
│   │   ├── domain/
│   │   │   ├── Request.java         @Entity — encabezado de la solicitud
│   │   │   ├── RequestItem.java     @Entity — renglones (máx. 8)
│   │   │   └── RequestStatus.java   enum BORRADOR | EMITIDA | CANCELADA
│   │   ├── application/
│   │   │   ├── RequestService.java
│   │   │   ├── RequestEvents.java
│   │   │   └── dto/RequestDto.java
│   │   ├── infrastructure/
│   │   │   ├── RequestRepository.java
│   │   │   └── RequestSpecification.java  búsquedas dinámicas con filtros
│   │   └── presentation/
│   │       └── RequestController.java
│   │
│   ├── documents/                   ← Generación de PDF
│   │   ├── domain/
│   │   │   ├── RequestDocument.java  @Entity — historial de PDFs generados
│   │   │   └── SolicitudPdfData.java record con los datos del PDF
│   │   ├── application/
│   │   │   └── PdfGeneratorService.java
│   │   ├── infrastructure/
│   │   │   └── RequestDocumentRepository.java
│   │   └── presentation/
│   │       └── DocumentController.java
│   │
│   └── audit/                       ← Bitácora inmutable
│       ├── domain/
│       │   └── AuditEvent.java       @Entity — solo INSERT, nunca UPDATE/DELETE
│       ├── application/
│       │   └── AuditListener.java    @EventListener — escucha todos los eventos
│       ├── infrastructure/
│       │   └── AuditRepository.java
│       └── presentation/
│           └── AuditController.java
│
├── security/                        ← Seguridad transversal (JWT, Spring Security)
│   ├── SecurityConfig.java
│   ├── JwtService.java
│   ├── JwtAuthenticationFilter.java
│   ├── AppUserDetails.java
│   ├── AppUserDetailsService.java
│   ├── RefreshTokenService.java
│   ├── AppRoles.java
│   └── SecurityHelper.java
│
├── exception/                       ← Excepciones de negocio
│   ├── DomainException.java         → HTTP 422
│   ├── NotFoundException.java       → HTTP 404
│   └── ConflictException.java       → HTTP 409
│
├── config/                          ← Configuración Spring
│   ├── AsyncConfig.java
│   ├── CacheConfig.java             Caffeine
│   ├── CorsConfig.java
│   ├── CompanyProperties.java
│   ├── CorrelationIdFilter.java     X-Correlation-ID en cada petición
│   ├── DataSeeder.java              usuarios iniciales (primer arranque)
│   ├── LegacyDataSeeder.java        importa datos del sistema VB6/Access
│   ├── RateLimitFilter.java
│   ├── RequestLoggingFilter.java
│   └── SettingsController.java
│
└── shared/                          ← Utilidades transversales
    ├── PageResult.java
    ├── web/
    │   ├── ApiResponse.java          { data } o { error, details }
    │   ├── GlobalExceptionHandler.java
    │   └── PageableDefaults.java
    └── idempotency/
        ├── IdempotencyFilter.java
        ├── IdempotencyKeyJpaEntity.java
        └── IdempotencyKeyJpaRepository.java
```

### Cómo leer cada módulo

Cada módulo sigue siempre el mismo orden de capas, de adentro hacia afuera:

```
domain/        ← La entidad y sus reglas. No depende de nada externo.
application/   ← El servicio que usa el dominio para hacer el trabajo.
infrastructure/← Cómo se guarda en base de datos (JPA).
presentation/  ← El controller que recibe la petición HTTP y devuelve JSON.
```

---

## 4. Módulos

| Módulo | Rutas HTTP | Responsabilidad |
|---|---|---|
| `users` | `/api/v1/auth/*`, `/api/v1/users` | Login, tokens JWT, CRUD de usuarios |
| `suppliers` | `/api/v1/suppliers` | Catálogo de proveedores |
| `workers` | `/api/v1/workers` | Catálogo de trabajadores |
| `requests` | `/api/v1/requests` | Solicitudes de compra — núcleo del sistema |
| `documents` | `/api/v1/requests/{id}/documents` | Generar y descargar PDFs |
| `audit` | `/api/v1/audit-events` | Bitácora de acciones (solo lectura) |

---

## 5. Modelo de datos

### Diagrama ER simplificado

```
users (1) ──────── (N) user_roles
  │
  ├── (1) ──────── (N) requests ──── (N) request_items ──── (1) workers
  │                       │
  │                       └── (N) ──── (1) suppliers
  │
  ├── (1) ──────── (N) refresh_tokens
  └── (1) ──────── (N) audit_events

suppliers (1) ──── (N) requests
workers   (1) ──── (N) request_items
```

### Tablas

| Tabla | Descripción | Campo clave |
|---|---|---|
| `users` | Usuarios del sistema | `token_version` para invalidar JWT |
| `user_roles` | Roles asignados | `ADMIN, CAPTURISTA, AUTORIZADOR, AUDITOR` |
| `refresh_tokens` | Tokens de refresco | `token_hash` SHA-256 (nunca el token en claro) |
| `suppliers` | Catálogo de proveedores | `code` UNIQUE, `version` para concurrencia |
| `workers` | Catálogo de trabajadores | `worker_type`: `SOCIO` o `EVENTUAL` |
| `requests` | Solicitudes de compra | `folio` atómico, `status` BORRADOR/EMITIDA/CANCELADA |
| `request_items` | Renglones de solicitud | máx. 8 por solicitud |
| `request_documents` | Historial de PDFs | checksum SHA-256, tamaño, versión |
| `audit_events` | Bitácora inmutable | Solo INSERT, nunca UPDATE ni DELETE |
| `idempotency_keys` | Claves de idempotencia | Evita duplicar POST si se repite la petición |

### Migraciones Flyway

| Versión | Descripción |
|---|---|
| V1 | Esquema inicial (tablas, índices, secuencia `folio_seq`) |
| V2 | Elimina admin placeholder del SQL (DataSeeder lo crea) |
| V3 | Agrega `version` (optimistic locking) y `updated_at` |
| V4 | Agrega `token_version` a `users` y tabla `refresh_tokens` |
| V5 | Tablas `idempotency_keys` y `request_documents` |
| V6 | Índices de búsqueda en `audit_events` |
| V7 | Columna `active` en `users` para desactivación lógica |
| V8 | Columna `notes` en `requests` |

> **Regla:** nunca modificar una migración ya aplicada. Crear siempre V_N+1.

### Estados de una solicitud

```
BORRADOR ──► EMITIDA ──► CANCELADA
   │
   └── folio = null     folio asignado por folio_seq (atómico)
```

---

## 6. Seguridad

### Flujo de autenticación

```
POST /auth/login
  → access_token (15 min) + refresh_token (7 días, HttpOnly cookie)

Cada petición:
  Bearer <access_token> en header Authorization
  → JwtAuthenticationFilter verifica firma + tokenVersion

POST /auth/refresh
  → Valida refresh token, lo revoca, emite nuevo par
  → Si el mismo token se usa dos veces: revoca TODAS las sesiones (detección de robo)

POST /auth/logout
  → Revoca todos los refresh tokens del usuario
```

### Roles y permisos

| Rol | Puede hacer |
|---|---|
| `ADMIN` | Todo: usuarios, catálogos, solicitudes, auditoría |
| `CAPTURISTA` | Crear/consultar/emitir/cancelar solicitudes; gestionar catálogos |
| `AUTORIZADOR` | Consultar solicitudes (segunda etapa — pendiente) |
| `AUDITOR` | Solo lectura: solicitudes y bitácora |

### Mecanismos implementados

- **Argon2id** para contraseñas
- **JWT HS256** con claims: `iss`, `aud`, `jti`, `tv` (tokenVersion), `type`
- **tokenVersion** en User — invalida todos los JWT anteriores al incrementar
- **Refresh token rotation** — cada token se usa una sola vez
- **CORS** por entorno (`CORS_ALLOWED_ORIGINS`)
- **Security headers**: CSP, X-Frame-Options, X-Content-Type-Options
- **Rate limiting**: 5 req/min en `/auth/login`, 10/min en `/auth/refresh`, 60/min en POST general
- **Idempotency-Key** en POST `/requests` — evita duplicados si se repite la petición
- **Optimistic locking** (`@Version`) — detecta modificaciones concurrentes

### Variables de entorno de seguridad

```bash
JWT_SECRET=<base64 de 256+ bits aleatorios>   # CAMBIAR EN PRODUCCIÓN
JWT_EXPIRATION_MS=900000                        # 15 min
JWT_REFRESH_EXPIRATION_MS=604800000             # 7 días
JWT_ISSUER=solicitudes-api
JWT_AUDIENCE=solicitudes-client
CORS_ALLOWED_ORIGINS=https://solicitudes.empresa.local
```

---

## 7. API Reference

Base URL: `http://localhost:8081/api/v1`

### Endpoints

| Método | Ruta | Descripción | Rol mínimo |
|---|---|---|---|
| POST | `/auth/login` | Login — retorna access + refresh token | Público |
| POST | `/auth/refresh` | Rota el refresh token | Público |
| POST | `/auth/logout` | Revoca todos los tokens | Autenticado |
| GET | `/auth/me` | Datos del usuario actual | Autenticado |
| POST | `/auth/change-password` | Cambia contraseña | Autenticado |
| GET | `/users` | Listar usuarios | ADMIN |
| POST | `/users` | Crear usuario | ADMIN |
| GET | `/users/{id}` | Ver usuario | ADMIN |
| PATCH | `/users/{id}` | Actualizar usuario | ADMIN |
| DELETE | `/users/{id}` | Desactivar usuario | ADMIN |
| GET | `/suppliers` | Listar proveedores | Autenticado |
| POST | `/suppliers` | Crear proveedor | CAPTURISTA+ |
| GET | `/suppliers/{id}` | Ver proveedor | Autenticado |
| PATCH | `/suppliers/{id}` | Actualizar proveedor | CAPTURISTA+ |
| DELETE | `/suppliers/{id}` | Eliminar proveedor | ADMIN |
| GET | `/workers` | Listar trabajadores | Autenticado |
| POST | `/workers` | Crear trabajador | CAPTURISTA+ |
| GET | `/workers/{id}` | Ver trabajador | Autenticado |
| PATCH | `/workers/{id}` | Actualizar trabajador | CAPTURISTA+ |
| DELETE | `/workers/{id}` | Eliminar trabajador | ADMIN |
| GET | `/requests` | Listar solicitudes (paginado + filtros) | Autenticado |
| POST | `/requests` | Crear borrador | CAPTURISTA+ |
| GET | `/requests/{id}` | Ver solicitud | Autenticado |
| PATCH | `/requests/{id}` | Actualizar borrador | CAPTURISTA+ |
| POST | `/requests/{id}/items` | Agregar renglón | CAPTURISTA+ |
| PATCH | `/requests/{id}/items/{itemId}` | Actualizar renglón | CAPTURISTA+ |
| DELETE | `/requests/{id}/items/{itemId}` | Eliminar renglón | CAPTURISTA+ |
| POST | `/requests/{id}/issue` | Emitir (asigna folio) | CAPTURISTA+ |
| POST | `/requests/{id}/cancel` | Cancelar | CAPTURISTA+ |
| GET | `/requests/{id}/documents` | Historial de PDFs | Autenticado |
| GET | `/requests/{id}/documents/latest` | Descargar PDF más reciente | Autenticado |
| GET | `/audit-events` | Buscar en bitácora (paginado + filtros) | AUDITOR+ |

### Formato de respuesta

```json
// Éxito con datos
{ "data": { "id": "...", "name": "..." } }

// Éxito paginado
{
  "data": [...],
  "meta": {
    "page": 1, "size": 20,
    "totalElements": 100, "totalPages": 5
  }
}

// Error de validación
{ "error": "Campos requeridos faltantes", "details": ["name es obligatorio"] }

// Error de negocio
{ "error": "La solicitud debe tener al menos un renglón antes de emitirse" }
```

### Códigos HTTP

| Código | Cuándo |
|---|---|
| 200 | OK |
| 201 | Recurso creado |
| 204 | Eliminado sin contenido |
| 400 | Validación fallida o parámetro inválido |
| 401 | Sin token / token expirado / credenciales incorrectas |
| 403 | Autenticado pero sin el rol necesario |
| 404 | Recurso no encontrado |
| 409 | Conflicto — clave duplicada o modificación concurrente |
| 422 | Regla de negocio violada |
| 429 | Rate limit excedido |
| 500 | Error interno — ver logs con `X-Correlation-ID` |

---

## 8. Cómo desarrollar

### Requisitos

- Java 21 (`JAVA_HOME` apuntando a `jdk-21`)
- Maven 3.9+
- PostgreSQL 16+ corriendo localmente

### Arranque

```bash
# 1. Crear credenciales locales (una sola vez)
# src/main/resources/application-local.yml
spring:
  datasource:
    password: tu_password_local
app:
  jwt:
    expiration-ms: 3600000   # 1h en local

# 2. Ejecutar
mvn spring-boot:run
```

### Añadir un nuevo módulo

Ejemplo: módulo `approvals`

```
modules/approvals/
├── domain/
│   ├── Approval.java          @Entity
│   └── ApprovalStatus.java    enum
├── application/
│   ├── ApprovalService.java
│   ├── ApprovalEvents.java
│   └── dto/
│       └── ApprovalDto.java
├── infrastructure/
│   └── ApprovalRepository.java  extends JpaRepository
└── presentation/
    └── ApprovalController.java  @RestController
```

**Checklist al crear un módulo:**

- [ ] `@Entity` en `domain/` con `@Version` para optimistic locking
- [ ] `Repository` en `infrastructure/` extiende `JpaRepository`
- [ ] `Service` en `application/` con `@Service`
- [ ] `Controller` en `presentation/` con `@RestController` y `@PreAuthorize`
- [ ] Events en `application/` publicados con `applicationEventPublisher.publishEvent(...)`
- [ ] `AuditListener` actualizado para escuchar los nuevos eventos
- [ ] Migración Flyway `V_N__descripcion.sql` para las nuevas tablas

### Añadir un campo a una entidad existente

1. Crear nueva migración: `src/main/resources/db/migration/V_N__add_campo.sql`
2. Agregar el campo en la entidad (`domain/`)
3. Agregar el campo en el DTO si debe exponerse (`application/dto/`)
4. Actualizar el service si hay lógica nueva

---

## 9. Cómo debuggear

### Rastrear una petición por los logs

Cada petición tiene un `X-Correlation-ID` único:

```
2026-07-16 10:30:11 [A3F9B2C1] INFO  RequestLoggingFilter - POST /api/v1/requests -> 422 (45ms)
2026-07-16 10:30:11 [A3F9B2C1] ERROR GlobalExceptionHandler - La solicitud debe tener al menos un renglón
```

Buscar ese ID en los logs para ver toda la traza de la petición.

### Errores comunes

| Síntoma | Causa probable | Solución |
|---|---|---|
| `401` en todos los endpoints | Token expirado (15 min en prod) | Usar `/auth/refresh` o volver a hacer login |
| `401` después de cambiar contraseña | `tokenVersion` incrementó | Volver a hacer login — comportamiento correcto |
| `409` en update | Otro usuario modificó el registro | Leer de nuevo y reintentar |
| `422` al emitir | Sin renglones o estado incorrecto | Agregar items antes de emitir |
| `Flyway checksum mismatch` | Se modificó una migración aplicada | Crear V_N+1, NUNCA editar migraciones existentes |

### Activar más logs en local

```yaml
# application-local.yml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.springframework.security: DEBUG
```

### Consultar auditoría directamente en BD

```sql
-- Últimas 20 acciones
SELECT actor_id, action, entity_type, entity_id, occurred_at
FROM audit_events
ORDER BY occurred_at DESC
LIMIT 20;

-- Historial de una solicitud específica
SELECT * FROM audit_events
WHERE entity_type = 'Request' AND entity_id = 'uuid-aqui';
```

---

## 10. Deployment

### Variables de entorno

| Variable | Desarrollo | Producción |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/solicitudes` | URL del servidor |
| `DB_USER` | `postgres` | Usuario dedicado (sin superusuario) |
| `DB_PASSWORD` | en `application-local.yml` | Variable de entorno del sistema |
| `JWT_SECRET` | valor por defecto | 256+ bits aleatorios — **cambiar** |
| `JWT_EXPIRATION_MS` | `3600000` (1h) | `900000` (15 min) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | `https://solicitudes.empresa.local` |
| `SESSION_SECURE` | `false` | `true` |

### Producción en Windows (sin Docker)

```cmd
REM Compilar
mvn clean package -DskipTests

REM Ejecutar
java -XX:MaxRAMPercentage=75 -jar target/solicitudes-backend-0.0.1-SNAPSHOT.jar
```

Las variables de entorno se configuran en el sistema operativo Windows, no en el `.jar`.

### Health check

```
GET /actuator/health  →  { "status": "UP" }
```

### Checklist antes de producción

- [ ] `JWT_SECRET` generado aleatoriamente (no el valor por defecto)
- [ ] `SESSION_SECURE=true`
- [ ] PostgreSQL con usuario dedicado sin superusuario
- [ ] Reverse proxy con HTTPS (Nginx, Caddy o IIS)
- [ ] Backup automático de PostgreSQL configurado
- [ ] `CORS_ALLOWED_ORIGINS` apunta solo al dominio real
- [ ] Logs rotados
- [ ] Firewall: solo exponer puerto 443

---

## 11. Decisiones de diseño

### ¿Por qué arquitectura modular (modules/)?

Cada módulo tiene todo lo suyo junto: dominio, servicio, repositorio y controller. Un desarrollador que abre `modules/requests/` encuentra todo lo relacionado con solicitudes sin necesitar buscar en múltiples carpetas. Es la misma idea que los módulos de NestJS o los paquetes de Go.

### ¿Por qué domain / application / infrastructure / presentation?

Cada carpeta tiene una responsabilidad clara:
- `domain/` — la entidad y sus datos. No depende de nada.
- `application/` — la lógica: qué hacer cuando llega una petición.
- `infrastructure/` — cómo hablar con la base de datos.
- `presentation/` — cómo recibir la petición HTTP y devolver JSON.

### ¿Por qué Domain Events en lugar de llamadas directas?

El módulo `requests` no necesita saber que existe una bitácora. Cuando se emite una solicitud, el servicio publica `RequestEvents.Issued`. El `AuditListener` lo escucha de forma independiente. Esto hace que agregar nuevos comportamientos (email, estadísticas) no requiera modificar el servicio de solicitudes.

### ¿Por qué tokenVersion en lugar de blacklist?

Una blacklist requiere Redis o una tabla consultada en cada petición. `tokenVersion` es un campo en `users` que ya se lee al validar el JWT. Sin costo adicional de infraestructura.

### ¿Por qué Specifications y no `@Query` para búsquedas?

`@Query` con parámetros opcionales null en PostgreSQL + Hibernate 6 genera errores de tipo (`bytea`). `Specification` construye el predicado en Java y solo agrega las condiciones de los parámetros que llegaron.

### ¿Por qué Argon2id y no BCrypt?

Argon2id es resistente a ataques de GPU y ASIC. BCrypt es seguro pero sin resistencia de memoria. Spring Security 6 incluye `Argon2PasswordEncoder` sin dependencias extra.

---

## 12. Roadmap

### Segunda etapa

| Feature | Descripción | Complejidad |
|---|---|---|
| **Flujo de aprobación** | Módulo `approvals`: PENDIENTE → APROBADA / RECHAZADA | Media |
| **Adjuntos** | Subir cotizaciones o facturas a la solicitud | Media |
| **Notificaciones por correo** | Al emitir, aprobar o cancelar | Baja |
| **Dashboard** | Solicitudes por período, proveedor y estado | Baja |
| **Exportación Excel** | Apache POI | Baja |
| **Login corporativo** | Microsoft Entra ID (Azure AD) con OAuth2/OIDC | Alta |
| **Frontend React** | React 18 + Vite + Material UI | Alta |

### Mejoras técnicas

| Mejora | Estado |
|---|---|
| Rate limiting | ✅ Implementado |
| Idempotency keys | ✅ Implementado |
| PDF versionado | ✅ Implementado |
| RS256 (JWT con RSA) | Pendiente — útil si se agregan múltiples servicios |
| Redis | Pendiente — solo necesario con múltiples instancias |
| Tests de integración | Pendiente — Testcontainers + PostgreSQL real |

---

## Glosario

| Término | Definición |
|---|---|
| **Folio** | Número consecutivo único asignado al emitir una solicitud. Nunca se reutiliza. |
| **Borrador** | Estado inicial de una solicitud. Se puede modificar libremente. |
| **Emitida** | Solicitud con folio asignado. Solo se puede cancelar. |
| **tokenVersion** | Contador en el usuario que invalida todos los JWT anteriores al incrementar. |
| **Refresh Token Rotation** | Cada refresh token se usa una sola vez; al usarlo se emite uno nuevo. |
| **Domain Event** | Notificación que el servicio publica cuando algo importante ocurrió. |
| **Correlation ID** | ID único por petición HTTP para rastrear todos los logs de esa operación. |
| **Optimistic Locking** | Mecanismo que detecta si alguien más modificó un registro antes de guardar. |
