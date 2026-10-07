package com.autodrive.motors.dto;

import java.math.BigDecimal;

public record ConversionUsdResponse(Long vehiculoId, BigDecimal precioCop, BigDecimal tasaCopPorUsd,
                                   BigDecimal precioUsd) { }
