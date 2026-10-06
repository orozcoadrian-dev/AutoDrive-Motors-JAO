# Diagramas de secuencia

**Proyecto:** AutoDrive Motors JAO
**Fecha:** 2026-10-06
**Estado:** diseno basado en RF, RNF, historias de usuario y narrativas de casos de uso. No representa ejecucion de codigo aun inexistente en el repositorio.

Los tres flujos cubren las operaciones con reglas de negocio, atomicidad o integracion externa que requieren mayor evidencia de comportamiento.

## 1. Registrar venta - CU-12

**Trazabilidad:** RF-12 a RF-18, HU-12 a HU-18 y RNF-18 a RNF-19.

```mermaid
sequenceDiagram
    actor Vendedor
    participant VC as VentaController
    participant VS as VentaService
    participant CR as ClienteRepository
    participant VR as VehiculoRepository
    participant CS as CalculoVentaService
    participant VP as VentaRepository
    participant EH as GlobalExceptionHandler

    Vendedor->>VC: POST /ventas (clienteId, vehiculoId)
    VC->>VS: registrar(dto)
    Note over VS,VP: Inicio de transaccion
    VS->>CR: findById(clienteId)
    alt cliente no existe
        CR-->>VS: vacio
        VS-->>EH: RecursoNoEncontradoException
        EH-->>Vendedor: 404 JSON de error
    else cliente existe
        CR-->>VS: Cliente
        VS->>VR: findById(vehiculoId)
        alt vehiculo no existe
            VR-->>VS: vacio
            VS-->>EH: RecursoNoEncontradoException
            EH-->>Vendedor: 404 JSON de error
        else vehiculo existe
            VR-->>VS: Vehiculo
            alt estado distinto de DISPONIBLE
                VS-->>EH: ReglaNegocioException
                EH-->>Vendedor: 409 JSON de error
            else vehiculo disponible
                VS->>CS: calcular(precioCop)
                alt precio mayor a 100000000 COP
                    CS-->>VS: total con descuento 5 %
                else precio menor o igual al umbral
                    CS-->>VS: total sin descuento
                end
                VS->>VS: asignar fecha y estado VENDIDO
                VS->>VP: save(venta)
                VP-->>VS: Venta persistida
                Note over VS,VP: Commit: venta y estado juntos
                VS-->>VC: VentaResponseDTO
                VC-->>Vendedor: 201 Created + JSON
            end
        end
    end
```

## 2. Registrar mantenimiento - CU-21

**Trazabilidad:** RF-21 a RF-23, HU-21 a HU-23 y RNF-18.

```mermaid
sequenceDiagram
    actor Vendedor
    participant MC as MantenimientoController
    participant MS as MantenimientoService
    participant VR as VehiculoRepository
    participant MP as MantenimientoRepository
    participant EH as GlobalExceptionHandler

    Vendedor->>MC: POST /mantenimientos (vehiculoId, fecha, descripcion, costo)
    MC->>MC: validar DTO
    alt DTO invalido
        MC-->>EH: MethodArgumentNotValidException
        EH-->>Vendedor: 400 JSON de error
    else DTO valido
        MC->>MS: registrar(dto)
        Note over MS,MP: Inicio de transaccion
        MS->>VR: findById(vehiculoId)
        alt vehiculo no existe
            VR-->>MS: vacio
            MS-->>EH: RecursoNoEncontradoException
            EH-->>Vendedor: 404 JSON de error
        else vehiculo existe
            VR-->>MS: Vehiculo
            MS->>MS: cambiar estado a EN_MANTENIMIENTO
            MS->>MP: save(mantenimiento)
            MP-->>MS: Mantenimiento persistido
            Note over MS,MP: Commit: mantenimiento y estado juntos
            MS-->>MC: MantenimientoResponseDTO
            MC-->>Vendedor: 201 Created + JSON
        end
    end
```

## 3. Convertir valor de vehiculo a USD - CU-25

**Trazabilidad:** RF-24 a RF-25, HU-24 a HU-25 y RNF-15 a RNF-16.

```mermaid
sequenceDiagram
    actor Vendedor
    participant DC as DivisaController
    participant TS as TasaCambioService
    participant VR as VehiculoRepository
    participant TG as TasaCambioGateway
    participant API as API externa de tasa
    participant EH as GlobalExceptionHandler

    Vendedor->>DC: GET /vehiculos/{id}/conversion-usd
    DC->>TS: convertirVehiculoAUsd(id)
    TS->>VR: findById(id)
    alt vehiculo no existe
        VR-->>TS: vacio
        TS-->>EH: RecursoNoEncontradoException
        EH-->>Vendedor: 404 JSON de error
    else vehiculo existe
        VR-->>TS: Vehiculo con precio COP
        TS->>TG: obtenerTasaCopUsd()
        TG->>API: GET HTTP
        alt API responde una tasa valida en JSON
            API-->>TG: 200 tasa COP/USD
            TG-->>TS: BigDecimal tasa
            TS->>TS: valorUsd = valorCop / tasa
            TS-->>DC: ConversionUsdDTO
            DC-->>Vendedor: 200 valor COP, tasa, valor USD
        else timeout, error HTTP o JSON invalido
            API-->>TG: fallo
            TG-->>EH: ServicioExternoException
            EH-->>Vendedor: 503 JSON de error controlado
        end
    end
```

## Convenciones usadas

- Los DTOs de entrada se validan antes de invocar la capa de servicio.
- Los errores pasan por un manejador centralizado y se devuelven como JSON, sin trazas ni detalles de SQL.
- Los codigos `400`, `404`, `409`, `201` y `503` son la propuesta REST para los resultados mostrados; deben consolidarse junto con la coleccion de Postman cuando exista la API.
- La conversion es solo de lectura: no modifica el precio almacenado del vehiculo.
