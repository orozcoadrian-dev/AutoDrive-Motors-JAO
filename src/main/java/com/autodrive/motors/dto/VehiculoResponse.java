package com.autodrive.motors.dto;

import com.autodrive.motors.model.EstadoVehiculo;

import java.math.BigDecimal;

public record VehiculoResponse(Long id, String placa, String marca, String modelo,
                               Integer anio, BigDecimal precioCop, EstadoVehiculo estado) { }
