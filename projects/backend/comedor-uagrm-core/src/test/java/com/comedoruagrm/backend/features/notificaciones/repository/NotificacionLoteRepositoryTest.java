package com.comedoruagrm.backend.features.notificaciones.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.features.notificaciones.enums.PlataformaDispositivo;
import com.comedoruagrm.backend.features.notificaciones.enums.TipoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.model.DispositivoFcm;
import com.comedoruagrm.backend.features.notificaciones.repository.NotificacionLoteRepository.AvisoGuardado;
import com.comedoruagrm.backend.features.notificaciones.repository.NotificacionLoteRepository.NuevoAviso;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.repository.UsuarioRepository;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Alta de avisos por lotes y las consultas en bloque del despacho (CU22), con Postgres real. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificacionLoteRepositoryTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private DispositivoFcmRepository dispositivoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeAll
    static void saltarSiNoHayPostgres() {
        PostgresTestDatabase.asegurarDisponible();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.registrar(registry);
    }

    private NotificacionLoteRepository repositorio() {
        return new NotificacionLoteRepository(new NamedParameterJdbcTemplate(jdbc));
    }

    @Test
    void insertar_guardaTodosLosAvisosEnOrden_conSuTipoYSuPostulacion_yDevuelveIdYCodigo() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long u1 = fx.postulante("9800001");
        long u2 = fx.postulante("9800002");
        long postulacion = fx.postulacion(fx.convocatoria("NLOTE"), u2, "calificada");

        List<AvisoGuardado> guardados = repositorio().insertar(List.of(
                new NuevoAviso(u1, "Titulo", "Mensaje uno", TipoNotificacion.SUCCESS, null),
                new NuevoAviso(u2, "Titulo", "Mensaje dos", TipoNotificacion.INFO, postulacion)));

        assertThat(guardados).extracting(AvisoGuardado::usuarioId).containsExactly(u1, u2);
        assertThat(guardados).allSatisfy(g -> {
            assertThat(g.id()).isPositive();
            assertThat(g.codigo()).isNotNull();
        });
        var filas = jdbc.queryForList("""
                SELECT usuario_id, mensaje, tipo::text AS tipo, postulacion_id, leida, enviada_en
                FROM notificacion WHERE usuario_id IN (?, ?) ORDER BY id""", u1, u2);
        assertThat(filas).hasSize(2);
        assertThat(filas.get(0)).containsEntry("mensaje", "Mensaje uno").containsEntry("tipo", "SUCCESS")
                .containsEntry("postulacion_id", null).containsEntry("leida", false).containsEntry("enviada_en", null);
        assertThat(filas.get(1)).containsEntry("tipo", "INFO").containsEntry("postulacion_id", postulacion);
    }

    @Test
    void insertar_quinientosAvisos_van_enUnaSolaSentencia() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long usuario = fx.postulante("9800010");
        List<NuevoAviso> avisos = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            avisos.add(new NuevoAviso(usuario, "Titulo", "Mensaje " + i, TipoNotificacion.INFO, null));
        }

        List<AvisoGuardado> guardados = repositorio().insertar(avisos);

        assertThat(guardados).hasSize(500);
        assertThat(guardados).extracting(AvisoGuardado::id).doesNotHaveDuplicates();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacion WHERE usuario_id = ?", Long.class,
                usuario)).isEqualTo(500);
    }

    @Test
    void insertar_sinAvisos_noHaceNada() {
        assertThat(repositorio().insertar(List.of())).isEmpty();
    }

    @Test
    void marcarEnviadas_actualizaSoloLosIndicados_conUnaSolaSentencia() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long usuario = fx.postulante("9800020");
        List<AvisoGuardado> g = repositorio().insertar(List.of(
                new NuevoAviso(usuario, "T", "uno", TipoNotificacion.INFO, null),
                new NuevoAviso(usuario, "T", "dos", TipoNotificacion.INFO, null),
                new NuevoAviso(usuario, "T", "tres", TipoNotificacion.INFO, null)));

        int cambiados = notificacionRepository.marcarEnviadas(List.of(g.get(0).id(), g.get(2).id()), Instant.now());

        assertThat(cambiados).isEqualTo(2);
        assertThat(jdbc.queryForList("SELECT mensaje FROM notificacion WHERE usuario_id = ? AND enviada_en IS NOT NULL "
                + "ORDER BY id", String.class, usuario)).containsExactly("uno", "tres");
    }

    @Test
    void findAllByUsuarioIdIn_traeLosTelefonosDeVariosUsuariosEnUnaConsulta() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        Usuario ana = usuarioRepository.findById(fx.postulante("9800030")).orElseThrow();
        Usuario beto = usuarioRepository.findById(fx.postulante("9800031")).orElseThrow();
        Usuario sinTelefono = usuarioRepository.findById(fx.postulante("9800032")).orElseThrow();
        dispositivoRepository.saveAndFlush(new DispositivoFcm(ana, "lote-ana-1", PlataformaDispositivo.ANDROID));
        dispositivoRepository.saveAndFlush(new DispositivoFcm(ana, "lote-ana-2", PlataformaDispositivo.ANDROID));
        dispositivoRepository.saveAndFlush(new DispositivoFcm(beto, "lote-beto", PlataformaDispositivo.ANDROID));

        List<DispositivoFcm> encontrados = dispositivoRepository.findAllByUsuarioIdIn(
                List.of(ana.getId(), beto.getId(), sinTelefono.getId()));

        assertThat(encontrados).extracting(DispositivoFcm::getToken)
                .containsExactlyInAnyOrder("lote-ana-1", "lote-ana-2", "lote-beto");
    }
}
