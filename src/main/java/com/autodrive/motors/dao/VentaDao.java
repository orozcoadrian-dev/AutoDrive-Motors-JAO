package com.autodrive.motors.dao;

import com.autodrive.motors.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface VentaDao extends JpaRepository<Venta, Long> {
    long countByClienteId(Long clienteId);
    long countByVehiculoId(Long vehiculoId);

    @Query("select coalesce(sum(v.montoTotal), 0) from Venta v")
    BigDecimal calcularMontoTotalVentas();
}
