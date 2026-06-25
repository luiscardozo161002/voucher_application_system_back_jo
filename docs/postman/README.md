# Colección Postman

## Importar

1. Abre Postman
2. **File → Import**
3. Selecciona `Sistema_Solicitudes.postman_collection.json`
4. Verifica que la variable `baseUrl = http://localhost:8081` en la pestaña **Variables** de la colección

## Flujo recomendado

```
Auth / Login
    ↓
Proveedores / Buscar proveedores   ← guarda {{supplierId}} automáticamente
    ↓
Trabajadores / Buscar trabajadores ← guarda {{workerId}} automáticamente
    ↓
Solicitudes / Crear borrador       ← guarda {{requestId}}
    ↓
Solicitudes / Agregar renglón
    ↓
Solicitudes / Emitir solicitud     ← asigna folio consecutivo
    ↓
Solicitudes / Descargar PDF
```

## Idempotency Key

Para evitar doble emisión de folio, agrega el header en Postman:

```
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
```

Una segunda llamada con la misma clave devuelve la respuesta original sin re-procesar.

## Usuarios de prueba

| Username | Password | Rol |
|---|---|---|
| admin | Admin123! | ADMIN |
| capturista | Capturista1! | CAPTURISTA |
