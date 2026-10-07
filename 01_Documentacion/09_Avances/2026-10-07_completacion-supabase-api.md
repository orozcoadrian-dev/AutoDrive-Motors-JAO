# Avance de implementación: Supabase y módulos faltantes

**Fecha:** 2026-10-07  
**Responsabilidad abordada:** persistencia, DAO, API REST, MVC, validación y pruebas manuales.

## Decisión de persistencia

El docente autorizó usar Supabase. Se adoptó PostgreSQL mediante JDBC y Spring Data JPA desde el backend; no se usa la Data API de Supabase desde el navegador. Así, las vistas Thymeleaf siguen consumiendo solamente `/api/*` y las reglas de negocio continúan en la capa de servicios.

No se guardaron credenciales en el repositorio. El perfil `supabase` exige estas variables de entorno:

```text
SUPABASE_DB_URL
SUPABASE_DB_USER
SUPABASE_DB_PASSWORD
```

La URL, usuario y contraseña reales se configuran solamente en la terminal de cada integrante. Como la contraseña no fue entregada en este avance, la conexión remota queda pendiente de validación por el equipo. Esta decisión sustituye el requisito original de MySQL por PostgreSQL/Supabase con autorización del docente.

## Implementado

1. Dependencias de PostgreSQL y Flyway, perfil `application-supabase.yml` y migración `V1__create_autodrive_schema.sql`.
2. La entidad y el contrato de cliente ahora incluyen documento único, además del correo único.
3. Mantenimiento completo: entidad, DAO, DTOs, servicio transaccional, controlador REST, historial y vista `/mantenimientos`.
4. Al registrar mantenimiento, el vehículo pasa a `EN_MANTENIMIENTO`; un vehículo vendido no admite mantenimiento y un vehículo con historial no se elimina.
5. Reporte `GET /api/reportes/resumen` con ventas, monto total y conteos por estado, mostrado también en la vista de ventas.
6. Cliente HTTP de tasa USD→COP con timeout, manejo de indisponibilidad como HTTP 503 y conversión `GET /api/vehiculos/{id}/conversion-usd`.
7. Respuestas HTTP de validación mejoradas: JSON o parámetros inválidos devuelven 400 y errores de integridad concurrentes devuelven 409.
8. Colección Postman importable en `postman/AutoDrive-Motors.postman_collection.json`.
9. Documentación actualizada en `README.md` y datos SQL opcionales compatibles con PostgreSQL.

## Verificaciones realizadas

- `node --check src/main/resources/static/js/app.js` y `animations.js`: correctos.
- La colección Postman se analizó como JSON válido.
- `mvn clean test` con Java 21: correcto. Pasan las pruebas de venta, mantenimiento y conversión con cliente externo simulado.
- Arranque local con el perfil `dev`: Flyway aplicó la migración V1 sobre H2 y la aplicación inició correctamente.
- Prueba HTTP local: inicio 200; documento del cliente presente; mantenimiento creado; vehículo con estado `EN_MANTENIMIENTO`; reporte actualizado; id `0` rechazado con 400.

## Revisión de seguridad focalizada

| Estado | Evidencia | Tratamiento |
|---|---|---|
| Listo | No hay contraseña, URL con secreto ni clave de proveedor escrita en código, perfiles, colección Postman o documentación. | Configuración mediante variables de entorno. |
| Listo | Las vistas no acceden a Supabase; solo llaman a la API del mismo origen. | Se conserva la frontera backend–base de datos. |
| Listo | DTOs, validación, normalización y respuestas genéricas evitan exponer entidades, SQL y trazas. | Implementado. |
| Listo | La tasa externa tiene timeout, contrato aislado e informa 503 al fallar. | Implementado; la tasa no se persiste. |
| Por validar | Conexión real, SSL/red y migración en el proyecto Supabase. | Ejecutar el perfil `supabase` con la contraseña privada y revisar `flyway_schema_history`. |
| Por validar | Autenticación y autorización si la aplicación deja localhost. | El taller no define usuarios/roles; no desplegar públicamente sin acordarlos. |

