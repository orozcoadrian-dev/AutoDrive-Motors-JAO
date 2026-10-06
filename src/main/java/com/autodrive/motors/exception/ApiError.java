package com.autodrive.motors.exception;

import java.time.Instant;

public record ApiError(Instant fecha, int estado, String error, String mensaje, String ruta) { }
