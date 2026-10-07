package com.autodrive.motors.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MantenimientoResponse(Long id, Long vehiculoId, String vehiculoDescripcion,
                                    LocalDate fechaMantenimiento, String descripcion, BigDecimal costo) { }