## Pendiente de equipo

- Configurar la contraseña privada de Supabase en cada entorno y ejecutar el arranque con perfil `supabase`.
- Importar la colección Postman y conservar evidencia de respuestas 201, 400, 404, 409 y 503.
- Definir el proceso para devolver un vehículo desde mantenimiento a `DISPONIBLE` si el docente lo exige; el taller entregado únicamente exige registrar y consultar mantenimientos con cambio a estado en mantenimiento.

## Control de acceso de administrador

**Fecha de incorporación:** 2026-10-07  
**Alcance:** autenticación web, autorización de API y protección CSRF.

- Se añadió Spring Security con una única cuenta `ADMIN` configurada por variables de entorno; no existe una contraseña productiva dentro del repositorio. El perfil `dev` conserva una cuenta ficticia solo para pruebas locales.
- Todas las vistas de operación y las rutas `/api/**` requieren sesión de administrador. Las vistas sin sesión redirigen al login; la API responde JSON 401 o 403 para que el cliente no interprete un HTML de redirección como datos.
- Las credenciales se procesan con BCrypt. La sesión vence tras 30 minutos, se rota al iniciar sesión y se invalida al salir.
- Los formularios y `fetch` conservan CSRF activo. Las páginas Thymeleaf entregan el token en metadatos y `app.js` lo añade a solicitudes que cambian estado. Postman obtiene y renueva su token en la carpeta `Autenticación`.
- La migración exclusiva de PostgreSQL activa RLS y revoca privilegios de `anon` y `authenticated` sobre las tablas. La Data API de Supabase no es una alternativa para administrar estos datos.

### Revisión focalizada de seguridad

| Estado | Hallazgo / evidencia | Tratamiento |
|---|---|---|
| Confirmado por fuente | El formulario de Spring Security exige token CSRF y las solicitudes JSON deben incluirlo fuera de una cookie automática. | Implementado con token renderizado por Thymeleaf y encabezado en `app.js`. |
| Confirmado por fuente | El frontend no debe contener la contraseña JDBC ni claves privilegiadas de Supabase. | Variables de entorno; `.gitignore` incluye archivos `.env`. |
| Confirmado por revisión estática | API y vistas están restringidas al rol `ADMIN`; API sin sesión usa 401 JSON. | `SecurityConfig` y prueba `SeguridadIntegracionTest`. |
| Rechazado | Desactivar CSRF para que los POST de la interfaz funcionen. | No aplicado: se conserva CSRF y se adapta el cliente. |
| Por validar | Compilación, pruebas `MockMvc`, login/logout, expiración y 401/403 con el código de seguridad nuevo. | Ejecutar en un entorno aislado `mvn clean test` y hacer la prueba manual local. |
| Por validar | Conexión Supabase, SSL/red, Flyway V1/V2 y RLS efectivo en el proyecto remoto. | Ejecutar con contraseña privada; consultar `flyway_schema_history` y comprobar que la Data API no liste tablas sin sesión. |

Greptile no ejecutado.

## Corrección de migración remota: campo `documento`

**Fecha:** 2026-10-07  
**Alcance:** corrección incremental de PostgreSQL/Supabase, sin modificar la migración histórica V1.

El arranque real con el perfil `supabase` confirmó que la conexión JDBC con SSL, el pool de sesión y Flyway funcionan. La aplicación se detuvo posteriormente durante `ddl-auto: validate`: la tabla remota `clientes` no tenía la columna obligatoria `documento` que exige la entidad `Cliente`.

La causa compatible con el registro es que una base ya existente fue marcada por Flyway como baseline en V1; por ello, la creación histórica no debe editarse ni volver a aplicarse. Se añadió `db/migration-postgresql/V3__add_documento_to_clientes.sql`, que:

1. crea la columna `documento`;
2. conserva los clientes existentes asignando `LEGACY-<id>` solo donde el valor es nulo o vacío;
3. hace el campo obligatorio y único, de acuerdo con JPA y el contrato REST.

