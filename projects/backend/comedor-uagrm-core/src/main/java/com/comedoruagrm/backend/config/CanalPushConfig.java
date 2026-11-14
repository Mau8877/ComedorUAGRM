package com.comedoruagrm.backend.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.comedoruagrm.backend.features.notificaciones.service.CanalPush;
import com.comedoruagrm.backend.features.notificaciones.service.CanalPushFcm;
import com.comedoruagrm.backend.features.notificaciones.service.CanalPushLog;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Elige el canal de push: Firebase si hay credenciales (existe el bean `FirebaseMessaging`),
 * el que solo escribe en el log si no (CU-EXTRA-01).
 */
@Configuration
public class CanalPushConfig {

    @Bean
    public CanalPush canalPush(ObjectProvider<FirebaseMessaging> firebaseMessaging,
            @Value("${app.notificaciones.android-channel-id}") String canalAndroid) {
        FirebaseMessaging messaging = firebaseMessaging.getIfAvailable();
        return messaging == null ? new CanalPushLog() : new CanalPushFcm(messaging, canalAndroid);
    }
}
