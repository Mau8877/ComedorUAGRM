package com.comedoruagrm.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import io.minio.credentials.ChainedProvider;
import io.minio.credentials.StaticProvider;

/** Configuracion del almacenamiento: MinIO con claves en desarrollo, S3 con el rol de AWS en produccion. */
class MinioConfigTest {

    @Test
    void proveedor_static_usaLasClavesDeAcceso() throws Exception {
        assertThat(MinioConfig.proveedor("static", "clave", "secreto")).isInstanceOf(StaticProvider.class);
        assertThat(MinioConfig.proveedor("static", "clave", "secreto").fetch().accessKey()).isEqualTo("clave");
    }

    @Test
    void proveedor_awsIam_usaLaCadenaDeCredencialesDeAws() {
        assertThat(MinioConfig.proveedor("aws-iam", "", "")).isInstanceOf(ChainedProvider.class);
    }

    @Test
    void proveedor_desconocido_falla() {
        assertThatThrownBy(() -> MinioConfig.proveedor("otro", "a", "b")).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("static").hasMessageContaining("aws-iam");
    }

    @Test
    void construir_firmaConLaRegionConfigurada() throws Exception {
        MinioClient s3 = MinioConfig.construir("https://s3.sa-east-1.amazonaws.com", "sa-east-1", "static", "clave",
                "secreto-de-prueba");

        String url = s3.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().method(Method.GET)
                .bucket("bucket").object("a/b.pdf").expiry(300).build());

        // La region va dentro del alcance de la firma: `clave/<fecha>/sa-east-1/s3/aws4_request`
        assertThat(url).contains("sa-east-1%2Fs3%2Faws4_request");
    }
}
