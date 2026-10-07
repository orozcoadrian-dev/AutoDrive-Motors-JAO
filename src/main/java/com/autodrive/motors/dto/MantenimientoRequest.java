package com.autodrive.motors.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MantenimientoRequest(
        @NotNull @Positive Long vehiculoId,
        @NotNull LocalDate fechaMantenimiento,
        @NotBlank @Size(max = 500) String descripcion,
        @NotNull @DecimalMin(value = "0.00") BigDecimal costo
) { }
