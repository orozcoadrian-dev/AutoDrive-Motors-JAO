package com.autodrive.motors.dto;

import java.time.LocalDateTime;

public record ClienteResponse(Long id, String nombre, String apellido, String email,
                              String telefono, LocalDateTime fechaRegistro) { }
