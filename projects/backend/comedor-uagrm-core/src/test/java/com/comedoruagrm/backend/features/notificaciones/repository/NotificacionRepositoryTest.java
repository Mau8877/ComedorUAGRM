package com.comedoruagrm.backend.features.notificaciones.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.features.notificaciones.enums.TipoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.model.Notificacion;
import com.comedoruagrm.backend.features.usuarios.constant.RolNombres;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.repository.RolRepository;
import com.comedoruagrm.backend.features.usuarios.repository.UsuarioRepository;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Bandeja de avisos (CU-EXTRA-01). Cada test se revierte (@DataJpaTest). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificacionRepositoryTest {

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @BeforeAll
    static void saltarSiNoHayPostgres() {
        PostgresTestDatabase.asegurarDisponible();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.registrar(registry);
    }

    @Test
    void guardar_persisteTipoCodigoYValoresPorDefecto() {
        Usuario ana = usuario("1", "ana@uagrm.edu.bo");

        Notificacion guardada = notificacionRepository.saveAndFlush(
                new Notificacion(ana, "Titulo", "Mensaje largo", TipoNotificacion.WARNING));

        Notificacion leida = notificacionRepository.findByCodigo(guardada.getCodigo()).orElseThrow();
        assertThat(leida.getTipo()).isEqualTo(TipoNotificacion.WARNING);
        assertThat(leida.getTitulo()).isEqualTo("Titulo");
        assertThat(leida.isLeida()).isFalse();
        assertThat(leida.getEnviadaEn()).isNull();
        assertThat(leida.getCreadoEn()).isNotNull();
    }

    @Test
    void countByUsuarioId_cuentaSoloLasDeEseUsuario() {
        Usuario ana = usuario("1", "ana@uagrm.edu.bo");
        Usuario beto = usuario("2", "beto@uagrm.edu.bo");
        notificacionRepository.saveAndFlush(new Notificacion(ana, "a", "m", TipoNotificacion.INFO));
        notificacionRepository.saveAndFlush(new Notificacion(ana, "b", "m", TipoNotificacion.INFO));
        notificacionRepository.saveAndFlush(new Notificacion(beto, "c", "m", TipoNotificacion.ERROR));

        assertThat(notificacionRepository.countByUsuarioId(ana.getId())).isEqualTo(2);
        assertThat(notificacionRepository.countByUsuarioId(beto.getId())).isEqualTo(1);
    }

    @Test
    void bandeja_leerTodoYEliminarOperanSoloSobreElUsuarioIndicado() {
        Usuario ana = usuario("1", "ana@uagrm.edu.bo");
        Usuario beto = usuario("2", "beto@uagrm.edu.bo");
        Notificacion propia = notificacionRepository.saveAndFlush(new Notificacion(ana, "a", "m", TipoNotificacion.INFO));
        notificacionRepository.saveAndFlush(new Notificacion(ana, "b", "m", TipoNotificacion.INFO));
        notificacionRepository.saveAndFlush(new Notificacion(beto, "c", "m", TipoNotificacion.INFO));

        assertThat(notificacionRepository.countByUsuarioIdAndLeidaFalse(ana.getId())).isEqualTo(2);
        assertThat(notificacionRepository.findByCodigoAndUsuarioId(propia.getCodigo(), beto.getId())).isEmpty();
        assertThat(notificacionRepository.findByCodigoAndUsuarioId(propia.getCodigo(), ana.getId())).isPresent();

        assertThat(notificacionRepository.marcarTodasLeidas(ana.getId())).isEqualTo(2);
        assertThat(notificacionRepository.countByUsuarioIdAndLeidaFalse(ana.getId())).isZero();
        assertThat(notificacionRepository.countByUsuarioIdAndLeidaFalse(beto.getId())).isEqualTo(1);

        assertThat(notificacionRepository.eliminarTodas(ana.getId())).isEqualTo(2);
        assertThat(notificacionRepository.countByUsuarioId(ana.getId())).isZero();
        assertThat(notificacionRepository.countByUsuarioId(beto.getId())).isEqualTo(1);
    }

    private Usuario usuario(String ci, String correo) {
        return usuarioRepository.saveAndFlush(new Usuario(
                rolRepository.findByNombre(RolNombres.POSTULANTE).orElseThrow(), ci, correo,
                "$2a$04$hash-de-prueba", "Nombre", "Apellido"));
    }
}
