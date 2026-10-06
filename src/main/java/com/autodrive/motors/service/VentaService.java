package com.autodrive.motors.service;

import com.autodrive.motors.dao.ClienteDao;
import com.autodrive.motors.dao.VehiculoDao;
import com.autodrive.motors.dao.VentaDao;
import com.autodrive.motors.dto.VentaRequest;
import com.autodrive.motors.dto.VentaResponse;
import com.autodrive.motors.exception.ReglaNegocioException;
import com.autodrive.motors.exception.RecursoNoEncontradoException;
import com.autodrive.motors.model.Cliente;
import com.autodrive.motors.model.EstadoVehiculo;
import com.autodrive.motors.model.Vehiculo;
import com.autodrive.motors.model.Venta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class VentaService {
    private static final BigDecimal UMBRAL_DESCUENTO = new BigDecimal("100000000.00");
    private static final BigDecimal PORCENTAJE_DESCUENTO = new BigDecimal("0.05");
    private final ClienteDao clienteDao;
    private final VehiculoDao vehiculoDao;
    private final VentaDao ventaDao;

    public VentaService(ClienteDao clienteDao, VehiculoDao vehiculoDao, VentaDao ventaDao) {
        this.clienteDao = clienteDao;
        this.vehiculoDao = vehiculoDao;
        this.ventaDao = ventaDao;
    }

    public VentaResponse registrar(VentaRequest request) {
        Cliente cliente = clienteDao.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado."));
        Vehiculo vehiculo = vehiculoDao.findById(request.vehiculoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Vehículo no encontrado."));
        if (vehiculo.getEstado() != EstadoVehiculo.DISPONIBLE) {
            throw new ReglaNegocioException("El vehículo no está disponible para la venta.");
        }

        BigDecimal descuento = calcularDescuento(vehiculo.getPrecioCop());
        Venta venta = new Venta();
        venta.setCliente(cliente);
        venta.setVehiculo(vehiculo);
        venta.setFechaVenta(LocalDateTime.now());
        venta.setDescuentoAplicado(descuento);
        venta.setMontoTotal(vehiculo.getPrecioCop().subtract(descuento));
        vehiculo.setEstado(EstadoVehiculo.VENDIDO);

        return aRespuesta(ventaDao.save(venta));
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listar() {
        return ventaDao.findAll().stream().map(this::aRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public VentaResponse obtenerPorId(Long id) {
        Venta venta = ventaDao.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta no encontrada."));
        return aRespuesta(venta);
    }

    private BigDecimal calcularDescuento(BigDecimal precioCop) {
        if (precioCop.compareTo(UMBRAL_DESCUENTO) <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return precioCop.multiply(PORCENTAJE_DESCUENTO).setScale(2, RoundingMode.HALF_UP);
    }

    private VentaResponse aRespuesta(Venta venta) {
        Cliente cliente = venta.getCliente();
        Vehiculo vehiculo = venta.getVehiculo();
        return new VentaResponse(venta.getId(), cliente.getId(), cliente.getNombre() + " " + cliente.getApellido(),
                vehiculo.getId(), vehiculo.getMarca() + " " + vehiculo.getModelo() + " (" + vehiculo.getPlaca() + ")",
                venta.getFechaVenta(), venta.getDescuentoAplicado(), venta.getMontoTotal());
    }
}
