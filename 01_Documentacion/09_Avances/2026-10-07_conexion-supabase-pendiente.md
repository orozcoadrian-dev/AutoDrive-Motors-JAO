# Conexión con Supabase: estado real y lo que falta

**Fecha:** 2026-10-07
**Responsabilidad abordada:** cierre de la brecha entre el código del repositorio y el proyecto Supabase del equipo.

Este documento responde una sola pregunta: **¿qué falta para que la aplicación funcione contra Supabase?**
No reemplaza a `2026-10-07_completacion-supabase-api.md`; lo continúa con la verificación del estado actual del repositorio.

## 1. Resumen: qué ya está listo y qué falta

| Pieza | Estado | Evidencia |
|---|---|---|
| Dependencia PostgreSQL | Lista | `pom.xml` incluye `org.postgresql:postgresql` |
| Dependencia Flyway para PostgreSQL | Lista | `pom.xml` incluye `flyway-core` y `flyway-database-postgresql` |
| Perfil `supabase` | Listo | `src/main/resources/application-supabase.yml` |
| Migraciones versionadas | Listas | `db/migration/V1` + `db/migration-postgresql/V2…V5` presentes también en `target/classes` |
| Esquema alineado con las entidades JPA | Parcialmente verificado | V3 (documento), V4 (fecha `DATE`), V5 (`version`) se agregaron tras errores reales de Hibernate |
| Contraseña y URL reales de Supabase | **Falta** | No existe en el repositorio ni en el entorno de esta máquina |
| Cuenta de administrador para el login | **Falta definir** | `application-supabase.yml` no trae credenciales; exige `AUTODRIVE_ADMIN_USER` y `AUTODRIVE_ADMIN_PASSWORD` |
| Red hacia el host de la base | **A verificar según la red del equipo** | La conexión directa de Supabase suele requerir IPv6 (ver punto 3) |
| Esquema remoto sin desviaciones restantes | **A verificar** | Para eso se agregó `Database/02_scripts/03_verificar_esquema_supabase.sql` |

Conclusión: **no falta código de conexión**. Falta (a) credenciales y cuenta de administrador, (b) resolver el camino de red hacia el host de la base y (c) confirmar que el esquema heredado de Supabase ya coincide con lo que exige JPA.

## 2. Cómo funciona la conexión (y qué NO se usa)

```text
Navegador (HTML + JS)  →  /api/*  →  Spring Boot  →  JDBC + HikariCP  →  PostgreSQL de Supabase
```

- La aplicación usa **JDBC/TCP con el driver de PostgreSQL**. Es el mismo camino que usa cualquier backend.
- **No se usa la Data API de Supabase** ni `supabase-js` en el navegador. Por eso `V2__protect_supabase_data_api.sql` activa RLS y revoca permisos a `anon` y `authenticated`: intentar leer las tablas desde el frontend debe fallar, no funcionar.
- Hibernate corre con `ddl-auto: validate`: **no crea ni corrige tablas**. Si algo no coincide, la aplicación no arranca. Flyway es el único que modifica el esquema.

## 3. Riesgo de red que hay que resolver antes de probar

En esta máquina se comprobó, sin credenciales, que:

- `aws-0-us-east-1.pooler.supabase.com` **sí resuelve** (IPv4: 52.45.94.125, 44.208.221.186, 44.216.29.125).
- Un host directo `db.<referencia>.supabase.co` **no resuelve** en una red solo-IPv4 ("Host desconocido").

La conexión directa de Supabase publica registros **AAAA (IPv6)**. En redes sin IPv6 (casa, universidad, red corporativa) la aplicación dirá `UnknownHostException` y parecerá un error de código cuando en realidad es de red. Solución: usar la **URL del pooler** que muestra el panel de Supabase.

