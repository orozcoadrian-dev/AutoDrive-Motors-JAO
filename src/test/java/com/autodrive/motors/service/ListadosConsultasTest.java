package com.autodrive.motors.service;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los listados arman la respuesta con datos del cliente y del vehículo. Si esas relaciones se cargan
 * una por una, cada fila agrega consultas; estas pruebas fijan que cada listado se resuelva en una sola.
 * El perfil dev ya siembra tres ventas y tres mantenimientos, así que hay varias filas que recorrer.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:listados;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@ActiveProfiles("dev")
class ListadosConsultasTest {
    @Autowired
    private VentaService ventaService;
    @Autowired
    private MantenimientoService mantenimientoService;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics estadisticas;

    @BeforeEach
    void reiniciarEstadisticas() {
        estadisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        estadisticas.clear();
    }

    @Test
    void listarVentasConsultaUnaSolaVez() {
        assertThat(ventaService.listar()).hasSizeGreaterThan(1);

        assertThat(estadisticas.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void listarMantenimientosConsultaUnaSolaVez() {
        assertThat(mantenimientoService.listar(null)).hasSizeGreaterThan(1);

        assertThat(estadisticas.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void listarMantenimientosDeUnVehiculoConsultaUnaSolaVez() {
        Long vehiculoId = mantenimientoService.listar(null).get(0).vehiculoId();
        estadisticas.clear();

        assertThat(mantenimientoService.listar(vehiculoId)).isNotEmpty();

        assertThat(estadisticas.getPrepareStatementCount()).isEqualTo(1);
    }
}
