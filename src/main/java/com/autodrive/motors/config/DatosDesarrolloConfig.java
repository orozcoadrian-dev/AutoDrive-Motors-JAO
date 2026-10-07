package com.autodrive.motors.config;

import com.autodrive.motors.dao.ClienteDao;
import com.autodrive.motors.dao.VehiculoDao;
import com.autodrive.motors.dto.MantenimientoRequest;
import com.autodrive.motors.dto.VentaRequest;
import com.autodrive.motors.model.Cliente;
import com.autodrive.motors.model.EstadoVehiculo;
import com.autodrive.motors.model.Vehiculo;
import com.autodrive.motors.service.MantenimientoService;
import com.autodrive.motors.service.VentaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DatosDesarrolloConfig {

    @Bean
    CommandLineRunner cargarDatosEjemplo(ClienteDao clienteDao, VehiculoDao vehiculoDao,
                                         VentaService ventaService, MantenimientoService mantenimientoService) {
        return args -> {
            if (clienteDao.count() > 0 || vehiculoDao.count() > 0) {
                return;
            }

            Cliente carlos = clienteDao.save(cliente("Carlos Andrés", "Ramírez Peña", "1012345678", "carlos.ramirez@example.test", "310 123 4567"));
            Cliente luisa = clienteDao.save(cliente("Luisa Fernanda", "Gómez Ortiz", "52876431", "luisa.gomez@example.test", "315 987 6543"));
            clienteDao.save(cliente("Jhon Edison", "Mosquera Rentería", "1098765432", "jhon.mosquera@example.test", "320 456 7890"));
            Cliente valentina = clienteDao.save(cliente("Valentina", "Ospina Cardona", "1037654321", "valentina.ospina@example.test", "301 222 3344"));
            clienteDao.save(cliente("Camilo", "Torres Beltrán", "80123456", "camilo.torres@example.test", "312 555 0198"));
            clienteDao.save(cliente("Daniela", "Rincón Vargas", "1020304050", "daniela.rincon@example.test", "318 765 4321"));

            vehiculoDao.save(vehiculo("FQT45D", "Yamaha", "FZ 2.0", 2024, "13890000"));
            Vehiculo honda = vehiculoDao.save(vehiculo("KDM28F", "Honda", "CB 190R", 2023, "11490000"));
            vehiculoDao.save(vehiculo("RUN91B", "AKT", "NKD 125", 2024, "6290000"));
            Vehiculo pulsar = vehiculoDao.save(vehiculo("TXA37G", "Bajaj", "Pulsar NS 200", 2023, "10990000"));
            Vehiculo gixxer = vehiculoDao.save(vehiculo("BNL62C", "Suzuki", "Gixxer 150", 2022, "9450000"));
            vehiculoDao.save(vehiculo("KLM123", "Mazda", "CX-30", 2024, "125000000"));
            Vehiculo duster = vehiculoDao.save(vehiculo("MNP456", "Renault", "Duster", 2023, "98000000"));
            Vehiculo tracker = vehiculoDao.save(vehiculo("HZR807", "Chevrolet", "Tracker", 2024, "102500000"));
            vehiculoDao.save(vehiculo("SJT254", "Kia", "Sportage", 2023, "134900000"));

            ventaService.registrar(new VentaRequest(carlos.getId(), tracker.getId()));
            ventaService.registrar(new VentaRequest(luisa.getId(), gixxer.getId()));
            ventaService.registrar(new VentaRequest(valentina.getId(), pulsar.getId()));

            LocalDate hoy = LocalDate.now();
            mantenimientoService.registrar(new MantenimientoRequest(duster.getId(), hoy.minusDays(19),
                    "Alineación y balanceo de las cuatro llantas.", new BigDecimal("220000")));
            mantenimientoService.registrar(new MantenimientoRequest(duster.getId(), hoy.minusDays(5),
                    "Cambio de pastillas delanteras y revisión del sistema de frenos.", new BigDecimal("380000")));
            mantenimientoService.registrar(new MantenimientoRequest(honda.getId(), hoy.minusDays(2),
                    "Cambio de kit de arrastre y ajuste de cadena.", new BigDecimal("265000")));
        };
    }

    private Cliente cliente(String nombre, String apellido, String documento, String email, String telefono) {
        Cliente cliente = new Cliente();
        cliente.setNombre(nombre);
        cliente.setApellido(apellido);
        cliente.setDocumento(documento);
        cliente.setEmail(email);
        cliente.setTelefono(telefono);
        return cliente;
    }

    private Vehiculo vehiculo(String placa, String marca, String modelo, int anio, String precio) {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setPlaca(placa);
        vehiculo.setMarca(marca);
        vehiculo.setModelo(modelo);
        vehiculo.setAnio(anio);
        vehiculo.setPrecioCop(new BigDecimal(precio));
        vehiculo.setEstado(EstadoVehiculo.DISPONIBLE);
        return vehiculo;
    }
}
