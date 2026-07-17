# Sistema de Solicitudes de Vales (Backend)

API REST para la gestión de solicitudes de compra (pedidos a proveedores).  
Migración del sistema legado VB6/Access — **Juarez de Oriente S.A. de C.V.**

---

## Tecnología

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.3 |
| Base de datos | PostgreSQL 16+ |
| Migraciones | Flyway |
| Build | Maven 3.9+ |
| Seguridad | JWT (HS256) + Argon2id |
| Cache | Caffeine (in-memory) |
| PDF | Flying Saucer + Thymeleaf |

---

## Arquitectura y patrones

Arquitectura **Modular** — cada módulo de negocio tiene sus propias capas internas:

```
modules/{modulo}/
├── domain/          ← La entidad (@Entity) y sus datos
├── application/     ← El servicio con la lógica de negocio + DTOs
├── infrastructure/  ← El repositorio JPA (acceso a base de datos)
└── presentation/    ← El controller REST (recibe y responde HTTP)
```

### Módulos

| Módulo | Rutas | Responsabilidad |
|---|---|---|
| `users` | `/api/v1/auth`, `/api/v1/users` | Login JWT, usuarios, refresh tokens |
| `suppliers` | `/api/v1/suppliers` | Catálogo de proveedores |
| `workers` | `/api/v1/workers` | Catálogo de trabajadores |
| `requests` | `/api/v1/requests` | Solicitudes de compra — núcleo |
| `documents` | `/api/v1/requests/{id}/documents` | Generación y descarga de PDFs |
| `audit` | `/api/v1/audit-events` | Bitácora inmutable de acciones |

---

## Requisitos previos

- Java 21 (`JAVA_HOME` configurado)
- Maven 3.9+
- PostgreSQL 16+ corriendo localmente

---

## Ejecución

### 1. Configurar credenciales locales

Crear `src/main/resources/application-local.yml`:

```yaml
spring:
  datasource:
    password: TU_PASSWORD_POSTGRES

app:
  jwt:
    expiration-ms: 3600000  # 1 hora en local (15 min en produccion)
```

### 2. Crear la base de datos

```sql
-- En psql o pgAdmin
CREATE DATABASE solicitudes;
```

### 3. Ejecutar

```bash
mvn spring-boot:run
```

Flyway aplica las migraciones automáticamente al arrancar.  
Los seeds de desarrollo (2 usuarios, 3 proveedores, 3 trabajadores) se crean en el primer arranque.

### 4. Verificar

```
http://localhost:8081/actuator/health   → { "status": "UP" }
http://localhost:8081/swagger-ui.html   → Documentación interactiva
```

---

## Variables de entorno

Todas las variables tienen valores por defecto para desarrollo.  
Copiar `.env.example` a `.env` y ajustar para producción.

### Base de datos

| Variable | Defecto | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/solicitudes` | URL de conexión |
| `DB_USER` | `postgres` | Usuario de BD |
| `DB_PASSWORD` | `postgres` | Contraseña de BD |
| `PORT` | `8081` | Puerto del servidor |

### JWT y seguridad

| Variable | Defecto | Descripción |
|---|---|---|
| `JWT_SECRET` | *(valor de desarrollo)* | **Cambiar en producción** — base64 de 256+ bits |
| `JWT_EXPIRATION_MS` | `900000` | Duración del access token (15 min) |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` | Duración del refresh token (7 días) |
| `JWT_ISSUER` | `solicitudes-api` | Claim `iss` del JWT |
| `JWT_AUDIENCE` | `solicitudes-client` | Claim `aud` del JWT |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,...` | Orígenes permitidos |

### Datos institucionales (PDF del pedido)

| Variable | Defecto | Descripción |
|---|---|---|
| `COMPANY_NAME` | `JUAREZ DE ORIENTE S.A. DE C.V.` | Razón social |
| `COMPANY_ADDRESS` | `AV. TULANCINGO NO. 103...` | Dirección |
| `COMPANY_POSTAL_CODE` | `42842` | C.P. |
| `COMPANY_RFC` | `JOR070518U13` | RFC |
| `COMPANY_PHONE` | `(01-773) 78-5-04-97` | Teléfono |
| `COMPANY_EXTENSION` | `125 - 126` | Extensión |
| `COMPANY_DOCUMENT_CODE` | `CJ-FA-44 REV. 0` | Código del formato |

### Seeds de desarrollo

| Variable | Defecto | Descripción |
|---|---|---|
| `SEED_ADMIN_PASSWORD` | `Admin123!` | Contraseña del usuario admin inicial |
| `SEED_CAPTURISTA_PASSWORD` | `Capturista1!` | Contraseña del usuario capturista inicial |
| `SEED_DEMO_DATA` | `true` | `false` = solo usuarios, proveedores y trabajadores (sin solicitudes demo). `true` = todo incluyendo solicitudes de ejemplo. Usar `false` en producción para que los folios inicien en `0000001`. |

---

## Usuarios iniciales (seeds de desarrollo)

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `Admin123!` | ADMIN — acceso total |
| `capturista` | `Capturista1!` | CAPTURISTA — crear y consultar solicitudes |

---

## Migración de datos del sistema legado

Los datos históricos de `Solicitudes.mdb` se cargan **automáticamente** en el primer arranque.  
No se requiere ninguna acción manual.

```
[Arranque 1]  DataSeeder crea: 2 usuarios + 3 proveedores + 3 trabajadores
              Si SEED_DEMO_DATA=true: también crea solicitudes de ejemplo

