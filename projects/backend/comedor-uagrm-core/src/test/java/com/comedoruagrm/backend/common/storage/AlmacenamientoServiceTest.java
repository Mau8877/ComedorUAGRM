package com.comedoruagrm.backend.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import io.minio.StatObjectResponse;

/**
 * Almacenamiento en MinIO (CU07). La URL temporal se firma de verdad con un cliente real (no consulta la
 * red porque la region va fija); las demas operaciones usan un cliente simulado.
 */
class AlmacenamientoServiceTest {

    private static final String BUCKET = "comedor-uagrm-storage";

    private final MinioClient interno = mock(MinioClient.class);

    private static MinioClient clientePublico() {
        return MinioClient.builder().endpoint("http://localhost:9000").credentials("clave", "secreto-de-prueba")
                .region("us-east-1").build();
    }

    private AlmacenamientoService servicio() {
        return new AlmacenamientoService(interno, clientePublico(), BUCKET);
    }

    @Test
    void urlTemporal_llevaElBucketLaRutaLaFirmaYVenceALosCincoMinutos() {
        String url = servicio().urlTemporal("expedientes/ab12/ci.pdf", AlmacenamientoService.VIGENCIA_ACCESO);

        assertThat(url).startsWith("http://localhost:9000/" + BUCKET + "/expedientes/ab12/ci.pdf?");
        assertThat(url).contains("X-Amz-Signature=").contains("X-Amz-Expires=300");
    }

    @Test
    void urlTemporal_seFirmaConElHostPublicoYNoConElInterno() {
        MinioClient publico = MinioClient.builder().endpoint("https://archivos.comedoru.test")
                .credentials("clave", "secreto-de-prueba").region("us-east-1").build();
        AlmacenamientoService servicio = new AlmacenamientoService(interno, publico, BUCKET);

        assertThat(servicio.urlTemporal("a/b.pdf", Duration.ofMinutes(5)))
                .startsWith("https://archivos.comedoru.test/" + BUCKET + "/a/b.pdf?");
    }

    @Test
    void subir_guardaElObjetoEnElBucketConSuTipo() throws Exception {
        servicio().subir("a/b.pdf", new byte[] {1, 2, 3}, "application/pdf");

        ArgumentCaptor<PutObjectArgs> captura = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(interno).putObject(captura.capture());
        assertThat(captura.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(captura.getValue().object()).isEqualTo("a/b.pdf");
        assertThat(captura.getValue().contentType()).isEqualTo("application/pdf");
    }

    @Test
    void subir_conMinioCaido_lanzaAlmacenamientoExceptionSinLaRutaEnElMensaje() throws Exception {
        when(interno.putObject(any(PutObjectArgs.class))).thenThrow(new IOException("connection refused"));

        assertThatThrownBy(() -> servicio().subir("secreta/ruta.pdf", new byte[] {1}, "application/pdf"))
                .isInstanceOf(AlmacenamientoException.class)
                .hasMessageNotContaining("secreta/ruta.pdf");
    }

    @Test
    void existe_conElObjetoPresente_esTrue() throws Exception {
        when(interno.statObject(any(StatObjectArgs.class))).thenReturn(mock(StatObjectResponse.class));

        assertThat(servicio().existe("a/b.pdf")).isTrue();
    }

    @Test
    void existe_conNoSuchKey_esFalse() throws Exception {
        ErrorResponse respuesta = mock(ErrorResponse.class);
        when(respuesta.code()).thenReturn("NoSuchKey");
        ErrorResponseException ausente = mock(ErrorResponseException.class);
        when(ausente.errorResponse()).thenReturn(respuesta);
        when(interno.statObject(any(StatObjectArgs.class))).thenThrow(ausente);

        assertThat(servicio().existe("a/b.pdf")).isFalse();
    }

    @Test
    void existe_conOtroErrorDeMinio_lanzaAlmacenamientoException() throws Exception {
        ErrorResponse respuesta = mock(ErrorResponse.class);
        when(respuesta.code()).thenReturn("AccessDenied");
        ErrorResponseException denegado = mock(ErrorResponseException.class);
        when(denegado.errorResponse()).thenReturn(respuesta);
        when(interno.statObject(any(StatObjectArgs.class))).thenThrow(denegado);

        assertThatThrownBy(() -> servicio().existe("a/b.pdf")).isInstanceOf(AlmacenamientoException.class);
    }

    @Test
    void existe_conMinioCaido_lanzaAlmacenamientoException() throws Exception {
        when(interno.statObject(any(StatObjectArgs.class))).thenThrow(new IOException("connection refused"));

        assertThatThrownBy(() -> servicio().existe("a/b.pdf")).isInstanceOf(AlmacenamientoException.class);
    }
}
