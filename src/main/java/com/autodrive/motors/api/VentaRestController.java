package com.autodrive.motors.api;

import com.autodrive.motors.dto.VentaRequest;
import com.autodrive.motors.dto.VentaResponse;
import com.autodrive.motors.service.VentaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/ventas")
public class VentaRestController {
    private final VentaService ventaService;

    public VentaRestController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @GetMapping
    public List<VentaResponse> listar() { return ventaService.listar(); }

    @GetMapping("/{id}")
    public VentaResponse obtener(@PathVariable @Positive Long id) { return ventaService.obtenerPorId(id); }

    @PostMapping
    public ResponseEntity<VentaResponse> registrar(@Valid @RequestBody VentaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.registrar(request));
    }
}
