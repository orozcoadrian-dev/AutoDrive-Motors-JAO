package com.autodrive.motors.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class VistaController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

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

    @GetMapping("/mantenimientos")
    public String mantenimientos() {
        return "mantenimientos";
    }
}
