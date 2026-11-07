package com.comedoruagrm.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import io.minio.MinioClient;

/**
 * Clientes de MinIO (archivos del expediente, CU07).
 *
 * * `minioClient` (el principal) habla con MinIO por la red interna (`app.minio.url`): sube y consulta objetos.
 * * `minioClientPublico` solo FIRMA direcciones temporales con el host que ve el navegador
 * * (`app.minio.public-url`), porque la firma incluye el host y `http://comedor-uagrm-minio:9000` solo existe
 * * dentro de Docker.
 * * La region va fija: asi firmar una URL no consulta la red (MinIO local no usa regiones de verdad).
 * ! Las credenciales vienen de variables de entorno; nunca se loguean.
 */
@Configuration
public class MinioConfig {

    static final String REGION = "us-east-1";

    @Bean
    @Primary
    public MinioClient minioClient(@Value("${app.minio.url}") String url,
            @Value("${app.minio.access-key}") String accessKey, @Value("${app.minio.secret-key}") String secretKey) {
        return construir(url, accessKey, secretKey);
    }

    @Bean
    public MinioClient minioClientPublico(@Value("${app.minio.public-url}") String url,
            @Value("${app.minio.access-key}") String accessKey, @Value("${app.minio.secret-key}") String secretKey) {
        return construir(url, accessKey, secretKey);
    }

    private static MinioClient construir(String url, String accessKey, String secretKey) {
        return MinioClient.builder().endpoint(url).credentials(accessKey, secretKey).region(REGION).build();
    }
}
