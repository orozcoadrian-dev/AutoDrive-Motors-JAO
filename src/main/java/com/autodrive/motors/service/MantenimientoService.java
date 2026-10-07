package com.autodrive.motors.service;

import com.autodrive.motors.dao.MantenimientoDao;
import com.autodrive.motors.dao.VehiculoDao;
import com.autodrive.motors.dto.MantenimientoRequest;
import com.autodrive.motors.dto.MantenimientoResponse;
import com.autodrive.motors.exception.ReglaNegocioException;
import com.autodrive.motors.exception.RecursoNoEncontradoException;
import com.autodrive.motors.model.EstadoVehiculo;
import com.autodrive.motors.model.Mantenimiento;
import com.autodrive.motors.model.Vehiculo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MantenimientoService {
    private final MantenimientoDao mantenimientoDao;
    private final VehiculoDao vehiculoDao;

    public MantenimientoService(MantenimientoDao mantenimientoDao, VehiculoDao vehiculoDao) {
        this.mantenimientoDao = mantenimientoDao;
        this.vehiculoDao = vehiculoDao;
    }

    public MantenimientoResponse registrar(MantenimientoRequest request) {
        Vehiculo vehiculo = vehiculoDao.findById(request.vehiculoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Vehículo no encontrado."));
        if (vehiculo.getEstado() == EstadoVehiculo.VENDIDO) {
            throw new ReglaNegocioException("No se puede registrar mantenimiento para un vehículo vendido.");
        }

        Mantenimiento mantenimiento = new Mantenimiento();
        mantenimiento.setVehiculo(vehiculo);
        mantenimiento.setFechaMantenimiento(request.fechaMantenimiento());
        mantenimiento.setDescripcion(request.descripcion().trim());
        mantenimiento.setCosto(request.costo());
        vehiculo.setEstado(EstadoVehiculo.EN_MANTENIMIENTO);
        return aRespuesta(mantenimientoDao.save(mantenimiento));
    }

    @Transactional(readOnly = true)
    public List<MantenimientoResponse> listar(Long vehiculoId) {
        List<Mantenimiento> mantenimientos = vehiculoId == null
                ? mantenimientoDao.findAll()
                : mantenimientoDao.findByVehiculoIdOrderByFechaMantenimientoDesc(vehiculoId);
        return mantenimientos.stream().map(this::aRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public MantenimientoResponse obtenerPorId(Long id) {
        return aRespuesta(mantenimientoDao.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mantenimiento no encontrado.")));
    }

    private MantenimientoResponse aRespuesta(Mantenimiento mantenimiento) {
        Vehiculo vehiculo = mantenimiento.getVehiculo();
        String descripcion = vehiculo.getMarca() + " " + vehiculo.getModelo() + " (" + vehiculo.getPlaca() + ")";
        return new MantenimientoResponse(mantenimiento.getId(), vehiculo.getId(), descripcion,
                mantenimiento.getFechaMantenimiento(), mantenimiento.getDescripcion(), mantenimiento.getCosto());
    }
}
