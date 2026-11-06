package com.comedoruagrm.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.comedoruagrm.backend.features.notificaciones.service.CanalPush;
import com.comedoruagrm.backend.features.notificaciones.service.CanalPushFcm;
import com.comedoruagrm.backend.features.notificaciones.service.CanalPushLog;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

/** Firebase solo se configura con credenciales utilizables; sin ellas el backend sigue en pie (CU-EXTRA-01, A7). */
class FirebaseConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(FirebaseConfig.class, CanalPushConfig.class)
            .withPropertyValues("app.notificaciones.android-channel-id=notificaciones");

    @TempDir
    Path carpeta;

    @AfterEach
    void limpiarFirebase() {
        FirebaseApp.getApps().forEach(FirebaseApp::delete);
    }

    @Test
    void sinRuta_noHayBeanDeFirebaseYElCanalEsElDeLog() {
        runner.withPropertyValues("app.firebase.credentials-path=").run(contexto -> {
            assertThat(contexto).hasNotFailed();
            assertThat(contexto).doesNotHaveBean(FirebaseMessaging.class);
            assertThat(contexto.getBean(CanalPush.class)).isInstanceOf(CanalPushLog.class);
        });
    }

    @Test
    void conRutaInexistente_noHayBeanDeFirebaseYElArranqueNoFalla() {
        String ruta = carpeta.resolve("no-existe.json").toString();

        runner.withPropertyValues("app.firebase.credentials-path=" + ruta).run(contexto -> {
            assertThat(contexto).hasNotFailed();
            assertThat(contexto).doesNotHaveBean(FirebaseMessaging.class);
            assertThat(contexto.getBean(CanalPush.class)).isInstanceOf(CanalPushLog.class);
        });
    }

    @Test
    void conArchivoCorrupto_noHayBeanDeFirebaseYElArranqueNoFalla() throws Exception {
        Path archivo = Files.writeString(carpeta.resolve("corrupto.json"), "esto no es json");

        runner.withPropertyValues("app.firebase.credentials-path=" + archivo).run(contexto -> {
            assertThat(contexto).hasNotFailed();
            assertThat(contexto).doesNotHaveBean(FirebaseMessaging.class);
            assertThat(contexto.getBean(CanalPush.class)).isInstanceOf(CanalPushLog.class);
        });
    }

    @Test
    void conCuentaDeServicioValida_creaFirebaseMessagingYElCanalFcm() throws Exception {
        Path archivo = Files.writeString(carpeta.resolve("firebase-sa.json"), cuentaDeServicioDePrueba());

        runner.withPropertyValues("app.firebase.credentials-path=" + archivo).run(contexto -> {
            assertThat(contexto).hasNotFailed();
            assertThat(contexto).hasSingleBean(FirebaseMessaging.class);
            assertThat(contexto.getBean(CanalPush.class)).isInstanceOf(CanalPushFcm.class);
        });
    }

    /** Una cuenta de servicio con forma valida y una llave RSA generada al vuelo (no es de ningun proyecto real). */
    private static String cuentaDeServicioDePrueba() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        KeyPair par = generador.generateKeyPair();
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                        .encodeToString(par.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        return """
                {
                  "type": "service_account",
                  "project_id": "proyecto-de-prueba",
                  "private_key_id": "0000000000000000000000000000000000000000",
                  "private_key": "%s",
                  "client_email": "prueba@proyecto-de-prueba.iam.gserviceaccount.com",
                  "client_id": "000000000000000000000",
                  "token_uri": "https://oauth2.googleapis.com/token"
                }
                """.formatted(pem.replace("\n", "\\n"));
    }
}
