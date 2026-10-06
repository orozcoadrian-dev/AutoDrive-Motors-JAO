package com.autodrive.motors.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Entrega las vistas HTML. Los datos se solicitan desde JavaScript a la API
 * REST; así se mantiene una separación clara entre MVC web y API.
 */
@Controller
public class VistaController {

    @GetMapping("/")
    public String inicio() {
        return "inicio";
    }

    @GetMapping("/clientes")
    public String clientes() {
        return "clientes";
    }

    @GetMapping("/vehiculos")
    public String vehiculos() {
        return "vehiculos";
    }

    @GetMapping("/ventas")
    public String ventas() {
        return "ventas";
    }
}
