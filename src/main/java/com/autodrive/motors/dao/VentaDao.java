package com.autodrive.motors.dao;

import com.autodrive.motors.model.Venta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface VentaDao extends JpaRepository<Venta, Long> {
    @Override
    @EntityGraph(attributePaths = {"cliente", "vehiculo"})
    List<Venta> findAll();

    long countByClienteId(Long clienteId);
    long countByVehiculoId(Long vehiculoId);

    @Query("select coalesce(sum(v.montoTotal), 0) from Venta v")
    BigDecimal calcularMontoTotalVentas();
}
