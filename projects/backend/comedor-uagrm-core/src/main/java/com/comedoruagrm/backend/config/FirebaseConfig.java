package com.comedoruagrm.backend.config;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Cliente de Firebase Cloud Messaging (push), CU-EXTRA-01.
 *
 * * El bean existe solo si hay credenciales utilizables ({@link FirebaseConfigurado}); si no,
 * * no hay bean y `CanalPushConfig` usa el canal que solo escribe en el log.
 * ! La credencial es secreta (secrets/firebase-sa.json, fuera de git): no se loguea.
 */
@Configuration
public class FirebaseConfig {

    private static final Logger LOG = LoggerFactory.getLogger(FirebaseConfig.class);

    @Bean
    @Conditional(FirebaseConfigurado.class)
    public FirebaseMessaging firebaseMessaging(@Value("${app.firebase.credentials-path}") String ruta)
            throws IOException {
        FirebaseApp app;
        if (FirebaseApp.getApps().isEmpty()) {
            try (InputStream entrada = Files.newInputStream(Path.of(ruta.trim()))) {
                FirebaseOptions opciones = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(entrada))
                        .build();
                app = FirebaseApp.initializeApp(opciones);
            }
        } else {
            app = FirebaseApp.getInstance();
        }
        LOG.info("Firebase configurado: el push se envia por FCM", kv("module", "notificaciones"));
        return FirebaseMessaging.getInstance(app);
    }
}
