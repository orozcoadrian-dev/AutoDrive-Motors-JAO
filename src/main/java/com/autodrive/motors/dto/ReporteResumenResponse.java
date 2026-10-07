package com.autodrive.motors.dto;

import java.math.BigDecimal;

public record ReporteResumenResponse(long totalVentas, BigDecimal montoTotalVentas,
                                     long vehiculosDisponibles, long vehiculosVendidos,
                                     long vehiculosEnMantenimiento) { }
