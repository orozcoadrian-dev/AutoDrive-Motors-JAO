package com.autodrive.motors.dao;

import com.autodrive.motors.model.Mantenimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MantenimientoDao extends JpaRepository<Mantenimiento, Long> {
    List<Mantenimiento> findByVehiculoIdOrderByFechaMantenimientoDesc(Long vehiculoId);
    long countByVehiculoId(Long vehiculoId);
}
