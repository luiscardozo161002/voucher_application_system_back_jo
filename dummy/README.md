# Datos de prueba y migración del legado

Esta carpeta contiene los scripts SQL extraídos del sistema original (VB6/Access) y datos de ejemplo para desarrollo.

---

## Carpeta `seeds/`

Scripts de migración del sistema legacy. Se aplican **una sola vez** sobre una BD limpia con las migraciones Flyway ya aplicadas.

| Archivo | Descripción | Registros |
|---|---|---|
| `01_proveedores_legado.sql` | Catálogo de proveedores (tabla `Doctor` del Access) | 154 |
| `02_trabajadores_legado.sql` | Catálogo de trabajadores (tabla `Trabajador`) | 164 |
| `03_solicitudes_legado.sql` | Solicitudes históricas (tabla `Movimiento`) | 700 |

### Cómo aplicar

```sql
-- 1. Conectarse a la base de datos
-- En pgAdmin: abrir Query Tool sobre la BD "solicitudes"
-- O por consola:
-- psql -U postgres -d solicitudes

-- 2. Aplicar en orden
\i dummy/seeds/01_proveedores_legado.sql
\i dummy/seeds/02_trabajadores_legado.sql
\i dummy/seeds/03_solicitudes_legado.sql
```

> **Importante:** Los renglones de detalle (tabla `Detalle` del Access) no se migran en esta versión porque las columnas heredadas (`Diagnostico`, `LAB`, `RX`) corresponden a otro dominio y requieren validación con el negocio antes de mapearlas a `request_items`.

---

## Notas de la migración

- Los folios históricos se conservan intactos (`NroDocumento` → `folio`).
- La secuencia `folio_seq` se avanza automáticamente al final de `03_solicitudes_legado.sql` para no colisionar con el último folio migrado.
- Los proveedores se identifican por `IdDoctor` (convertido a `code` en mayúsculas).
- Las solicitudes sin proveedor válido se asignan al primer proveedor disponible.
- Todas las solicitudes migradas quedan en estado `EMITIDA` con la fecha original de captura.
- El campo `created_by` se asigna al usuario `admin` del sistema nuevo.

---

## Fuente

Base de datos original: `SOLICITUDES/Solicitudes.mdb`
Extraída con: PowerShell + Microsoft.ACE.OLEDB.12.0
Fecha de extracción: 2026-06-24
