package com.autodrive.motors.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VentaResponse(Long id, Long clienteId, String clienteNombre, Long vehiculoId,
                            String vehiculoDescripcion, LocalDateTime fechaVenta,
                            BigDecimal descuentoAplicado, BigDecimal montoTotal) { }