| Camino | Host de ejemplo | Puerto | Cuándo usarlo |
|---|---|---|---|
| Conexión directa | `db.<ref>.supabase.co` | 5432 | Red con IPv6 |
| Pooler de sesión | `aws-0-<region>.pooler.supabase.com` | 5432 | Red sin IPv6 (recomendado) |
| Pooler de transacciones | `aws-0-<region>.pooler.supabase.com` | 6543 | Red sin IPv6; **requiere** `&prepareThreshold=0` en la URL, porque ese pooler no soporta sentencias preparadas de pgJDBC |

Detalle adicional: con el pooler, el usuario lleva la referencia del proyecto (`postgres.<referencia>`), no solo `postgres`.

## 4. Procedimiento de conexión, paso a paso

1. **Pedir la contraseña** de la base al dueño del proyecto Supabase por un canal privado. No se escribe en el repositorio, ni en capturas, ni en este documento.
2. **Crear el archivo de variables** en la raíz del proyecto:

   ```powershell
   Copy-Item .env.example .env
   ```

3. **Completar `.env`** con la URL del pooler o la conexión directa, la contraseña y las credenciales de administrador. `.env` está en `.gitignore`: no se sube.
4. **Verificar variables y red sin arrancar nada**:

   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts\supabase-run.ps1 -SoloDiagnostico
   ```

   El script avisa si falta una variable, si el perfil activo no es `supabase` o si el host/puerto no responde.
5. **Comprobar el esquema remoto** (evita el error de Hibernate más frecuente). Abra el SQL Editor de Supabase y ejecute:

   `Database/02_scripts/03_verificar_esquema_supabase.sql`

   Solo debe aparecer la fila informativa de `flyway_schema_history` con estado `OK`. Si aparece `FALTA`, `TIPO` o `NULABLE`, cree `V6__…sql` en `db/migration-postgresql/` con la corrección; **nunca edite V1**.
6. **Arrancar la aplicación**:

   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts\supabase-run.ps1
   ```

   Equivale a definir las variables de entorno y ejecutar `mvn clean spring-boot:run`. Se espera que Flyway aplique hasta V5 y que aparezca `Started AutoDriveMotorsApplication`.
7. **Probar el flujo real**: abrir `http://127.0.0.1:8080`, entrar con el usuario administrador, registrar un cliente, un vehículo, una venta y un mantenimiento, y confirmar en el panel de Supabase (Table Editor) que las filas se guardaron.

## 5. Errores esperables y su causa

| Mensaje | Causa | Solución |
|---|---|---|
| `UnknownHostException` / `Host desconocido` | Red sin IPv6 contra el host directo | Use la URL del pooler |
| `FATAL: password authentication failed` | Contraseña incorrecta, o URL del pooler con usuario sin `.<referencia>` | Corregir `.env` |
| `prepared statement "S_1" already exists` | Pooler de transacciones (6543) con sentencias preparadas | Agregar `&prepareThreshold=0` a la URL o usar el pooler de sesión |
| `Schema-validation: missing column [documento]` o `[version]` | El esquema remoto no recibió V3/V5 (la base fue marcada como baseline) | Ejecutar el script del punto 5 y completar con V6 |
| `wrong column type ... found [timestamptz], expecting [date]` | Columna de fecha sin aplicar V4 | Igual que el caso anterior |
| `Could not resolve placeholder 'SUPABASE_DB_URL'` | Variables no definidas en esa terminal | Use el script, o defínalas con `$env:` |
| `mvn : Could not open file channel ... .m2\repository\.locks` | Otra consola retuvo el candado del repositorio local de Maven | Cerrar la otra consola; ejecutar `mvn clean spring-boot:run` |
| El login falla con credenciales correctas en `.env` | La sesión se arrancó en el perfil `dev`, o la cookie quedó `Secure` en HTTP | Confirmar `SPRING_PROFILES_ACTIVE=supabase` y `AUTODRIVE_SESSION_COOKIE_SECURE=false` |

## 6. Trampas de configuración detectadas

