package com.autodrive.motors.dto;

import java.math.BigDecimal;

public record TasaCambioResponse(String monedaBase, String monedaDestino, BigDecimal tasa) { }
