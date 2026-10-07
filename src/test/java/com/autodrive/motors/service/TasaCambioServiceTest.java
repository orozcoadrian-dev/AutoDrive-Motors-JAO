package com.autodrive.motors.service;

import com.autodrive.motors.dao.VehiculoDao;
import com.autodrive.motors.external.TasaCambioClient;
import com.autodrive.motors.model.Vehiculo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TasaCambioServiceTest {
    @Mock private TasaCambioClient tasaCambioClient;
    @Mock private VehiculoDao vehiculoDao;

    @Test
    void conviertePrecioCopAUsdConLaTasaRecibida() {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setPrecioCop(new BigDecimal("50000000.00"));
        when(tasaCambioClient.obtenerCopPorUsd()).thenReturn(new BigDecimal("4000.00"));
        when(vehiculoDao.findById(7L)).thenReturn(Optional.of(vehiculo));

        var respuesta = new TasaCambioService(tasaCambioClient, vehiculoDao).convertirVehiculo(7L);

        assertThat(respuesta.precioUsd()).isEqualByComparingTo("12500.00");
        assertThat(respuesta.tasaCopPorUsd()).isEqualByComparingTo("4000.00");
    }
}
