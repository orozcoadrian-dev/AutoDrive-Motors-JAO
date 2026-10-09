package com.autodrive.motors.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.autodrive.motors.dao.ClienteDao;
import com.autodrive.motors.dao.VentaDao;
import com.autodrive.motors.dto.ClienteRequest;
import com.autodrive.motors.exception.ReglaNegocioException;
import com.autodrive.motors.model.Cliente;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock private ClienteDao clienteDao;
    @Mock private VentaDao ventaDao;

    private ClienteService clienteService;

    @BeforeEach
    void prepararServicio() {
        clienteService = new ClienteService(clienteDao, ventaDao);
    }

    private ClienteRequest solicitud(String email, String documento) {
        return new ClienteRequest("Ana", "Pérez", documento, email, "3001234567");
    }

    @Test
    void registraClienteYNormalizaCorreoYDocumento() {
        when(clienteDao.existsByEmailIgnoreCase("ana@correo.com")).thenReturn(false);
        when(clienteDao.existsByDocumentoIgnoreCase("CC12345")).thenReturn(false);
        when(clienteDao.save(any(Cliente.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        var respuesta = clienteService.registrar(solicitud("  ANA@Correo.com ", "cc12345"));

        assertThat(respuesta.email()).isEqualTo("ana@correo.com");
        assertThat(respuesta.documento()).isEqualTo("CC12345");
    }

    @Test
    void rechazaCorreoRepetido() {
        when(clienteDao.existsByEmailIgnoreCase("ana@correo.com")).thenReturn(true);

        assertThatThrownBy(() -> clienteService.registrar(solicitud("ana@correo.com", "CC12345")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Ya existe un cliente registrado con ese correo.");
        verify(clienteDao, never()).save(any(Cliente.class));
    }

    @Test
    void rechazaDocumentoRepetido() {
        when(clienteDao.existsByEmailIgnoreCase("ana@correo.com")).thenReturn(false);
        when(clienteDao.existsByDocumentoIgnoreCase("CC12345")).thenReturn(true);

        assertThatThrownBy(() -> clienteService.registrar(solicitud("ana@correo.com", "CC12345")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Ya existe un cliente registrado con ese documento.");
    }

    @Test
    void noEliminaUnClienteConVentasRegistradas() {
        when(clienteDao.findById(1L)).thenReturn(Optional.of(new Cliente()));
        when(ventaDao.countByClienteId(1L)).thenReturn(2L);

        assertThatThrownBy(() -> clienteService.eliminar(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede eliminar un cliente con ventas registradas.");
        verify(clienteDao, never()).delete(any(Cliente.class));
    }
}
