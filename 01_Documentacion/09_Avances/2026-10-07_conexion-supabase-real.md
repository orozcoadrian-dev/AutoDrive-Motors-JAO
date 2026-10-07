# Conexión real con Supabase: resultado verificado

**Fecha:** 2026-10-07
**Estado:** conexión establecida y flujo completo verificado contra el proyecto Supabase del equipo.
**Reemplaza a:** `2026-10-07_conexion-supabase-pendiente.md`, que describía lo que faltaba antes de tener credenciales.

## 1. Resultado en una línea

La aplicación arranca con el perfil `supabase`, Hibernate valida el esquema, y el flujo completo (login, clientes, vehículos, ventas, mantenimientos, reporte y conversión USD) funciona contra la base real. En el camino se encontró y corrigió **una incompatibilidad de datos** que impedía leer y escribir vehículos.

## 2. Evidencia de la conexión

| Comprobación | Resultado |
|---|---|
| Resolución del pooler | `aws-0-us-west-2.pooler.supabase.com` → IPv4 (35.160.209.8, 44.238.118.41, 54.70.143.232) |
| TCP puertos 5432 y 6543 | Ambos conectan |
| Autenticación JDBC | Correcta con el usuario `postgres.<referencia>` |
| Servidor | PostgreSQL 17.11 |
| Flyway | V1 (baseline), V2, V3, V4, V5 aplicadas; V6 agregada y aplicada en esta sesión |
| Hibernate | `ddl-auto: validate` superado: `Started AutoDriveMotorsApplication` |
| Proyecto activo | La Data API con clave secreta responde datos reales |

## 3. El defecto encontrado y su corrección

### Síntoma

Con la conexión ya funcionando, tres endpoints respondían **HTTP 500**:

```text
GET /api/vehiculos      -> 500
GET /api/ventas         -> 500
GET /api/mantenimientos -> 500
```

Y `GET /api/reportes/resumen` informaba **0 vehículos** en todos los estados, aunque la tabla tenía 5 filas.

### Causa raíz

La tabla heredada guardaba el estado con formato de presentación y su restricción solo aceptaba esos textos:

```text
vehiculos.estado = 'Disponible' | 'Vendido' | 'En mantenimiento'
chk_vehiculos_estado: CHECK (estado IN ('Disponible','Vendido','En mantenimiento'))
```

La entidad `Vehiculo` usa `@Enumerated(EnumType.STRING)` con las constantes `DISPONIBLE`, `VENDIDO` y `EN_MANTENIMIENTO`. Consecuencias:

1. Al leer cualquier vehículo, Hibernate no podía convertir `Disponible` a la enumeración y lanzaba una excepción. Como `VentaService` y `MantenimientoService` navegan al vehículo para componer su respuesta, **los tres endpoints fallaban por el mismo motivo**.
2. `countByEstado('DISPONIBLE')` no encontraba filas: de ahí los ceros del reporte.
3. Aunque se hubiera podido leer, ninguna venta nueva se habría guardado: el `CHECK` habría rechazado el valor `VENDIDO`.

Ninguna validación de Hibernate detecta esto: la columna existe y es del tipo correcto (`varchar`), pero su **contenido** no pertenece al contrato de la aplicación.

### Corrección: migración V6

`src/main/resources/db/migration-postgresql/V6__normalize_estado_vehiculo.sql`

1. Elimina la restricción heredada.
2. Normaliza los datos existentes a las constantes exactas de la entidad, cubriendo variantes de espacios y mayúsculas.
3. Crea una restricción nueva que solo admite `DISPONIBLE`, `VENDIDO` y `EN_MANTENIMIENTO`, como centinela para el futuro.
4. Amplía las columnas de texto cortas para que coincidan con las longitudes de las entidades: `nombre` 100→80 se mantiene, `email` 150→160, `placa` 10→12, `marca` 50→60, `modelo` 50→80, `descripcion` text→VARCHAR(500), `telefono` 20→30. Son ampliaciones: no se recorta ningún dato.

Evidencia de la aplicación:

```text
Current version of schema "public": 5
Migrating schema "public" to version "6 - normalize estado vehiculo"
Successfully applied 1 migration to schema "public", now at version v6
Started AutoDriveMotorsApplication in 21.889 seconds
```

## 4. Prueba funcional completa contra la base real

`scripts/prueba-api-supabase.ps1` — **20 pruebas, 0 fallas**:

| Bloque | Pruebas | Resultado |
|---|---|---|
| Autenticación y CSRF | login de administrador (302), `GET /api/sesion/csrf` | OK |
| Lecturas reales | clientes 4, vehículos 5, ventas 1, mantenimientos 1, disponibles 3 | OK |
| Reporte | `totalVentas=1`, `montoTotalVentas=109250000.00`, `disponibles=3`, `vendidos=1`, `enMantenimiento=1` | OK |
| Escritura real | `POST /api/clientes` 201, `POST /api/vehiculos` 201, `POST /api/ventas` 201 | OK |
| Regla de negocio | el vehículo pasa a `VENDIDO` tras la venta | OK |
| Errores | id inválido 400, inexistente 404, documento duplicado 409, venta duplicada 409 | OK |
| Servicio externo | conversión USD 200 con tasa real | OK |
| Seguridad | `GET /api/clientes` sin sesión 401 | OK |
| Eliminación | cliente con venta 409, vehículo con venta 409 | OK |

Respuestas reales de la base:

