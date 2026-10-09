# Pruebas de la API con Postman

Sistema de Gestión Vehicular "AutoDrive Motors JAO". Este documento explica cómo importar la colección de Postman, en qué orden ejecutarla y qué respuesta se espera de cada petición.

Archivo de la colección: [`postman/AutoDrive-Motors.postman_collection.json`](../../postman/AutoDrive-Motors.postman_collection.json)

## 1. Requisitos

- Java 21 y Maven.
- Postman (aplicación de escritorio o web).
- La aplicación corriendo en `http://localhost:8080`:

```powershell
mvn spring-boot:run
```

El perfil por defecto es `dev` y usa H2 en memoria con datos de ejemplo, por lo que cada reinicio devuelve la base a su estado inicial. Las credenciales del administrador de desarrollo están en el `README.md` y solo sirven en local.

## 2. Importar y configurar

1. En Postman: **Import** y seleccionar `AutoDrive-Motors.postman_collection.json`.
2. Abrir la colección, pestaña **Variables**, y completar en *Current value*:
   - `adminUser`: usuario del administrador.
   - `adminPassword`: contraseña del administrador.
3. Dejar `baseUrl` en `http://localhost:8080`.

Las variables `clienteId`, `vehiculoId`, `ventaId`, `mantenimientoId`, `csrfToken` y `csrfHeader` las llenan las propias peticiones. No deben escribirse a mano.

Las credenciales se escriben solo en *Current value* para que no queden en el archivo exportado.

## 3. Por qué hay que iniciar sesión primero

Todas las rutas `/api/**` exigen sesión de administrador y, para los métodos que modifican datos (`POST`, `PUT`, `PATCH`, `DELETE`), un token CSRF. Sin sesión la API responde `401`; sin token, `403`.

Por eso la carpeta **Autenticación** se ejecuta siempre primero:

| # | Petición | Resultado esperado |
|---|---|---|
| 1 | `GET /login` | 200. Guarda `csrfToken`. |
| 2 | `POST /login` | 200 o 302. Postman conserva la cookie de sesión. |
| 3 | `GET /api/sesion/csrf` | 200. Renueva `csrfToken` con el de la sesión. |

La sesión vence a los 30 minutos sin actividad. Si aparece un 401 o el HTML del login, se repiten los tres pasos.

## 4. Correspondencia con los endpoints del taller

El enunciado pide los endpoints mínimos sin prefijo. En la implementación todos cuelgan de `/api`, que es lo que protege el control de acceso.

| Endpoint pedido | Endpoint implementado |
|---|---|
| `GET /clientes` | `GET /api/clientes` |
| `POST /clientes` | `POST /api/clientes` |
| `PUT /clientes/{id}` | `PUT /api/clientes/{id}` |
| `DELETE /clientes/{id}` | `DELETE /api/clientes/{id}` |
| `GET /vehiculos` | `GET /api/vehiculos` |
| `POST /vehiculos` | `POST /api/vehiculos` |
| `GET /vehiculos/disponibles` | `GET /api/vehiculos/disponibles` |
| `GET /vehiculos/marca/{marca}` | `GET /api/vehiculos/marca/{marca}` |
| `POST /ventas` | `POST /api/ventas` |
| `GET /ventas` | `GET /api/ventas` |
| `POST /mantenimientos` | `POST /api/mantenimientos` |
| `GET /mantenimientos` | `GET /api/mantenimientos` |

Además de los mínimos, la colección cubre: consulta por id de clientes, vehículos, ventas y mantenimientos; actualización y eliminación de vehículos; historial de mantenimientos por vehículo; resumen operativo; tasa de cambio; conversión de un vehículo a USD y finalización de mantenimiento.

## 5. Orden de ejecución

La colección tiene 26 peticiones en cinco carpetas. Se ejecutan de arriba hacia abajo, con una excepción.

| Carpeta | Peticiones | Esperado |
|---|---|---|
| Autenticación | 3 | 200 / 302 / 200 |
| Clientes | listar, registrar, consultar, actualizar | 200, 201, 200, 200 |
| Vehículos | listar, registrar, consultar, disponibles, por marca, actualizar, convertir a USD, finalizar mantenimiento | 200, 201, 200, 200, 200, 200, 200, ver nota |
| Ventas y reportes | registrar, listar, consultar, resumen | 201, 200, 200, 200 |
| Mantenimientos y tasa | registrar, listar, historial, consultar, tasa USD a COP | 201, 200, 200, 200, 200 |

