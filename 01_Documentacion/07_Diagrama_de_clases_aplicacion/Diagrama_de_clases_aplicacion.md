# Diagrama de clases de implementacion

**Proyecto:** AutoDrive Motors JAO
**Fecha:** 2026-10-06
**Alcance:** diseno de la aplicacion Spring Boot por capas; complementa el diagrama de dominio existente.
**Estado:** propuesta de implementacion. Este repositorio aun no contiene codigo Java ejecutable.

El diagrama expone la capa REST solicitada en el taller y mantiene las responsabilidades separadas: los controladores reciben y devuelven DTOs JSON, los servicios concentran reglas de negocio y transacciones, y los repositorios JPA representan el acceso DAO a la base de datos.

```mermaid
classDiagram
direction LR

class ClienteController {
  <<RestController>>
  +registrar(dto) ClienteResponseDTO
  +listar() List~ClienteResponseDTO~
  +obtenerPorId(id) ClienteResponseDTO
  +actualizar(id, dto) ClienteResponseDTO
  +eliminar(id) void
}
class VehiculoController {
  <<RestController>>
  +registrar(dto) VehiculoResponseDTO
  +listar() List~VehiculoResponseDTO~
  +listarPorMarca(marca) List~VehiculoResponseDTO~
  +listarDisponibles() List~VehiculoResponseDTO~
  +actualizar(id, dto) VehiculoResponseDTO
  +eliminar(id) void
}
class VentaController {
  <<RestController>>
  +registrar(dto) VentaResponseDTO
  +listar() List~VentaResponseDTO~
  +obtenerPorId(id) VentaResponseDTO
}
class MantenimientoController {
  <<RestController>>
  +registrar(dto) MantenimientoResponseDTO
  +listar(vehiculoId) List~MantenimientoResponseDTO~
}
class DivisaController {
  <<RestController>>
  +consultarTasaActual() TasaCambioDTO
  +convertirVehiculoAUsd(id) ConversionUsdDTO
}

class ClienteService {
  <<Service>>
  +registrar(dto) ClienteResponseDTO
  +listar() List~ClienteResponseDTO~
  +obtenerPorId(id) ClienteResponseDTO
  +actualizar(id, dto) ClienteResponseDTO
  +eliminar(id) void
  -validarCorreoUnico(email, idActual) void
}
class VehiculoService {
  <<Service>>
  +registrar(dto) VehiculoResponseDTO
  +listar() List~VehiculoResponseDTO~
  +listarPorMarca(marca) List~VehiculoResponseDTO~
  +listarDisponibles() List~VehiculoResponseDTO~
  +actualizar(id, dto) VehiculoResponseDTO
  +eliminar(id) void
  -validarPlacaUnica(placa, idActual) void
}
class VentaService {
  <<Service>>
  +registrar(dto) VentaResponseDTO
  +listar() List~VentaResponseDTO~
  +obtenerPorId(id) VentaResponseDTO
  -validarDisponibilidad(vehiculo) void
}
class MantenimientoService {
  <<Service>>
  +registrar(dto) MantenimientoResponseDTO
  +listar(vehiculoId) List~MantenimientoResponseDTO~
}
class TasaCambioService {
  <<Service>>
  +consultarTasaActual() TasaCambioDTO
  +convertirVehiculoAUsd(id) ConversionUsdDTO
}
class CalculoVentaService {
  <<Service>>
  +calcular(precioCop) ResultadoCalculoVenta
}

class ClienteRepository {
  <<Repository>>
  +existsByEmail(email) boolean
}
class VehiculoRepository {
  <<Repository>>
  +existsByPlaca(placa) boolean
  +findByMarcaIgnoreCase(marca) List~Vehiculo~
  +findByEstado(estado) List~Vehiculo~
}
class VentaRepository {
  <<Repository>>
  +findAll() List~Venta~
}
class MantenimientoRepository {
  <<Repository>>
  +findByVehiculoId(vehiculoId) List~Mantenimiento~
}
class JpaRepository {
  <<Spring Data JPA>>
}

class ClienteMapper {
  <<Component>>
  +toEntity(dto) Cliente
  +toResponseDTO(entidad) ClienteResponseDTO
}
class VehiculoMapper {
  <<Component>>
  +toEntity(dto) Vehiculo
  +toResponseDTO(entidad) VehiculoResponseDTO
}
class VentaMapper {
  <<Component>>
  +toResponseDTO(entidad) VentaResponseDTO
}
class MantenimientoMapper {
  <<Component>>
  +toEntity(dto, vehiculo) Mantenimiento
  +toResponseDTO(entidad) MantenimientoResponseDTO
}

class ClienteRequestDTO {
  <<DTO>>
}
class ClienteResponseDTO {
  <<DTO>>
}
class VehiculoRequestDTO {
  <<DTO>>
}
class VehiculoResponseDTO {
  <<DTO>>
}
class VentaRequestDTO {
  <<DTO>>
  +clienteId Long
  +vehiculoId Long
}
class VentaResponseDTO {
  <<DTO>>
}
class MantenimientoRequestDTO {
  <<DTO>>
  +vehiculoId Long
}
class MantenimientoResponseDTO {
  <<DTO>>
}
class TasaCambioDTO {
  <<DTO>>
}
class ConversionUsdDTO {
  <<DTO>>
  +valorCop BigDecimal
  +tasaCopUsd BigDecimal
  +valorUsd BigDecimal
}

class Cliente {
  <<Entity>>
  +id Long
  +nombre String
  +apellido String
  +email String
  +telefono String
  +fechaRegistro LocalDateTime
}
class Vehiculo {
  <<Entity>>
  +id Long
  +placa String
  +marca String
  +modelo String
  +anio Integer
  +precioCop BigDecimal
  +estado EstadoVehiculo
}
class Venta {
  <<Entity>>
  +id Long
  +fechaVenta LocalDateTime
  +descuentoAplicado BigDecimal
  +montoTotal BigDecimal
}
class Mantenimiento {
  <<Entity>>
  +id Long
  +fechaMantenimiento LocalDateTime
  +descripcion String
  +costo BigDecimal
}
class EstadoVehiculo {
  <<enumeration>>
  DISPONIBLE
  VENDIDO
  EN_MANTENIMIENTO
}
class ResultadoCalculoVenta {
  <<Value Object>>
  +descuento BigDecimal
  +total BigDecimal
}
class TasaCambioGateway {
  <<Component>>
  +obtenerTasaCopUsd() BigDecimal
}
class ApiExternaTasaCambio {
  <<Sistema externo>>
}
class GlobalExceptionHandler {
  <<RestControllerAdvice>>
  +manejarValidacion() ErrorResponse
  +manejarNoEncontrado() ErrorResponse
  +manejarReglaNegocio() ErrorResponse
  +manejarServicioExterno() ErrorResponse
}

ClienteController --> ClienteService
VehiculoController --> VehiculoService
VentaController --> VentaService
MantenimientoController --> MantenimientoService
DivisaController --> TasaCambioService

ClienteService --> ClienteRepository
ClienteService --> ClienteMapper
VehiculoService --> VehiculoRepository
VehiculoService --> VehiculoMapper
VentaService --> ClienteRepository
VentaService --> VehiculoRepository
VentaService --> VentaRepository
VentaService --> VentaMapper
VentaService --> CalculoVentaService
MantenimientoService --> VehiculoRepository
MantenimientoService --> MantenimientoRepository
MantenimientoService --> MantenimientoMapper
TasaCambioService --> VehiculoRepository
TasaCambioService --> TasaCambioGateway
TasaCambioGateway --> ApiExternaTasaCambio : HTTP JSON

ClienteRepository ..|> JpaRepository
VehiculoRepository ..|> JpaRepository
VentaRepository ..|> JpaRepository
MantenimientoRepository ..|> JpaRepository

ClienteMapper ..> ClienteRequestDTO
ClienteMapper ..> ClienteResponseDTO
ClienteMapper ..> Cliente
VehiculoMapper ..> VehiculoRequestDTO
VehiculoMapper ..> VehiculoResponseDTO
VehiculoMapper ..> Vehiculo
VentaMapper ..> VentaResponseDTO
VentaMapper ..> Venta
MantenimientoMapper ..> MantenimientoRequestDTO
MantenimientoMapper ..> MantenimientoResponseDTO
MantenimientoMapper ..> Mantenimiento

Cliente "1" --> "0..*" Venta : realiza
Vehiculo "1" --> "0..1" Venta : se vende en
Vehiculo "1" --> "0..*" Mantenimiento : tiene
Vehiculo --> EstadoVehiculo
Venta --> Cliente
Venta --> Vehiculo
Mantenimiento --> Vehiculo
CalculoVentaService --> ResultadoCalculoVenta

GlobalExceptionHandler ..> ClienteController
GlobalExceptionHandler ..> VehiculoController
GlobalExceptionHandler ..> VentaController
GlobalExceptionHandler ..> MantenimientoController
GlobalExceptionHandler ..> DivisaController
```

