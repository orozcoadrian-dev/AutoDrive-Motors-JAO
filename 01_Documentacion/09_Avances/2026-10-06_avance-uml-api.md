# Avance de diseno UML y API

**Fecha:** 2026-10-06
**Responsabilidad abordada:** DAO/repositorios, API REST, capa MVC web y funciones consumidoras de la API.

## Alcance revisado

- Taller final: gestion de clientes, vehiculos, ventas, mantenimientos y conversion COP/USD mediante API externa.
- Requisitos funcionales RF-01 a RF-25, requisitos no funcionales RNF-01 a RNF-25, historias de usuario y narrativas de casos de uso del repositorio.
- Diagramas de dominio y casos de uso ya existentes.

## Entregables creados en este avance

1. [Diagrama de clases de implementacion](../07_Diagrama_de_clases_aplicacion/Diagrama_de_clases_aplicacion.md) y su exportacion [PDF](../../../output/pdf/diagrama-de-clases-aplicacion.pdf). Incluye controladores REST, servicios, repositorios DAO/JPA, DTOs, entidades, manejo de errores e integracion de tasa de cambio propuesta.
2. [Diagramas de secuencia](../08_Diagramas_de_secuencia/Diagramas_de_secuencia.md) y su exportacion [PDF](../../../output/pdf/diagramas-de-secuencia.pdf), para registrar venta, registrar mantenimiento y convertir el valor de un vehiculo a USD.
3. Proyecto Spring Boot inicial en `pom.xml` con una implementacion MVC parcial y ejecutable: entidades JPA, DAO Spring Data, servicios transaccionales, DTOs validados, controladores REST y controladores de vistas.
4. Vistas Thymeleaf para inicio, clientes, vehiculos y ventas. `static/js/app.js` consume la API REST de la misma aplicacion con `fetch`, muestra estados de carga/error/vacio y no inserta texto de la API como HTML.
5. Persistencia implementada: `Cliente`, `Vehiculo` y `Venta`. El mantenimiento y la conversion COP/USD se conservan como pendientes de integracion; no se simula una API externa ni se persiste una entidad que aun no tiene contrato validado por el equipo.
6. Prueba unitaria de la regla de venta: descuento del 5 % sobre valores mayores de $100.000.000 COP y bloqueo de venta si el vehiculo no esta disponible.

Los diagramas se mantienen en Mermaid dentro de Markdown para versionarlos en GitHub y ahora tambien se entregan como PDF. La exportacion se reviso visualmente pagina por pagina: tres paginas por documento, sin texto recortado ni elementos superpuestos.

## Flujo MVC implementado

`Vista Thymeleaf -> app.js (fetch) -> /api/* REST -> Servicio transaccional -> DAO/JPA -> H2 en perfil dev o MySQL en perfil mysql`.

- Las vistas solo entregan estructura y accesibilidad; no contienen reglas de negocio ni acceso directo a base de datos.
- Las ventas marcan el vehiculo como `VENDIDO`; `@Version` protege contra doble confirmacion concurrente y el conflicto devuelve HTTP 409.
- El perfil `dev` usa H2 en memoria, datos de ejemplo y se limita a `127.0.0.1`. El perfil `mysql` toma URL, usuario y contrasena desde variables de entorno y valida el esquema, sin credenciales dentro del repositorio.

## Verificaciones realizadas

- Se verifico que `Taller final.pdf` recibido y `01_Documentacion/01_problema_raiz/Taller-final.pdf` tienen el mismo SHA-256: `39C2D31E99E4F6DE528C7959315CD1568A25BDED7F2CEB6F16BCDD7FF2E85D01`.
- Se extrajo y se reviso el texto de los PDF relevantes; se inspeccionaron visualmente las paginas del taller y las tablas de requisitos funcionales y no funcionales.
- Se genero y se renderizo cada PDF con ReportLab/Poppler para inspeccion visual. Ambos tienen tres paginas y presentan relaciones/mensajes separados de forma legible.
- `pom.xml` se valido como XML y `static/js/app.js` paso `node --check`.
- Se verifico por inspeccion estatica que las rutas usadas por las vistas existen en los controladores REST y que los DAO extienden `JpaRepository`.
- Se agrego la prueba `VentaServiceTest`, pero no se ejecuto porque Maven no esta instalado en el entorno y no se descargaron ni ejecutaron dependencias del proyecto sin un aislamiento de ejecucion adecuado.

## Revision enfocada de seguridad de la API

| Estado | Hallazgo / evidencia | Tratamiento |
|---|---|---|
| Confirmado | Entradas REST validadas con Bean Validation; la capa de servicio normaliza placa/correo y aplica reglas de unicidad y venta. | Implementado. |
| Confirmado | La respuesta de error generica no expone trazas, SQL ni detalles internos; DTOs evitan serializar entidades JPA. | Implementado. |
| Confirmado | La vista y API comparten origen, no se habilito CORS global y el perfil de desarrollo escucha solo en `127.0.0.1`. | Implementado para desarrollo local. |
| Confirmado | Sin autenticacion/autorizacion porque el taller no define usuarios ni roles. Si se despliega fuera de desarrollo, los endpoints quedan abiertos. | Pendiente definir con el equipo antes de exponer el servicio. |
| Needs validation | El esquema MySQL del repositorio esta vacio/no se ha validado contra estas entidades; el perfil `mysql` usa `ddl-auto: validate`. | Crear y revisar la migracion MySQL antes de activar ese perfil. |
| Needs validation | Pruebas Maven, arranque real, transaccion concurrente e integracion navegador/API no se ejecutaron en este entorno. | Ejecutar `mvn test` y prueba manual local cuando haya Maven y un entorno aislado. |

## Pendiente para la siguiente fase

- Crear y acordar la migracion MySQL: `Database/02_scripts/01_squema.sql` estaba vacio al revisarlo. Tambien hay que resolver la diferencia MySQL/PostgreSQL del material existente antes de activar el perfil `mysql`.
- Definir autenticacion y autorizacion si la API se desplegara fuera de localhost.
- Implementar mantenimiento y la integracion de tasa de cambio con proveedor, timeout, manejo de fallo y pruebas de contrato.
- Ejecutar `mvn test`, iniciar el perfil `dev` en un entorno aislado y completar una coleccion Postman con los codigos 201, 400, 404 y 409.
