package com.autodrive.motors.service;

import com.autodrive.motors.dao.VehiculoDao;
import com.autodrive.motors.dao.VentaDao;
import com.autodrive.motors.dao.MantenimientoDao;
import com.autodrive.motors.dto.VehiculoRequest;
import com.autodrive.motors.dto.VehiculoResponse;
import com.autodrive.motors.exception.ReglaNegocioException;
import com.autodrive.motors.exception.RecursoNoEncontradoException;
import com.autodrive.motors.model.EstadoVehiculo;
import com.autodrive.motors.model.Vehiculo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class VehiculoService {
    private final VehiculoDao vehiculoDao;
    private final VentaDao ventaDao;
    private final MantenimientoDao mantenimientoDao;

    public VehiculoService(VehiculoDao vehiculoDao, VentaDao ventaDao, MantenimientoDao mantenimientoDao) {
        this.vehiculoDao = vehiculoDao;
        this.ventaDao = ventaDao;
        this.mantenimientoDao = mantenimientoDao;
    }

    public VehiculoResponse registrar(VehiculoRequest request) {
        validarPlacaUnica(request.placa(), null);
        Vehiculo vehiculo = new Vehiculo();
        aplicar(request, vehiculo);
        vehiculo.setEstado(EstadoVehiculo.DISPONIBLE);
        return aRespuesta(vehiculoDao.save(vehiculo));
    }

    @Transactional(readOnly = true)
    public List<VehiculoResponse> listar() {
        return vehiculoDao.findAll().stream().map(this::aRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public VehiculoResponse obtenerPorId(Long id) {
        return aRespuesta(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<VehiculoResponse> listarPorMarca(String marca) {
        return vehiculoDao.findByMarcaContainingIgnoreCase(marca.trim()).stream().map(this::aRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public List<VehiculoResponse> listarDisponibles() {
        return vehiculoDao.findByEstado(EstadoVehiculo.DISPONIBLE).stream().map(this::aRespuesta).toList();
    }

    public VehiculoResponse actualizar(Long id, VehiculoRequest request) {
        Vehiculo vehiculo = buscar(id);
        validarPlacaUnica(request.placa(), id);
        aplicar(request, vehiculo);
        return aRespuesta(vehiculoDao.save(vehiculo));
    }

    public VehiculoResponse finalizarMantenimiento(Long id) {
        Vehiculo vehiculo = buscar(id);
        if (vehiculo.getEstado() != EstadoVehiculo.EN_MANTENIMIENTO) {
            throw new ReglaNegocioException("Solo un vehículo en mantenimiento puede volver a estar disponible.");
        }
        vehiculo.setEstado(EstadoVehiculo.DISPONIBLE);
        return aRespuesta(vehiculoDao.save(vehiculo));
    }

    public void eliminar(Long id) {
        Vehiculo vehiculo = buscar(id);
        if (ventaDao.countByVehiculoId(id) > 0) {
            throw new ReglaNegocioException("No se puede eliminar un vehículo con una venta registrada.");
        }
        if (mantenimientoDao.countByVehiculoId(id) > 0) {
            throw new ReglaNegocioException("No se puede eliminar un vehículo con mantenimientos registrados.");
        }
        vehiculoDao.delete(vehiculo);
    }

    private Vehiculo buscar(Long id) {
        return vehiculoDao.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Vehículo no encontrado."));
    }

    private void validarPlacaUnica(String placa, Long idActual) {
        String normalizada = placa.trim().toUpperCase();
        boolean repetida = idActual == null
                ? vehiculoDao.existsByPlacaIgnoreCase(normalizada)
                : vehiculoDao.existsByPlacaIgnoreCaseAndIdNot(normalizada, idActual);
        if (repetida) {
            throw new ReglaNegocioException("Ya existe un vehículo registrado con esa placa.");
        }
    }

    private void aplicar(VehiculoRequest request, Vehiculo vehiculo) {
        vehiculo.setPlaca(request.placa().trim().toUpperCase());
        vehiculo.setMarca(request.marca().trim());
        vehiculo.setModelo(request.modelo().trim());
        vehiculo.setAnio(request.anio());
        vehiculo.setPrecioCop(request.precioCop());
    }

    private VehiculoResponse aRespuesta(Vehiculo vehiculo) {
        return new VehiculoResponse(vehiculo.getId(), vehiculo.getPlaca(), vehiculo.getMarca(), vehiculo.getModelo(),
                vehiculo.getAnio(), vehiculo.getPrecioCop(), vehiculo.getEstado());
    }
}
