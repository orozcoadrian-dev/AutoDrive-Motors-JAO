package com.autodrive.motors.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.autodrive.motors.dao.MantenimientoDao;
import com.autodrive.motors.dao.VehiculoDao;
import com.autodrive.motors.dao.VentaDao;
import com.autodrive.motors.dto.VehiculoRequest;
import com.autodrive.motors.exception.ReglaNegocioException;
import com.autodrive.motors.model.EstadoVehiculo;
import com.autodrive.motors.model.Vehiculo;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VehiculoServiceTest {

    @Mock private VehiculoDao vehiculoDao;
    @Mock private VentaDao ventaDao;
    @Mock private MantenimientoDao mantenimientoDao;

    private VehiculoService vehiculoService;
    private Vehiculo vehiculo;

    @BeforeEach
    void prepararServicio() {
        vehiculoService = new VehiculoService(vehiculoDao, ventaDao, mantenimientoDao);
        vehiculo = new Vehiculo();
        vehiculo.setPlaca("KLM123");
        vehiculo.setMarca("Mazda");
        vehiculo.setModelo("CX-30");
        vehiculo.setAnio(2024);
        vehiculo.setPrecioCop(new BigDecimal("90000000.00"));
        vehiculo.setEstado(EstadoVehiculo.DISPONIBLE);
    }

    @Test
    void registraVehiculoNuevoComoDisponible() {
        when(vehiculoDao.existsByPlacaIgnoreCase("ABC123")).thenReturn(false);
        when(vehiculoDao.save(any(Vehiculo.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        var respuesta = vehiculoService.registrar(new VehiculoRequest(
                "abc123", "Mazda", "CX-30", 2024, new BigDecimal("90000000.00")));

        assertThat(respuesta.placa()).isEqualTo("ABC123");
        assertThat(respuesta.estado()).isEqualTo(EstadoVehiculo.DISPONIBLE);
    }

    @Test
    void rechazaPlacaRepetida() {
        when(vehiculoDao.existsByPlacaIgnoreCase("KLM123")).thenReturn(true);

        assertThatThrownBy(() -> vehiculoService.registrar(new VehiculoRequest(
                "klm123", "Mazda", "CX-30", 2024, new BigDecimal("90000000.00"))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Ya existe un vehículo registrado con esa placa.");
        verify(vehiculoDao, never()).save(any(Vehiculo.class));
    }

    @Test
    void finalizaMantenimientoYDevuelveElVehiculoADisponible() {
        vehiculo.setEstado(EstadoVehiculo.EN_MANTENIMIENTO);
        when(vehiculoDao.findById(2L)).thenReturn(Optional.of(vehiculo));
        when(vehiculoDao.save(any(Vehiculo.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        var respuesta = vehiculoService.finalizarMantenimiento(2L);

        assertThat(respuesta.estado()).isEqualTo(EstadoVehiculo.DISPONIBLE);
        assertThat(vehiculo.getEstado()).isEqualTo(EstadoVehiculo.DISPONIBLE);
    }

    @Test
    void noFinalizaMantenimientoSiElVehiculoNoEstaEnMantenimiento() {
        when(vehiculoDao.findById(2L)).thenReturn(Optional.of(vehiculo));

        assertThatThrownBy(() -> vehiculoService.finalizarMantenimiento(2L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo un vehículo en mantenimiento puede volver a estar disponible.");
        assertThat(vehiculo.getEstado()).isEqualTo(EstadoVehiculo.DISPONIBLE);
    }

    @Test
    void noEliminaUnVehiculoConVentaRegistrada() {
        when(vehiculoDao.findById(2L)).thenReturn(Optional.of(vehiculo));
        when(ventaDao.countByVehiculoId(2L)).thenReturn(1L);

        assertThatThrownBy(() -> vehiculoService.eliminar(2L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede eliminar un vehículo con una venta registrada.");
        verify(vehiculoDao, never()).delete(any(Vehiculo.class));
    }
}
