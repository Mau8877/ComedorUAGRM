package com.comedoruagrm.backend.config;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.features.usuarios.constant.RolNombres;
import com.comedoruagrm.backend.features.usuarios.model.Rol;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.repository.RolRepository;
import com.comedoruagrm.backend.features.usuarios.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Cuenta inicial de Administrador DUBSS para desarrollo: sin ella no hay forma de entrar al panel
 * (el rol no se puede elegir al registrarse y el alta del personal la hace un administrador).
 *
 * * Solo con el perfil `dev` (el de docker-compose) y, por defecto, activa; se apaga con
 * * `app.seed.admin.enabled=false`. Es idempotente: si ya existe una cuenta con ese correo, no
 * * hace nada (no pisa una contraseña cambiada).
 * * Correo y contraseña salen de `app.seed.admin.*` (valores de prueba del entorno de desarrollo).
 * ! NUNCA en produccion: no es un perfil activo alli y la contraseña es de prueba. Con el perfil
 * ! `prod` (o cualquiera distinto de `dev`) este bean ni se crea.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.admin.enabled", havingValue = "true", matchIfMissing = true)
@Order(1)
@RequiredArgsConstructor
public class DatosInicialesAdmin implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DatosInicialesAdmin.class);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin.correo:admin@dubss.com}")
    private String correo;

    @Value("${app.seed.admin.contrasena:123123123}")
    private String contrasena;

    @Value("${app.seed.admin.ci:0000001}")
    private String ci;

    @Override
    @Transactional
    public void run(String... args) {
        String correoNormalizado = correo.trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByCorreo(correoNormalizado)) {
            return;
        }
        if (usuarioRepository.existsByCi(ci)) {
            LOG.warn("No se creó el administrador inicial: el CI {} ya pertenece a otra cuenta", ci);
            return;
        }
        Rol rol = rolRepository.findByNombre(RolNombres.ADMINISTRADOR_DUBSS).orElse(null);
        if (rol == null) {
            LOG.warn("No se creó el administrador inicial: no existe el rol {}", RolNombres.ADMINISTRADOR_DUBSS);
            return;
        }
        usuarioRepository.save(new Usuario(rol, ci, correoNormalizado, passwordEncoder.encode(contrasena),
                "Administrador", "DUBSS"));
        LOG.info("Administrador DUBSS inicial creado para desarrollo: {}", correoNormalizado);
    }
}
