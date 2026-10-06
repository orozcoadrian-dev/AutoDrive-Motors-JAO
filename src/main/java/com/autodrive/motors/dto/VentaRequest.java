package com.autodrive.motors.dto;

import jakarta.validation.constraints.NotNull;

public record VentaRequest(@NotNull Long clienteId, @NotNull Long vehiculoId) { }
