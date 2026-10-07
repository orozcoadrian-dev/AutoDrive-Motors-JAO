package com.autodrive.motors.service;

import com.autodrive.motors.dao.VehiculoDao;
import com.autodrive.motors.dao.VentaDao;
import com.autodrive.motors.dto.ReporteResumenResponse;
import com.autodrive.motors.model.EstadoVehiculo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional(readOnly = true)
public class ReporteService {
    private final VentaDao ventaDao;
    private final VehiculoDao vehiculoDao;

    public ReporteService(VentaDao ventaDao, VehiculoDao vehiculoDao) {
        this.ventaDao = ventaDao;
        this.vehiculoDao = vehiculoDao;
    }

    public ReporteResumenResponse obtenerResumen() {
        BigDecimal montoTotal = ventaDao.calcularMontoTotalVentas();
        return new ReporteResumenResponse(
                ventaDao.count(),
                montoTotal == null ? BigDecimal.ZERO.setScale(2) : montoTotal,
                vehiculoDao.countByEstado(EstadoVehiculo.DISPONIBLE),
                vehiculoDao.countByEstado(EstadoVehiculo.VENDIDO),
                vehiculoDao.countByEstado(EstadoVehiculo.EN_MANTENIMIENTO)
        );
    }
}
