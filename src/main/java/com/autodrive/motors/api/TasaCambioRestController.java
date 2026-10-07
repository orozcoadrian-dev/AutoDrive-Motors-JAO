package com.autodrive.motors.api;

import com.autodrive.motors.dto.ConversionUsdResponse;
import com.autodrive.motors.dto.TasaCambioResponse;
import com.autodrive.motors.service.TasaCambioService;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
public class TasaCambioRestController {
    private final TasaCambioService tasaCambioService;

    public TasaCambioRestController(TasaCambioService tasaCambioService) {
        this.tasaCambioService = tasaCambioService;
    }

    @GetMapping("/tasa-cambio")
    public TasaCambioResponse tasaActual() {
        return tasaCambioService.obtenerTasaActual();
    }

    @GetMapping("/vehiculos/{id}/conversion-usd")
    public ConversionUsdResponse convertirVehiculo(@PathVariable @Positive Long id) {
        return tasaCambioService.convertirVehiculo(id);
    }
}
