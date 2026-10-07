package com.autodrive.motors.api;

import com.autodrive.motors.dto.VehiculoRequest;
import com.autodrive.motors.dto.VehiculoResponse;
import com.autodrive.motors.service.VehiculoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoRestController {
    private final VehiculoService vehiculoService;

    public VehiculoRestController(VehiculoService vehiculoService) {
        this.vehiculoService = vehiculoService;
    }

    @GetMapping
    public List<VehiculoResponse> listar() { return vehiculoService.listar(); }

    @GetMapping("/{id}")
    public VehiculoResponse obtener(@PathVariable @Positive Long id) { return vehiculoService.obtenerPorId(id); }

    @GetMapping("/disponibles")
    public List<VehiculoResponse> disponibles() { return vehiculoService.listarDisponibles(); }

    @GetMapping("/marca/{marca}")
    public List<VehiculoResponse> porMarca(@PathVariable String marca) { return vehiculoService.listarPorMarca(marca); }

    @PostMapping
    public ResponseEntity<VehiculoResponse> registrar(@Valid @RequestBody VehiculoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehiculoService.registrar(request));
    }

    @PutMapping("/{id}")
    public VehiculoResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody VehiculoRequest request) {
        return vehiculoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        vehiculoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
