package com.autodrive.motors.config;

import com.autodrive.motors.dao.ClienteDao;
import com.autodrive.motors.dao.VehiculoDao;
import com.autodrive.motors.model.Cliente;
import com.autodrive.motors.model.EstadoVehiculo;
import com.autodrive.motors.model.Vehiculo;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Datos mínimos y descartables para demostrar el flujo con el perfil dev. */
@Configuration
@Profile("dev")
public class DatosDesarrolloConfig {

    @Bean
    CommandLineRunner cargarDatosEjemplo(ClienteDao clienteDao, VehiculoDao vehiculoDao) {
        return args -> {
            if (clienteDao.count() == 0) {
                Cliente cliente = new Cliente();
                cliente.setNombre("Ana");
                cliente.setApellido("Orozco");
                cliente.setEmail("ana.orozco@example.test");
                cliente.setTelefono("3001234567");
                clienteDao.save(cliente);
            }

            if (vehiculoDao.count() == 0) {
                vehiculoDao.save(vehiculo("KLM123", "Mazda", "CX-30", 2024, "125000000"));
                vehiculoDao.save(vehiculo("MNP456", "Renault", "Duster", 2023, "98000000"));
            }
        };
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
