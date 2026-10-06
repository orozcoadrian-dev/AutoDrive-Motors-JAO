package com.autodrive.motors.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record VehiculoRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{5,12}") String placa,
        @NotBlank @Size(max = 60) String marca,
        @NotBlank @Size(max = 80) String modelo,
        @NotNull @Max(2100) Integer anio,
        @NotNull @DecimalMin(value = "0.00") BigDecimal precioCop
) { }
