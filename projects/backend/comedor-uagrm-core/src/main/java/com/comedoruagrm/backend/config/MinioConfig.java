package com.comedoruagrm.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import io.minio.MinioClient;
import io.minio.credentials.AwsEnvironmentProvider;
import io.minio.credentials.ChainedProvider;
import io.minio.credentials.IamAwsProvider;
import io.minio.credentials.Provider;
import io.minio.credentials.StaticProvider;

/**
 * Clientes del almacenamiento de archivos (documentos, CU07 y CU16): MinIO en desarrollo, Amazon S3 en
 * produccion. El cliente de MinIO habla el protocolo S3, asi que pasar a AWS es solo configuracion.
 *
 * * `minioClient` (el principal) habla con el almacenamiento por la red interna (`app.minio.url`): sube,
 * * consulta y elimina objetos.
 * * `minioClientPublico` solo FIRMA direcciones temporales con el host que ve el navegador
 * * (`app.minio.public-url`), porque la firma incluye el host y `http://comedor-uagrm-minio:9000` solo existe
 * * dentro de Docker. En AWS no hay dos direcciones: `public-url` queda igual a `url`.
 * * `app.minio.region` es la region del bucket. En MinIO local no importa (`us-east-1`); en S3 tiene que ser
 * * la real, porque la firma la incluye. Va fija en el cliente: firmar una URL no consulta la red.
 * * `app.minio.credentials`: `static` (clave de acceso y secreto, para MinIO) o `aws-iam` (el rol de la
 * * maquina o del contenedor, o las variables `AWS_*`, sin guardar claves en la configuracion).
 * ! Las credenciales vienen de variables de entorno; nunca se loguean.
 */
@Configuration
public class MinioConfig {

    static final String CREDENCIALES_ESTATICAS = "static";
    static final String CREDENCIALES_AWS_IAM = "aws-iam";

    @Bean
    @Primary
    public MinioClient minioClient(@Value("${app.minio.url}") String url,
            @Value("${app.minio.region}") String region,
            @Value("${app.minio.credentials}") String credenciales,
            @Value("${app.minio.access-key:}") String accessKey,
            @Value("${app.minio.secret-key:}") String secretKey) {
        return construir(url, region, credenciales, accessKey, secretKey);
    }

    @Bean
    public MinioClient minioClientPublico(@Value("${app.minio.public-url}") String url,
            @Value("${app.minio.region}") String region,
            @Value("${app.minio.credentials}") String credenciales,
            @Value("${app.minio.access-key:}") String accessKey,
            @Value("${app.minio.secret-key:}") String secretKey) {
        return construir(url, region, credenciales, accessKey, secretKey);
    }

    static MinioClient construir(String url, String region, String credenciales, String accessKey,
            String secretKey) {
        return MinioClient.builder().endpoint(url).region(region)
                .credentialsProvider(proveedor(credenciales, accessKey, secretKey)).build();
    }

    static Provider proveedor(String credenciales, String accessKey, String secretKey) {
        if (CREDENCIALES_AWS_IAM.equals(credenciales)) {
            return new ChainedProvider(new AwsEnvironmentProvider(), new IamAwsProvider(null, null));
        }
        if (!CREDENCIALES_ESTATICAS.equals(credenciales)) {
            throw new IllegalStateException("app.minio.credentials debe ser «static» o «aws-iam»");
        }
        return new StaticProvider(accessKey, secretKey, null);
    }
}
