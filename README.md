# AutoDrive Motors JAO

Sistema de Gestión Vehicular para el taller final de Análisis y Diseño de Software. Integra una aplicación MVC con Thymeleaf y una API REST de Spring Boot para clientes, vehículos, ventas y mantenimientos.

## Lo que implementa

- Clientes: registro, consulta, actualización y eliminación; documento y correo únicos.
- Vehículos: CRUD, placa única, búsqueda por marca, disponibilidad y estados.
- Ventas: asociación cliente–vehículo, fecha automática, descuento del 5 % para valores superiores a 100.000.000 COP y protección contra ventas duplicadas.
- Mantenimientos: registro e historial; un vehículo con mantenimiento queda en estado `EN_MANTENIMIENTO` y no se ofrece en ventas.
- Reporte REST: resumen de ventas, total vendido y vehículos por estado.
- Moneda: consulta de tasa USD→COP y conversión de un vehículo COP→USD.
- Persistencia: JPA/Hibernate, migración PostgreSQL versionada por Flyway y perfil para Supabase.
- Vistas: formularios y tablas que consumen la misma API REST, sin acceso directo del navegador a la base de datos.
- Acceso: inicio y cierre de sesión de administrador con BCrypt, sesión de 30 minutos y CSRF en formularios y solicitudes JSON.

## Arquitectura

```text
Vista Thymeleaf / JavaScript
            │ fetch JSON
            ▼
Controladores REST → Servicios (reglas y transacciones) → DAO JPA → PostgreSQL/Supabase
                                      │
                                      └→ Cliente de tasa de cambio externa
```

Los DTO evitan exponer entidades JPA directamente. La API devuelve errores uniformes: 400 para solicitudes inválidas, 404 para recursos inexistentes, 409 para reglas o conflictos y 503 para la tasa externa no disponible.

## Ejecutar en desarrollo local

Requiere Java 21 y Maven. El perfil por defecto `dev` usa H2 en memoria con datos mínimos descartables.

```powershell
mvn spring-boot:run
```

Abra `http://localhost:8080`. Las pruebas unitarias no llaman a servicios externos:

```powershell
mvn test
```

El perfil local permite probar el acceso con `admin` / `admin-local-2026`. Es una credencial ficticia exclusiva para `dev`; nunca la use fuera del equipo local.

## Ejecutar con Supabase

El profesor autorizó PostgreSQL/Supabase como motor relacional. La aplicación se conecta por JDBC desde el backend; **no** se usan ni se publican claves de Supabase en el frontend.

1. Obtenga la contraseña de base de datos del dueño del proyecto Supabase por un canal privado.
2. Defina las variables solo en su terminal. Para la conexión directa entregada al equipo:

```powershell
$env:SPRING_PROFILES_ACTIVE = "supabase"
$env:SUPABASE_DB_URL = "jdbc:postgresql://db.<referencia-del-proyecto>.supabase.co:5432/postgres?sslmode=require"
$env:SUPABASE_DB_USER = "postgres"
$env:SUPABASE_DB_PASSWORD = Read-Host "Contraseña privada de Supabase"
$env:AUTODRIVE_ADMIN_USER = Read-Host "Usuario administrador"
$env:AUTODRIVE_ADMIN_PASSWORD = Read-Host "Contraseña fuerte del administrador"
mvn spring-boot:run
```

En el primer arranque, Flyway ejecuta `src/main/resources/db/migration/V1__create_autodrive_schema.sql`, aplica la protección de las tablas frente a la Data API de Supabase y JPA valida el esquema. Si la red local no admite la conexión directa del proveedor, use en `SUPABASE_DB_URL` la URL de pooler que muestra el panel de Supabase; no cambie ni suba una contraseña al repositorio.

`Database/02_scripts/02_data_inserts.sql` es opcional: solo sirve para poblar una base vacía ya migrada y no se ejecuta automáticamente.

## Endpoints principales

| Recurso | Operaciones |
|---|---|
| `/api/clientes` | `GET`, `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}` |
| `/api/vehiculos` | `GET`, `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}`, `GET /disponibles`, `GET /marca/{marca}` |
| `/api/ventas` | `GET`, `GET /{id}`, `POST` |
| `/api/mantenimientos` | `GET`, `GET /{id}`, `GET ?vehiculoId={id}`, `POST` |
| `/api/reportes/resumen` | `GET` |
| `/api/tasa-cambio` | `GET` |
| `/api/vehiculos/{id}/conversion-usd` | `GET` |
| `/api/sesion/csrf` | `GET` autenticado; token para clientes no HTML como Postman |

La colección de pruebas está en [`postman/AutoDrive-Motors.postman_collection.json`](postman/AutoDrive-Motors.postman_collection.json). Importe el archivo, defina `adminUser` y `adminPassword`, y ejecute primero la carpeta **Autenticación**.

## Tasa de cambio

Por defecto se consulta `https://open.er-api.com/v6/latest/USD`. Se puede reemplazar por `AUTODRIVE_TASA_CAMBIO_URL`. La interfaz enlaza la atribución requerida a ExchangeRate-API. La tasa es informativa y se consulta bajo demanda; no se guarda como precio del vehículo.

## Organización

```text
src/main/java/com/autodrive/motors/
├── api/        controladores REST
├── service/    reglas de negocio y transacciones
├── dao/        repositorios JPA
├── model/      entidades
├── dto/        contratos JSON
├── external/   integración de tasa de cambio
├── exception/  manejo uniforme de errores
└── web/        rutas de las vistas MVC
```

El avance fechado de esta entrega está en `01_Documentacion/09_Avances/`.