La nueva migración no modifica ventas, vehículos ni mantenimientos. Los registros con identificador `LEGACY-<id>` deberán actualizarse a su documento real desde el módulo de clientes antes de una entrega con datos reales.

### Verificación y pendiente

- Evidencia real: el registro de terminal mostró conexión PostgreSQL, ejecución de Flyway hasta versión 2 y el error de validación exacto de `clientes.documento`.
- Revisión estática: la migración es PostgreSQL, conserva datos existentes y coincide con `Cliente.documento` (`VARCHAR(30)`, no nulo y único).
- Pendiente de validación: reiniciar la aplicación con el mismo perfil `supabase` y confirmar que Flyway aplica V3 y que aparece `Started AutoDriveMotorsApplication`. Esta ejecución debe realizarla el integrante que mantiene las credenciales locales; no se registran credenciales en este documento.

### Compatibilidad de perfiles

El primer intento posterior ejecutó el perfil `dev` porque la nueva consola no heredó las variables de la anterior. H2 ya crea `documento` en V1 y no debe aplicar V3. Por ello V3 se movió a `db/migration-postgresql/`; el perfil `supabase` sí carga ambas ubicaciones y el perfil `dev` conserva únicamente la migración base compatible con H2.

Para que el login pueda probarse desde `http://127.0.0.1`, el perfil Supabase queda ligado a localhost por defecto y la cookie `Secure` se controla con `AUTODRIVE_SESSION_COOKIE_SECURE` (por defecto `false`). Si el equipo despliega bajo HTTPS, debe definir `AUTODRIVE_SERVER_ADDRESS` para su host y `AUTODRIVE_SESSION_COOKIE_SECURE=true`; las cookies siguen siendo `HttpOnly` y `SameSite=Strict`.

Si una migración ya eliminada sigue apareciendo al ejecutar solo `mvn spring-boot:run`, se debe usar `mvn clean spring-boot:run`. Maven conserva recursos eliminados en `target/classes` hasta limpiar esa salida generada; `clean` no modifica el código fuente ni la base de datos.

## Corrección de migración remota: fecha de mantenimiento

**Fecha:** 2026-10-07  
**Alcance:** alineación de esquema PostgreSQL heredado con la entidad JPA.

El siguiente arranque real con perfil `supabase` confirmó la conexión, validó cuatro migraciones y llegó al esquema público versión 3. Hibernate detectó que `mantenimientos.fecha_mantenimiento` era `timestamptz` en la base heredada, mientras la entidad `Mantenimiento` usa `LocalDate` y el contrato de V1 exige `DATE`.

Se añadió `db/migration-postgresql/V4__align_mantenimiento_date_type.sql`. Convierte la columna a `DATE` utilizando UTC para conservar el día calendario de registros existentes. La migración se aplica solamente a PostgreSQL/Supabase, no a H2.

**Pendiente de validación:** ejecutar de nuevo `mvn clean spring-boot:run` con `SPRING_PROFILES_ACTIVE=supabase`; se espera que Flyway aplique V4 y que Hibernate continúe con la siguiente validación del esquema. No se almacenaron credenciales en este documento.

## Corrección de migración remota: control de concurrencia de vehículos

**Fecha:** 2026-10-07  
**Alcance:** completar una columna requerida por JPA en la tabla heredada `vehiculos`.

La ejecución posterior aplicó V4 correctamente y después Hibernate informó que faltaba `vehiculos.version`. La entidad `Vehiculo` usa `@Version` para impedir que dos ventas simultáneas confirmen el mismo vehículo; V1 define esa columna, pero la tabla preexistente de Supabase no la tenía.

Se añadió `db/migration-postgresql/V5__add_vehiculo_optimistic_lock.sql`, que agrega `version BIGINT NOT NULL DEFAULT 0`. El valor inicial mantiene vehículos existentes en una versión válida y activa el control optimista para operaciones posteriores. La migración es exclusiva de PostgreSQL/Supabase.

**Pendiente de validación:** reiniciar con `mvn clean spring-boot:run` en la misma consola Supabase y confirmar aplicación de V5 seguida de `Started AutoDriveMotorsApplication`.