1. **El perfil anterior persiste en la terminal.** Si una consola anterior definió `SUPABASE_DB_URL`, una ejecución posterior sin `SPRING_PROFILES_ACTIVE` arranca con el perfil `dev` (H2 en memoria) y los datos parecen "no guardarse". Peor aún: el perfil `dev` ejecuta `create-drop`, así que nunca toca Supabase de forma silenciosa, pero la prueba queda inválida. El script `supabase-run.ps1` fija el perfil y avisa si no es `supabase`.
2. **Spring Boot no lee `.env` por sí solo.** El archivo solo funciona a través del script o definiendo las variables con `$env:NOMBRE = "valor"` en la misma consola.
3. **Credenciales de administrador vacías.** `application.yml` define `AUTODRIVE_ADMIN_USER` y `AUTODRIVE_ADMIN_PASSWORD` con valor por defecto vacío y `application-supabase.yml` no los sobreescribe. Si quedan vacíos, la aplicación arranca pero el login no funciona; no es un fallo de Supabase.
4. **La contraseña de la base no es la del usuario del panel.** Es la contraseña de PostgreSQL en *Project Settings → Database*, distinta de la contraseña de la cuenta de Supabase.
5. **Proyectos gratuitos pausados.** Si el proyecto lleva días inactivo, la conexión se rechaza hasta reactivarlo en el panel.

## 7. Verificaciones realizadas en esta revisión

- Lectura estática de `pom.xml`, `application*.yml`, entidades JPA y las cinco migraciones: la configuración del perfil `supabase` está completa (URL, usuario, contraseña, driver, `ddl-auto: validate`, `baseline-on-migrate: true`, ubicaciones de migración de H2 y de PostgreSQL).
- `target/classes` contiene las migraciones V1–V5, es decir, el último `mvn test` compiló el código con todas las correcciones.
- Resolución DNS comprobada en esta máquina: pooler IPv4 resuelve; host directo no resuelve (ver punto 3).
- `Database/02_scripts/03_verificar_esquema_supabase.sql` se probó contra dos esquemas simulados: uno ya alineado (no reporta desviaciones) y uno heredado con las desviaciones descritas en los avances anteriores (detecta correctamente `documento` y `version` ausentes, `fecha_mantenimiento` como `timestamptz`, y columnas que admiten nulos).
- `scripts/supabase-run.ps1` se probó sin `.env`, con `.env` incompleto y con host inexistente: en los tres casos informa la causa y se detiene sin arrancar la aplicación.

## 8. Pendiente de validación por el equipo

| Pendiente | Quién | Cómo se cierra |
|---|---|---|
| Conexión real con contraseña y esquema remoto | Integrante con las credenciales | Puntos 4 a 6 de este documento |
| Confirmar que Flyway llega a V5 y `Started AutoDriveMotorsApplication` aparece | Mismo integrante | Registro de consola del arranque |
| Si el script del punto 5 reporta desviaciones, crear V6 | Equipo | Nueva migración en `db/migration-postgresql/` |
| Evidencia de endpoints 201/400/404/409/503 con Postman contra Supabase | Equipo | Importar `postman/AutoDrive-Motors.postman_collection.json`, definir `adminUser` y `adminPassword` |
| Confirmar que la Data API no expone las tablas sin sesión | Equipo | Consultar `https://<ref>.supabase.co/rest/v1/clientes` con la clave `anon`: debe responder sin filas o con error |
| Publicar los archivos nuevos del perfil Supabase en Git | Equipo | Hoy `application-supabase.yml`, `.gitignore`, `.env.example`, `scripts/` y `db/migration*` están sin confirmar en `main` |

## 9. Seguridad

- No se registraron contraseñas, cadenas de conexión con secreto ni claves de proveedor en el repositorio ni en este documento.
- `.env` permanece ignorado por Git (`.gitignore` incluye `.env` y `.env.*`, con la excepción de `.env.example`).
- El script de arranque muestra las variables obligatorias en pantalla, pero enmascara cualquier valor cuyo nombre contenga `PASSWORD`.
- La frontera se mantiene: el navegador nunca recibe credenciales de base de datos; solo consume la API del mismo origen con sesión de administrador y CSRF.
