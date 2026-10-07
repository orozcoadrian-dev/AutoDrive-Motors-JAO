package com.autodrive.motors.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record VentaRequest(@NotNull @Positive Long clienteId, @NotNull @Positive Long vehiculoId) { }
