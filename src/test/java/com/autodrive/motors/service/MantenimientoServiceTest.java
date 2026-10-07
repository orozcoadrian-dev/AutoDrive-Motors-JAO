package com.autodrive.motors.service;

import com.autodrive.motors.dao.MantenimientoDao;
import com.autodrive.motors.dao.VehiculoDao;
import com.autodrive.motors.dto.MantenimientoRequest;
import com.autodrive.motors.exception.ReglaNegocioException;
import com.autodrive.motors.model.EstadoVehiculo;
import com.autodrive.motors.model.Mantenimiento;
import com.autodrive.motors.model.Vehiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MantenimientoServiceTest {
    @Mock private MantenimientoDao mantenimientoDao;
    @Mock private VehiculoDao vehiculoDao;

    private MantenimientoService mantenimientoService;
    private Vehiculo vehiculo;

    @BeforeEach
    void prepararServicio() {
        mantenimientoService = new MantenimientoService(mantenimientoDao, vehiculoDao);
        vehiculo = new Vehiculo();
        vehiculo.setPlaca("KLM123");
        vehiculo.setMarca("Mazda");
        vehiculo.setModelo("CX-30");
        vehiculo.setEstado(EstadoVehiculo.DISPONIBLE);
    }

    @Test
    void registraMantenimientoYCambiaEstadoDelVehiculo() {
        when(vehiculoDao.findById(2L)).thenReturn(Optional.of(vehiculo));
        when(mantenimientoDao.save(any(Mantenimiento.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        var respuesta = mantenimientoService.registrar(new MantenimientoRequest(
                2L, LocalDate.of(2026, 10, 7), "Cambio de aceite", new BigDecimal("250000.00")
        ));

        assertThat(vehiculo.getEstado()).isEqualTo(EstadoVehiculo.EN_MANTENIMIENTO);
        assertThat(respuesta.descripcion()).isEqualTo("Cambio de aceite");
        assertThat(respuesta.costo()).isEqualByComparingTo("250000.00");
    }

    @Test
    void rechazaMantenimientoParaVehiculoVendido() {
        vehiculo.setEstado(EstadoVehiculo.VENDIDO);
        when(vehiculoDao.findById(2L)).thenReturn(Optional.of(vehiculo));

        assertThatThrownBy(() -> mantenimientoService.registrar(new MantenimientoRequest(
                2L, LocalDate.now(), "Revisión", BigDecimal.ZERO
        )))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede registrar mantenimiento para un vehículo vendido.");
    }
}
