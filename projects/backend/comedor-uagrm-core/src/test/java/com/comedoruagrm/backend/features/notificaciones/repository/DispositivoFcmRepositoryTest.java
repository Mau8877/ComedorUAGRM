package com.comedoruagrm.backend.features.notificaciones.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.features.notificaciones.enums.PlataformaDispositivo;
import com.comedoruagrm.backend.features.notificaciones.model.DispositivoFcm;
import com.comedoruagrm.backend.features.usuarios.constant.RolNombres;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.repository.RolRepository;
import com.comedoruagrm.backend.features.usuarios.repository.UsuarioRepository;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Telefonos registrados (CU-EXTRA-01). Cada test se revierte (@DataJpaTest). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DispositivoFcmRepositoryTest {

    @Autowired
    private DispositivoFcmRepository dispositivoRepository;

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
    void guardar_yBuscarPorTokenYPorUsuario_devuelveElRegistroActivo() {
        Usuario ana = usuario("1", "ana@uagrm.edu.bo");
        Usuario beto = usuario("2", "beto@uagrm.edu.bo");
        dispositivoRepository.saveAndFlush(new DispositivoFcm(ana, "token-ana-1", PlataformaDispositivo.ANDROID));
        dispositivoRepository.saveAndFlush(new DispositivoFcm(ana, "token-ana-2", PlataformaDispositivo.ANDROID));
        dispositivoRepository.saveAndFlush(new DispositivoFcm(beto, "token-beto", PlataformaDispositivo.ANDROID));

        assertThat(dispositivoRepository.findByToken("token-ana-1")).isPresent();
        assertThat(dispositivoRepository.findAllByUsuarioId(ana.getId())).hasSize(2);
        assertThat(dispositivoRepository.findAllByTokenIn(List.of("token-ana-1", "token-beto"))).hasSize(2);
        assertThat(dispositivoRepository.findByToken("no-existe")).isEmpty();
    }

    @Test
    void guardar_dosVecesElMismoTokenActivo_violaElIndiceUnico() {
        Usuario ana = usuario("1", "ana@uagrm.edu.bo");
        dispositivoRepository.saveAndFlush(new DispositivoFcm(ana, "token-repetido", PlataformaDispositivo.ANDROID));

        assertThatThrownBy(() -> dispositivoRepository
                .saveAndFlush(new DispositivoFcm(ana, "token-repetido", PlataformaDispositivo.ANDROID)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void guardar_elMismoTokenConElAnteriorEliminadoLogicamente_loPermite() {
        Usuario ana = usuario("1", "ana@uagrm.edu.bo");
        DispositivoFcm viejo = dispositivoRepository
                .saveAndFlush(new DispositivoFcm(ana, "token-reusado", PlataformaDispositivo.ANDROID));
        viejo.setEliminadoEn(Instant.now());
        dispositivoRepository.saveAndFlush(viejo);

        assertThat(dispositivoRepository.findByToken("token-reusado")).isEmpty();
        dispositivoRepository.saveAndFlush(new DispositivoFcm(ana, "token-reusado", PlataformaDispositivo.ANDROID));
        assertThat(dispositivoRepository.findByToken("token-reusado")).isPresent();
    }

    private Usuario usuario(String ci, String correo) {
        return usuarioRepository.saveAndFlush(new Usuario(
                rolRepository.findByNombre(RolNombres.POSTULANTE).orElseThrow(), ci, correo,
                "$2a$04$hash-de-prueba", "Nombre", "Apellido"));
    }
}
