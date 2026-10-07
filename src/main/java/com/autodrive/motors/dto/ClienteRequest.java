package com.autodrive.motors.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
        @NotBlank @Size(min = 2, max = 80) String nombre,
        @NotBlank @Size(min = 2, max = 80) String apellido,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9.-]{5,30}") String documento,
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Pattern(regexp = "[0-9+() -]{7,30}") String telefono
) { }
