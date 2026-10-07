package com.autodrive.motors.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice(basePackages = "com.autodrive.motors.api")
public class ManejadorGlobalErrores {
    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<ApiError> manejarNoEncontrado(RecursoNoEncontradoException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    ResponseEntity<ApiError> manejarReglaNegocio(ReglaNegocioException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(ServicioExternoNoDisponibleException.class)
    ResponseEntity<ApiError> manejarServicioExterno(ServicioExternoNoDisponibleException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiError> manejarConflictoConcurrente(HttpServletRequest request) {
        return respuesta(HttpStatus.CONFLICT,
                "El vehículo fue actualizado por otra operación. Actualice la información e intente de nuevo.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> manejarValidacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getField)
                .distinct()
                .collect(Collectors.joining(", ", "Campos inválidos: ", "."));
        return respuesta(HttpStatus.BAD_REQUEST, mensaje, request);
    }

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> manejarParametroInvalido(HttpServletRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST, "Parámetro de solicitud inválido.", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> manejarJsonInvalido(HttpServletRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST, "El JSON enviado no es válido.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> manejarIntegridad(HttpServletRequest request) {
        return respuesta(HttpStatus.CONFLICT, "La operación entra en conflicto con un registro existente.", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> manejarInesperado(Exception ignored, HttpServletRequest request) {
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado.", request);
    }

    private ResponseEntity<ApiError> respuesta(HttpStatus estado, String mensaje, HttpServletRequest request) {
        return ResponseEntity.status(estado).body(
                new ApiError(Instant.now(), estado.value(), estado.getReasonPhrase(), mensaje, request.getRequestURI())
        );
    }
}
