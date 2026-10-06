package com.autodrive.motors.dao;

import com.autodrive.motors.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VentaDao extends JpaRepository<Venta, Long> {
    long countByClienteId(Long clienteId);
    long countByVehiculoId(Long vehiculoId);
}
