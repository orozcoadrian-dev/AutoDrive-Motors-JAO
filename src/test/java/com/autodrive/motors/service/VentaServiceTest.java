package com.autodrive.motors.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.autodrive.motors.dao.ClienteDao;
import com.autodrive.motors.dao.VehiculoDao;
import com.autodrive.motors.dao.VentaDao;
import com.autodrive.motors.dto.VentaRequest;
import com.autodrive.motors.exception.ReglaNegocioException;
import com.autodrive.motors.model.Cliente;
import com.autodrive.motors.model.EstadoVehiculo;
import com.autodrive.motors.model.Vehiculo;
import com.autodrive.motors.model.Venta;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock private ClienteDao clienteDao;
    @Mock private VehiculoDao vehiculoDao;
    @Mock private VentaDao ventaDao;

    private VentaService ventaService;
    private Cliente cliente;
    private Vehiculo vehiculo;

    @BeforeEach
    void prepararServicio() {
        ventaService = new VentaService(clienteDao, vehiculoDao, ventaDao);
        cliente = new Cliente();
        cliente.setNombre("Ana");
        cliente.setApellido("Orozco");
        vehiculo = new Vehiculo();
        vehiculo.setMarca("Mazda");
        vehiculo.setModelo("CX-30");
        vehiculo.setPlaca("KLM123");
        vehiculo.setPrecioCop(new BigDecimal("125000000.00"));
        vehiculo.setEstado(EstadoVehiculo.DISPONIBLE);
    }

    @Test
    void registraVentaConDescuentoYMarcaVehiculoComoVendido() {
        when(clienteDao.findById(1L)).thenReturn(Optional.of(cliente));
        when(vehiculoDao.findById(2L)).thenReturn(Optional.of(vehiculo));
        when(ventaDao.save(any(Venta.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        var respuesta = ventaService.registrar(new VentaRequest(1L, 2L));

        assertThat(respuesta.descuentoAplicado()).isEqualByComparingTo("6250000.00");
        assertThat(respuesta.montoTotal()).isEqualByComparingTo("118750000.00");
        assertThat(vehiculo.getEstado()).isEqualTo(EstadoVehiculo.VENDIDO);
    }

    @Test
    void rechazaVentaSiElVehiculoNoEstaDisponible() {
        vehiculo.setEstado(EstadoVehiculo.VENDIDO);
        when(clienteDao.findById(1L)).thenReturn(Optional.of(cliente));
        when(vehiculoDao.findById(2L)).thenReturn(Optional.of(vehiculo));

        assertThatThrownBy(() -> ventaService.registrar(new VentaRequest(1L, 2L)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El vehículo no está disponible para la venta.");
    }
}
