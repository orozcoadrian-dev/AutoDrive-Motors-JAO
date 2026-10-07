package com.autodrive.motors.api;

import com.autodrive.motors.dto.ReporteResumenResponse;
import com.autodrive.motors.service.ReporteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
public class ReporteRestController {
    private final ReporteService reporteService;

    public ReporteRestController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/resumen")
    public ReporteResumenResponse resumen() {
        return reporteService.obtenerResumen();
    }
}
