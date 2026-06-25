# Datos del sistema legado

Esta carpeta es de **referencia y documentación**.

Los scripts SQL que aquí se describen están copiados en  
`src/main/resources/db/legacy/` y **se ejecutan automáticamente** al iniciar  
la aplicación por primera vez mediante `LegacyDataSeeder`.

---

## Carga automática al arrancar

El componente `LegacyDataSeeder` (`config/LegacyDataSeeder.java`) detecta si los  
datos del legado ya fueron importados contando los proveedores:

- **Si hay ≤ 10 proveedores** → importa los tres archivos en orden.
- **Si hay > 10 proveedores** → asume que ya están cargados y omite silenciosamente.

```
mvn spring-boot:run
```

```
[Order 1] DataSeeder        → crea usuarios, 3 proveedores y 3 trabajadores de prueba
[Order 2] LegacyDataSeeder  → importa 154 proveedores, 164 trabajadores, 700 solicitudes
```

---

## Scripts SQL (referencia)

Los archivos originales generados desde `Solicitudes.mdb` están en:

```
src/main/resources/db/legacy/
├── 01_proveedores.sql   ← 154 proveedores (Doctor → suppliers)
├── 02_trabajadores.sql  ← 164 trabajadores (Trabajador → workers)
└── 03_solicitudes.sql   ← ~700 solicitudes (Movimiento → requests)
```

Fuente original: `SOLICITUDES/Solicitudes.mdb`  
Extraída con: PowerShell + Microsoft.ACE.OLEDB.12.0  
Fecha: 2026-06-24
