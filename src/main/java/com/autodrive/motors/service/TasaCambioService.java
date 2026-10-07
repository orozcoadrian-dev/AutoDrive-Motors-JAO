package com.autodrive.motors.service;

import com.autodrive.motors.dao.VehiculoDao;
import com.autodrive.motors.dto.ConversionUsdResponse;
import com.autodrive.motors.dto.TasaCambioResponse;
import com.autodrive.motors.exception.RecursoNoEncontradoException;
import com.autodrive.motors.external.TasaCambioClient;
import com.autodrive.motors.model.Vehiculo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class TasaCambioService {
    private final TasaCambioClient tasaCambioClient;
    private final VehiculoDao vehiculoDao;

    public TasaCambioService(TasaCambioClient tasaCambioClient, VehiculoDao vehiculoDao) {
        this.tasaCambioClient = tasaCambioClient;
        this.vehiculoDao = vehiculoDao;
    }

    public TasaCambioResponse obtenerTasaActual() {
        return new TasaCambioResponse("USD", "COP", tasaCambioClient.obtenerCopPorUsd());
    }

    public ConversionUsdResponse convertirVehiculo(Long vehiculoId) {
        BigDecimal tasa = tasaCambioClient.obtenerCopPorUsd();
        Vehiculo vehiculo = vehiculoDao.findById(vehiculoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Vehículo no encontrado."));
        BigDecimal precioUsd = vehiculo.getPrecioCop().divide(tasa, 2, RoundingMode.HALF_UP);
        return new ConversionUsdResponse(vehiculo.getId(), vehiculo.getPrecioCop(), tasa, precioUsd);
    }
}
