package com.autodrive.motors.api;

import com.autodrive.motors.dto.MantenimientoRequest;
import com.autodrive.motors.dto.MantenimientoResponse;
import com.autodrive.motors.service.MantenimientoService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/mantenimientos")
public class MantenimientoRestController {
    private final MantenimientoService mantenimientoService;

    public MantenimientoRestController(MantenimientoService mantenimientoService) {
        this.mantenimientoService = mantenimientoService;
    }

    @GetMapping
    public List<MantenimientoResponse> listar(@RequestParam(required = false) @Positive Long vehiculoId) {
        return mantenimientoService.listar(vehiculoId);
    }

    @GetMapping("/{id}")
    public MantenimientoResponse obtener(@PathVariable @Positive Long id) {
        return mantenimientoService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<MantenimientoResponse> registrar(@Valid @RequestBody MantenimientoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mantenimientoService.registrar(request));
    }
}