[Arranque 1]  LegacyDataSeeder detecta 0 proveedores → importa desde SQL:
              154 proveedores + 164 trabajadores
              Si SEED_DEMO_DATA=true: también importa ~700 solicitudes históricas

[Arranque 2+] LegacyDataSeeder detecta >10 proveedores → omite silenciosamente
```

**Producción:** usar `SEED_DEMO_DATA=false` para que los folios inicien en `0000001`.  
**Local/desarrollo:** usar `SEED_DEMO_DATA=true` para tener datos de prueba completos.

Los scripts SQL de origen están en `src/main/resources/db/legacy/` y en `dummy/` como referencia.  
Ver `dummy/README.md` para más detalles.

---

## Pruebas

```bash
# Pruebas unitarias + controller tests
mvn test

# Pruebas de integración (requiere Docker)
mvn test -Dintegration.tests=true
```

---

## API

Base URL: `http://localhost:8081/api/v1`

| Módulo | Ruta |
|---|---|
| Autenticación | `/api/v1/auth/*` |
| Usuarios | `/api/v1/users` |
| Proveedores | `/api/v1/suppliers` |
| Trabajadores | `/api/v1/workers` |
| Solicitudes | `/api/v1/requests` |
| Auditoría | `/api/v1/audit-events` |

La colección Postman con todos los endpoints documentados está en `docs/postman/`.

---

## Estructura del proyecto

```
solicitudes-backend/
├── src/
│   ├── main/java/.../solicitudes/
│   │   ├── modules/
│   │   │   ├── users/         ← Autenticación y usuarios
│   │   │   │   ├── domain/
│   │   │   │   ├── application/
│   │   │   │   ├── infrastructure/
│   │   │   │   └── presentation/
│   │   │   ├── suppliers/     ← Catálogo de proveedores
│   │   │   ├── workers/       ← Catálogo de trabajadores
│   │   │   ├── requests/      ← Solicitudes (núcleo)
│   │   │   ├── documents/     ← Generación de PDF
│   │   │   └── audit/         ← Bitácora
│   │   ├── security/          ← JWT y Spring Security
│   │   ├── exception/         ← Excepciones de negocio
│   │   ├── config/            ← Configuraciones transversales
│   │   └── shared/            ← Utilidades compartidas
│   └── main/resources/
│       ├── db/migration/      ← Migraciones Flyway (V1–V8)
│       └── templates/pdf/     ← Plantilla HTML del pedido
├── docs/postman/              ← Colección Postman
├── dummy/                     ← Scripts SQL del sistema legado
├── docker/                    ← docker-compose y Dockerfile
├── ARCHITECTURE.md            ← Documentación técnica detallada
├── .env.example               ← Plantilla de variables de entorno
└── pom.xml
```

---

## Documentación adicional

Ver [`ARCHITECTURE.md`](ARCHITECTURE.md) para:
- Diagrama de componentes y modelo ER
- Modelo de seguridad JWT detallado
- Guía para agregar nuevos módulos
- Checklist de producción
- Roadmap y decisiones de diseño
