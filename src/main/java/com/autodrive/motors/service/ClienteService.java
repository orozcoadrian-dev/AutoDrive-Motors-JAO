package com.autodrive.motors.service;

import com.autodrive.motors.dao.ClienteDao;
import com.autodrive.motors.dao.VentaDao;
import com.autodrive.motors.dto.ClienteRequest;
import com.autodrive.motors.dto.ClienteResponse;
import com.autodrive.motors.exception.ReglaNegocioException;
import com.autodrive.motors.exception.RecursoNoEncontradoException;
import com.autodrive.motors.model.Cliente;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ClienteService {
    private final ClienteDao clienteDao;
    private final VentaDao ventaDao;

    public ClienteService(ClienteDao clienteDao, VentaDao ventaDao) {
        this.clienteDao = clienteDao;
        this.ventaDao = ventaDao;
    }

    public ClienteResponse registrar(ClienteRequest request) {
        validarCorreoUnico(request.email(), null);
        Cliente cliente = new Cliente();
        aplicar(request, cliente);
        return aRespuesta(clienteDao.save(cliente));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return clienteDao.findAll().stream().map(this::aRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorId(Long id) {
        return aRespuesta(buscar(id));
    }

    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscar(id);
        validarCorreoUnico(request.email(), id);
        aplicar(request, cliente);
        return aRespuesta(clienteDao.save(cliente));
    }

    public void eliminar(Long id) {
        Cliente cliente = buscar(id);
        if (ventaDao.countByClienteId(id) > 0) {
            throw new ReglaNegocioException("No se puede eliminar un cliente con ventas registradas.");
        }
        clienteDao.delete(cliente);
    }

    private Cliente buscar(Long id) {
        return clienteDao.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado."));
    }

    private void validarCorreoUnico(String email, Long idActual) {
        String correo = email.trim().toLowerCase();
        boolean repetido = idActual == null
                ? clienteDao.existsByEmailIgnoreCase(correo)
                : clienteDao.existsByEmailIgnoreCaseAndIdNot(correo, idActual);
        if (repetido) {
            throw new ReglaNegocioException("Ya existe un cliente registrado con ese correo.");
        }
    }

    private void aplicar(ClienteRequest request, Cliente cliente) {
        cliente.setNombre(request.nombre().trim());
        cliente.setApellido(request.apellido().trim());
        cliente.setEmail(request.email().trim().toLowerCase());
        cliente.setTelefono(request.telefono().trim());
    }

    private ClienteResponse aRespuesta(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getNombre(), cliente.getApellido(), cliente.getEmail(),
                cliente.getTelefono(), cliente.getFechaRegistro());
    }
}
