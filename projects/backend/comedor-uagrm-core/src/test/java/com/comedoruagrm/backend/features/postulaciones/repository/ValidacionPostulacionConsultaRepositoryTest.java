package com.comedoruagrm.backend.features.postulaciones.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Consultas de la validacion de elegibilidad y duplicados (CU18) con Postgres real. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ValidacionPostulacionConsultaRepository.class)
class ValidacionPostulacionConsultaRepositoryTest {

    @Autowired
    private ValidacionPostulacionConsultaRepository consulta;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void saltarSiNoHayPostgres() {
        PostgresTestDatabase.asegurarDisponible();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.registrar(registry);
    }

    private UUID codigoUsuario(long usuario) {
        return jdbc.queryForObject("SELECT codigo_usuario FROM usuario WHERE id = ?", UUID.class, usuario);
    }

    private UUID codigoConvocatoria(long convocatoria) {
        return jdbc.queryForObject("SELECT codigo_convocatoria FROM convocatoria WHERE id = ?", UUID.class,
                convocatoria);
    }

    private long gestionDe(long convocatoria) {
        return jdbc.queryForObject("SELECT gestion_id FROM convocatoria WHERE id = ?", Long.class, convocatoria);
    }

    /** Otra convocatoria de la MISMA gestion que `convocatoria`. */
    private long otraConvocatoriaDeLaGestion(long convocatoria) {
        return jdbc.queryForObject("""
                INSERT INTO convocatoria (gestion_id, nombre, fecha_apertura, fecha_cierre, estado)
                VALUES (?, 'Otra', now() - interval '2 days', now() - interval '1 day', 'CERRADA') RETURNING id
                """, Long.class, gestionDe(convocatoria));
    }

    @Test
    void postulanteDe_devuelveElPostulanteYSuEstadoActivo() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long usuario = fx.postulante("9400001");

        var datos = consulta.postulanteDe(codigoUsuario(usuario)).orElseThrow();

        assertThat(datos.id()).isEqualTo(usuario);
        assertThat(datos.activo()).isTrue();
    }

    @Test
    void postulanteDe_conCuentaQueNoEsDePostulante_devuelveVacio() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long admin = fx.usuario("9400002", "ADMINISTRADOR_DUBSS");

        assertThat(consulta.postulanteDe(codigoUsuario(admin))).isEmpty();
        assertThat(consulta.postulanteDe(UUID.randomUUID())).isEmpty();
    }

    @Test
    void convocatoria_devuelveLaGestionYElEstado() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("VAL1");

        var datos = consulta.convocatoria(codigoConvocatoria(conv)).orElseThrow();

        assertThat(datos.id()).isEqualTo(conv);
        assertThat(datos.gestionId()).isEqualTo(gestionDe(conv));
        // * `ExpedienteFixtures.convocatoria` antepone «G-» a la etiqueta de la gestión.
        assertThat(datos.gestionEtiqueta()).isEqualTo("G-VAL1");
        assertThat(datos.estado()).isNotBlank();
    }

    @Test
    void convocatoria_inexistenteOEliminada_devuelveVacio() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("VAL2");
        UUID codigo = codigoConvocatoria(conv);
        jdbc.update("UPDATE convocatoria SET eliminado_en = now() WHERE id = ?", conv);

        assertThat(consulta.convocatoria(codigo)).isEmpty();
        assertThat(consulta.convocatoria(UUID.randomUUID())).isEmpty();
    }

    @Test
    void postulacionesEnLaGestion_traeLasDeTodasLasConvocatoriasDeLaGestionYNoLasDeOtra() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("VAL3");
        long hermana = otraConvocatoriaDeLaGestion(conv);
        long ajena = fx.convocatoria("VAL3-OTRA");
        long usuario = fx.postulante("9400003");
        long enGestion = fx.postulacion(hermana, usuario, "archivada");
        fx.postulacion(ajena, usuario, "pendiente");

        var lista = consulta.postulacionesEnLaGestion(usuario, gestionDe(conv));

        assertThat(lista).extracting(p -> p.id()).containsExactly(enGestion);
        assertThat(lista.get(0).convocatoriaId()).isEqualTo(hermana);
        assertThat(lista.get(0).estado()).isEqualTo("archivada");
    }

    @Test
    void postulacionesEnLaGestion_ignoraLasEliminadasYLasDeOtroPostulante() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("VAL4");
        long usuario = fx.postulante("9400004");
        long otro = fx.postulante("9400005");
        long propia = fx.postulacion(conv, usuario, "pendiente");
        fx.postulacion(conv, otro, "pendiente");
        jdbc.update("UPDATE postulacion SET eliminado_en = now() WHERE id = ?", propia);

        assertThat(consulta.postulacionesEnLaGestion(usuario, gestionDe(conv))).isEmpty();
    }
}
