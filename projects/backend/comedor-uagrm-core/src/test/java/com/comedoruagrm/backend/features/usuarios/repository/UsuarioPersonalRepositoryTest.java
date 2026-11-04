package com.comedoruagrm.backend.features.usuarios.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.features.usuarios.constant.RolNombres;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

import jakarta.persistence.EntityManager;

/** Consultas del listado y de las protecciones del CU14. Cada test se revierte (@DataJpaTest). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UsuarioPersonalRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeAll
    static void saltarSiNoHayPostgres() {
        PostgresTestDatabase.asegurarDisponible();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.registrar(registry);
    }

    @Test
    void buscarPersonal_noIncluyeAPostulantesNiCuentasEliminadas() {
        crear("1", "admin@uagrm.edu.bo", "Ana", "Admin", RolNombres.ADMINISTRADOR_DUBSS, true);
        crear("2", "post@uagrm.edu.bo", "Pedro", "Postulante", RolNombres.POSTULANTE, true);
        Usuario eliminado = crear("3", "borrado@uagrm.edu.bo", "Bruno", "Borrado", RolNombres.DIRECTIVO, true);
        eliminado.setEliminadoEn(Instant.now());
        usuarioRepository.flush();

        Page<Usuario> pagina = usuarioRepository.buscarPersonal(null, null, null, PageRequest.of(0, 20));

        assertThat(pagina.getContent()).extracting(Usuario::getCi).containsExactly("1");
    }

    @Test
    void buscarPersonal_filtraPorRolYPorEstado() {
        crear("1", "a@uagrm.edu.bo", "Ana", "Admin", RolNombres.ADMINISTRADOR_DUBSS, true);
        crear("2", "d1@uagrm.edu.bo", "Dario", "Directivo", RolNombres.DIRECTIVO, true);
        crear("3", "d2@uagrm.edu.bo", "Diana", "Directiva", RolNombres.DIRECTIVO, false);
        crear("4", "c@uagrm.edu.bo", "Carlos", "Comedor", RolNombres.PERSONAL_COMEDOR, true);

        assertThat(cis(RolNombres.DIRECTIVO, null, null)).containsExactlyInAnyOrder("2", "3");
        assertThat(cis(null, false, null)).containsExactly("3");
        assertThat(cis(RolNombres.DIRECTIVO, true, null)).containsExactly("2");
        assertThat(cis(null, null, null)).hasSize(4);
    }

    @Test
    void buscarPersonal_buscaPorCiNombreYApellidoSinDistinguirMayusculas() {
        crear("7654321", "a@uagrm.edu.bo", "María José", "Quispe Rojas", RolNombres.DIRECTIVO, true);
        crear("1111111", "b@uagrm.edu.bo", "Otro", "Funcionario", RolNombres.DIRECTIVO, true);

        assertThat(cis(null, null, "%7654%")).containsExactly("7654321");
        assertThat(cis(null, null, "%maría%")).containsExactly("7654321");
        assertThat(cis(null, null, "%quispe%")).containsExactly("7654321");
        assertThat(cis(null, null, "%funcionario%")).containsExactly("1111111");
        assertThat(cis(null, null, "%no-existe%")).isEmpty();
    }

    @Test
    void buscarPersonal_ordenaYPagina() {
        crear("1", "a@uagrm.edu.bo", "A", "Zeta", RolNombres.DIRECTIVO, true);
        crear("2", "b@uagrm.edu.bo", "B", "Alfa", RolNombres.PERSONAL_COMEDOR, true);
        crear("3", "c@uagrm.edu.bo", "C", "Medio", RolNombres.ADMINISTRADOR_DUBSS, true);

        Page<Usuario> primera = usuarioRepository.buscarPersonal(null, null, null,
                PageRequest.of(0, 2, Sort.by("apellidos")));
        Page<Usuario> porRol = usuarioRepository.buscarPersonal(null, null, null,
                PageRequest.of(0, 3, Sort.by("rol.nombre")));

        assertThat(primera.getContent()).extracting(Usuario::getApellidos).containsExactly("Alfa", "Medio");
        assertThat(primera.getTotalElements()).isEqualTo(3);
        assertThat(primera.getTotalPages()).isEqualTo(2);
        assertThat(porRol.getContent()).extracting(Usuario::getCi).containsExactly("3", "1", "2");
    }

    @Test
    void findPersonalByCodigo_devuelveElPersonalPeroNoAUnPostulante() {
        Usuario directivo = crear("1", "d@uagrm.edu.bo", "Dario", "Directivo", RolNombres.DIRECTIVO, true);
        Usuario postulante = crear("2", "p@uagrm.edu.bo", "Pedro", "Post", RolNombres.POSTULANTE, true);

        assertThat(usuarioRepository.findPersonalByCodigo(directivo.getCodigo())).isPresent();
        assertThat(usuarioRepository.findPersonalByCodigo(postulante.getCodigo())).isEmpty();
        assertThat(usuarioRepository.findPersonalByCodigo(java.util.UUID.randomUUID())).isEmpty();
    }

    @Test
    void bloquearAdministradoresActivos_cuentaSoloAdministradoresActivos() {
        crear("1", "a1@uagrm.edu.bo", "A", "Uno", RolNombres.ADMINISTRADOR_DUBSS, true);
        crear("2", "a2@uagrm.edu.bo", "B", "Dos", RolNombres.ADMINISTRADOR_DUBSS, false);
        crear("3", "d@uagrm.edu.bo", "C", "Tres", RolNombres.DIRECTIVO, true);

        List<Usuario> administradores = usuarioRepository.bloquearAdministradoresActivos();

        assertThat(administradores).extracting(Usuario::getCi).containsExactly("1");
    }

    private List<String> cis(String rol, Boolean activo, String busqueda) {
        return usuarioRepository.buscarPersonal(busqueda, rol, activo, PageRequest.of(0, 50)).getContent().stream()
                .map(Usuario::getCi).toList();
    }

    private Usuario crear(String ci, String correo, String nombres, String apellidos, String rol, boolean activo) {
        Usuario usuario = new Usuario(rolRepository.findByNombre(rol).orElseThrow(), ci, correo,
                "$2a$04$hash-de-prueba", nombres, apellidos);
        usuario.setActivo(activo);
        Usuario guardado = usuarioRepository.saveAndFlush(usuario);
        entityManager.clear();
        return usuarioRepository.findById(guardado.getId()).orElseThrow();
    }
}
