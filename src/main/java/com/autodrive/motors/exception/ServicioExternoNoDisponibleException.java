package com.autodrive.motors.exception;

public class ServicioExternoNoDisponibleException extends RuntimeException {
    public ServicioExternoNoDisponibleException(String message) { super(message); }
    public ServicioExternoNoDisponibleException(String message, Throwable cause) { super(message, cause); }
}
