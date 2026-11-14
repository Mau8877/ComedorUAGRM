package com.comedoruagrm.backend.config;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import com.google.auth.oauth2.GoogleCredentials;

/**
 * Condicion de {@link FirebaseConfig}: hay credenciales de Firebase utilizables.
 *
 * * Es verdadera solo si `app.firebase.credentials-path` apunta a un archivo que existe y
 * * que se puede leer como cuenta de servicio. Vacia, inexistente o corrupta: falsa, y el
 * * backend arranca igual con el push en modo log (CU-EXTRA-01, A7). Solo parsea el
 * * archivo, no abre ninguna conexion.
 * ! Nunca escribe el contenido del archivo; ni siquiera su ruta.
 */
public class FirebaseConfigurado implements Condition {

    private static final Logger LOG = LoggerFactory.getLogger(FirebaseConfigurado.class);

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String ruta = context.getEnvironment().getProperty("app.firebase.credentials-path");
        if (ruta == null || ruta.isBlank()) {
            LOG.warn("Firebase sin credenciales: el push queda en modo log", kv("module", "notificaciones"));
            return false;
        }
        Path archivo = Path.of(ruta.trim());
        if (!Files.isReadable(archivo)) {
            LOG.warn("Firebase sin credenciales (el archivo no existe o no se puede leer): el push queda en modo log",
                    kv("module", "notificaciones"));
            return false;
        }
        try (InputStream entrada = Files.newInputStream(archivo)) {
            GoogleCredentials.fromStream(entrada);
            return true;
        } catch (IOException | RuntimeException e) {
            // Solo el tipo: el mensaje de la excepcion podria traer parte del contenido.
            LOG.warn("Las credenciales de Firebase no son validas: el push queda en modo log",
                    kv("module", "notificaciones"), kv("causa", e.getClass().getSimpleName()));
            return false;
        }
    }
}
