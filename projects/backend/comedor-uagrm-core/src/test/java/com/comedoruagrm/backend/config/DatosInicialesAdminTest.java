package com.comedoruagrm.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.comedoruagrm.backend.features.usuarios.model.Rol;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.repository.RolRepository;
import com.comedoruagrm.backend.features.usuarios.repository.UsuarioRepository;

class DatosInicialesAdminTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final RolRepository rolRepository = mock(RolRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final DatosInicialesAdmin seeder = new DatosInicialesAdmin(usuarioRepository, rolRepository, passwordEncoder);

    @BeforeEach
    void configurar() {
        ReflectionTestUtils.setField(seeder, "correo", "Admin@Dubss.com");
        ReflectionTestUtils.setField(seeder, "contrasena", "123123123");
        ReflectionTestUtils.setField(seeder, "ci", "0000001");
    }

    @Test
    void run_creaElAdministradorConElCorreoNormalizadoYLaContrasenaHasheada() {
        Rol rol = mock(Rol.class);
        when(rolRepository.findByNombre("ADMINISTRADOR_DUBSS")).thenReturn(Optional.of(rol));

        seeder.run();

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        Usuario admin = guardado.getValue();
        assertThat(admin.getCorreo()).isEqualTo("admin@dubss.com");
        assertThat(admin.getRol()).isSameAs(rol);
        assertThat(admin.getPasswordHash()).isNotEqualTo("123123123");
        assertThat(passwordEncoder.matches("123123123", admin.getPasswordHash())).isTrue();
    }

    @Test
    void run_siYaExisteLaCuentaNoHaceNada() {
        when(usuarioRepository.existsByCorreo("admin@dubss.com")).thenReturn(true);

        seeder.run();

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void run_siElCiYaEsDeOtraCuentaNoCreaNada() {
        when(usuarioRepository.existsByCi("0000001")).thenReturn(true);

        seeder.run();

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void run_sinElRolNoCreaNada() {
        when(rolRepository.findByNombre("ADMINISTRADOR_DUBSS")).thenReturn(Optional.empty());

        seeder.run();

        verify(usuarioRepository, never()).save(any());
    }
}