```json
GET /api/ventas/1
{"id":1,"clienteNombre":"Carlos Mendoza","vehiculoDescripcion":"Mazda CX-30 (XYZ789)",
 "descuentoAplicado":5750000.00,"montoTotal":109250000.00}

GET /api/vehiculos/2/conversion-usd
{"vehiculoId":2,"precioCop":115000000.00,"tasaCopPorUsd":3199.575723,"precioUsd":35942.27}
```

Los datos de prueba se eliminaron después de verificar; la base quedó con los datos del taller: 4 clientes, 5 vehículos, 1 venta y 1 mantenimiento.

### Cómo limpiar los datos que deja la prueba

La API no expone `DELETE` para ventas ni mantenimientos por decisión de diseño, y tampoco permite borrar un cliente o un vehículo que tenga una venta (responde 409). Para dejar la base como estaba, borre en este orden desde el SQL Editor de Supabase:

```sql
DELETE FROM ventas    WHERE vehiculo_id IN (SELECT id FROM vehiculos WHERE placa LIKE 'TEST%');
DELETE FROM mantenimientos WHERE vehiculo_id IN (SELECT id FROM vehiculos WHERE placa LIKE 'TEST%');
DELETE FROM vehiculos WHERE placa LIKE 'TEST%';
DELETE FROM clientes  WHERE documento LIKE 'TEST-%';
```

## 5. Seguridad comprobada en el proyecto real

| Prueba | Resultado |
|---|---|
| Data API con clave **publishable** sobre `clientes` | `42501 permission denied for table clientes` |
| Data API con clave **secreta** | Responde datos (uso administrativo fuera del navegador) |
| API de la aplicación sin sesión | 401 JSON |

El primer resultado es la confirmación práctica de que la migración V2 hizo su trabajo: el navegador no puede leer las tablas por la Data API. La clave secreta se usó solo desde la terminal para diagnóstico y **no** se guardó en el repositorio.

## 6. Estado de los perfiles

| Perfil | Base | Estado |
|---|---|---|
| `dev` (por defecto) | H2 en memoria | Verificado: `mvn clean test` 9/9 en verde, Flyway aplica solo V1 |
| `supabase` | PostgreSQL 17.11 en Supabase | Verificado: arranque, validación y 20 pruebas funcionales |

`mvn clean test` final: **Tests run: 9, Failures: 0, Errors: 0 — BUILD SUCCESS**.

## 7. Desviaciones conocidas que quedan documentadas

Ninguna impide el funcionamiento; se registran para transparencia:

1. `clientes.telefono` admite `NULL` aunque la entidad declara `nullable = false`. Corregirlo exigiría inventar un teléfono para registros históricos; la entidad no se ha relajado.
2. Los identificadores usan `bigserial` en lugar de `GENERATED ... AS IDENTITY`; JPA los maneja igual.
3. Las marcas de tiempo son `timestamp with time zone` y la aplicación las trata como `LocalDateTime`: los valores se guardan y se leen en UTC.
4. Los documentos `LEGACY-<id>` de los cuatro clientes heredados siguen pendientes de reemplazo por documentos reales.
5. La cuenta de administrador vive en memoria con BCrypt desde variables de entorno; no hay tabla de usuarios ni roles.

## 8. Cómo repetir la verificación

```powershell
# 1. Variables reales en .env (ignorado por Git)
Copy-Item .env.example .env

# 2. Diagnóstico de variables y red, sin arrancar nada
powershell -ExecutionPolicy Bypass -File scripts\supabase-run.ps1 -SoloDiagnostico

# 3. Arranque con el perfil supabase (use -Puerto si el 8080 está ocupado)
powershell -ExecutionPolicy Bypass -File scripts\supabase-run.ps1 -Puerto 8099

# 4. En otra terminal: prueba funcional completa contra la base real
powershell -ExecutionPolicy Bypass -File scripts\prueba-api-supabase.ps1 -BaseUrl http://127.0.0.1:8099
```

Si en el futuro aparece un error de Hibernate por columna faltante o tipo distinto, ejecute `Database/02_scripts/03_verificar_esquema_supabase.sql` en el SQL Editor de Supabase y cree la migración siguiente.

## 9. Pendientes del equipo

1. Confirmar en Git los archivos nuevos del perfil Supabase: `application-supabase.yml`, `db/migration*`, `.gitignore`, `.env.example`, `scripts/` y esta documentación. Hoy siguen sin confirmar, así que un clon del repositorio no reproduce nada de esto.
2. Reemplazar los documentos `LEGACY-<id>` por documentos reales desde el módulo de clientes.
3. Definir el proceso para devolver un vehículo de `EN_MANTENIMIENTO` a `DISPONIBLE`, si el docente lo exige.
4. Considerar una tarea de limpieza para los vehículos y ventas creados por las pruebas funcionales en entregas futuras: la API no expone `DELETE` para ventas ni mantenimientos por diseño.
5. No publicar el proyecto con la cuenta de administrador de prueba; definir credenciales propias y `AUTODRIVE_SESSION_COOKIE_SECURE=true` al desplegar bajo HTTPS.

## 10. Nota operativa sobre el pooler

El pooler de Supabase bloquea temporalmente las conexiones nuevas con `(ECIRCUITBREAKER) too many authentication failures` después de varios intentos fallidos de autenticación. Se observó al reintentar con rapidez; se libera solo tras unos minutos. Recomendaciones:

- No reintentar en bucle contra la base: espere y haga un solo intento.
- Si ocurre, la aplicación arranca bien en cuanto se libera, sin cambios de código.
- Use el pooler de sesión (puerto 5432). Con el de transacciones (6543) agregue `&prepareThreshold=0` a la URL.