**Las dos peticiones DELETE (cliente y vehículo) van al final.** Borran los registros que usan las ventas y los mantenimientos; si se ejecutan antes, esas peticiones responden 404. Si se usa el *Runner*, hay que desmarcarlas o moverlas al final.

Nota sobre `Finalizar mantenimiento`: solo funciona con un vehículo en estado `EN_MANTENIMIENTO` (por ejemplo, el creado por la petición *Registrar mantenimiento*). Si el vehículo está disponible o vendido responde 409.

## 6. Evidencia de errores y reglas de negocio

Cada caso se prueba duplicando la petición y cambiando el dato indicado.

| Código | Caso | Cómo provocarlo |
|---|---|---|
| 400 | Correo inválido | `POST /api/clientes` con `"email": "no-es-un-correo"` |
| 400 | Id fuera de rango | `GET /api/clientes/0` |
| 400 | Precio no positivo | `POST /api/vehiculos` con `"precioCop": -1` |
| 401 | Sin sesión | `GET /api/clientes` sin haber iniciado sesión |
| 403 | Sin token CSRF | `POST /api/clientes` sin el encabezado CSRF |
| 404 | Recurso inexistente | `GET /api/clientes/9999` o `GET /api/vehiculos/9999` |
| 409 | Correo o documento repetido | registrar dos veces el mismo cliente |
| 409 | Placa repetida | `POST /api/vehiculos` con una placa existente (por ejemplo `KLM123`) |
| 409 | Venta de un vehículo vendido | `POST /api/ventas` con `vehiculoId` 4 |
| 409 | Vehículo en mantenimiento | `POST /api/ventas` con `vehiculoId` 2 |
| 409 | Mantenimiento de un vehículo vendido | `POST /api/mantenimientos` con `vehiculoId` 4 |
| 409 | Eliminar cliente con ventas | `DELETE /api/clientes/1` |
| 409 | Eliminar vehículo con mantenimientos | `DELETE /api/vehiculos/7` |
| 409 | Finalizar mantenimiento de un vehículo disponible | `PATCH /api/vehiculos/3/finalizar-mantenimiento` |
| 503 | API de tasa de cambio no disponible | enviar *Convertir a USD* o *Tasa USD a COP* sin conexión a internet |

Descuento del 5 %: al vender un vehículo de más de $100.000.000 COP, la respuesta trae `descuentoAplicado` igual al 5 % del precio y `montoTotal` igual al precio menos ese descuento. Cualquier venta registra además `fechaVenta`, generada por el sistema.

## 7. Datos de ejemplo del perfil dev

| Vehículo | Placa | Estado inicial |
|---|---|---|
| 1 | FQT45D | DISPONIBLE |
| 2 | KDM28F | EN_MANTENIMIENTO |
| 3 | RUN91B | DISPONIBLE |
| 4 | TXA37G | VENDIDO |
| 5 | BNL62C | VENDIDO |
| 6 | KLM123 | DISPONIBLE |
| 7 | MNP456 | EN_MANTENIMIENTO |
| 8 | HZR807 | VENDIDO (con descuento) |
| 9 | SJT254 | DISPONIBLE |

La base trae 6 clientes, 3 ventas y 3 mantenimientos. Al arrancar, `GET /api/reportes/resumen` devuelve 3 ventas, 4 vehículos disponibles, 3 vendidos y 2 en mantenimiento.

## 8. Problemas frecuentes

| Síntoma | Causa | Solución |
|---|---|---|
| `Could not send request` | La aplicación no está corriendo. | Ejecutar `mvn spring-boot:run`. |
| 401 o respuesta con HTML | La sesión venció. | Repetir la carpeta Autenticación. |
| 403 en POST, PUT, PATCH o DELETE | Falta el token CSRF. | Ejecutar `GET /api/sesion/csrf`. |
| 404 en ventas o mantenimientos | Se ejecutó un DELETE antes de tiempo. | Reiniciar la aplicación. |
| 409 al registrar | Documento, correo o placa ya existen. | Cambiar el dato. |
| 503 en la conversión a USD | No hay acceso a la API de tasa de cambio. | Revisar la conexión a internet. |