## Contrato REST representado

| Recurso | Operaciones principales |
| --- | --- |
| `ClienteController` | `GET/POST /clientes`, `GET/PUT/DELETE /clientes/{id}` |
| `VehiculoController` | `GET/POST /vehiculos`, `GET/PUT/DELETE /vehiculos/{id}`, `GET /vehiculos/disponibles`, `GET /vehiculos/marca/{marca}` |
| `VentaController` | `POST /ventas`, `GET /ventas`, `GET /ventas/{id}` |
| `MantenimientoController` | `POST /mantenimientos`, `GET /mantenimientos` y filtro opcional por `vehiculoId` |
| `DivisaController` | endpoints de consulta de tasa y conversion a USD, adicionales a los minimos del taller |

## Reglas reflejadas

- `VentaService.registrar` y `MantenimientoService.registrar` son transaccionales: el registro y el cambio de estado deben confirmarse o revertirse juntos.
- `CalculoVentaService` aplica 5 % solo cuando `precioCop > 100.000.000`; importes y tasas usan `BigDecimal`.
- `VentaService` solo acepta vehiculos `DISPONIBLE`; `ClienteService` y `VehiculoService` verifican unicidad de correo y placa respectivamente.
- `TasaCambioGateway` encapsula la dependencia HTTP externa. Un fallo se propaga como error controlado hacia `GlobalExceptionHandler`, sin detener los demas recursos.

## Trazabilidad

Este diseno cubre RF-01 a RF-25 y RNF-03, RNF-05 a RNF-19, RNF-22 a RNF-25. Las secuencias de los flujos transaccionales y de integracion externa estan en [Diagramas_de_secuencia.md](../08_Diagramas_de_secuencia/Diagramas_de_secuencia.md).
