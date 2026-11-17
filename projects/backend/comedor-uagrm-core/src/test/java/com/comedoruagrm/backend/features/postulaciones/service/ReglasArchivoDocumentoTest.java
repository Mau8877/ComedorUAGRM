package com.comedoruagrm.backend.features.postulaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.postulaciones.exception.DocumentoPostulacionErrorCodes;

class ReglasArchivoDocumentoTest {

    private static final Set<String> TODOS = Set.of("application/pdf", "image/jpeg", "image/png");

    private final ReglasArchivoDocumento reglas = new ReglasArchivoDocumento(1024, TODOS, 5);

    private static byte[] conRelleno(byte[] cabecera) {
        byte[] r = Arrays.copyOf(cabecera, cabecera.length + 10);
        return r;
    }

    private static final byte[] PDF = conRelleno(new byte[] {'%', 'P', 'D', 'F', '-', '1'});
    private static final byte[] JPEG = conRelleno(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});
    private static final byte[] PNG = conRelleno(
            new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});

    private static String codigoDe(Throwable e) {
        return ((BusinessException) e).getErrorCode();
    }

    @Test
    void validar_reconoceElFormatoPorElContenido() {
        assertThat(reglas.validar(new ArchivoSubido("a.pdf", "application/pdf", PDF)))
                .isEqualTo(new ReglasArchivoDocumento.Formato("application/pdf", "pdf"));
        assertThat(reglas.validar(new ArchivoSubido("a.jpg", "image/jpeg", JPEG)))
                .isEqualTo(new ReglasArchivoDocumento.Formato("image/jpeg", "jpg"));
        assertThat(reglas.validar(new ArchivoSubido("a.png", "image/png", PNG)))
                .isEqualTo(new ReglasArchivoDocumento.Formato("image/png", "png"));
    }

    @Test
    void validar_conTipoGenericoDelCliente_noEstorba() {
        assertThat(reglas.validar(new ArchivoSubido("a", "application/octet-stream", PDF)).mime())
                .isEqualTo("application/pdf");
        assertThat(reglas.validar(new ArchivoSubido("a", null, JPEG)).mime()).isEqualTo("image/jpeg");
    }

    @Test
    void validar_conImageJpgYParametros_loNormaliza() {
        assertThat(reglas.validar(new ArchivoSubido("a", "Image/JPG; charset=x", JPEG)).mime())
                .isEqualTo("image/jpeg");
    }

    @Test
    void validar_sinArchivoOVacio_lanzaErrDoc01() {
        ArchivoSubido sinContenido = new ArchivoSubido("a", "x", null);
        ArchivoSubido vacio = new ArchivoSubido("a", "x", new byte[0]);
        for (ArchivoSubido a : new ArchivoSubido[] {null, sinContenido, vacio}) {
            assertThatThrownBy(() -> reglas.validar(a)).isInstanceOf(ValidationException.class)
                    .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(DocumentoPostulacionErrorCodes.ERR_DOC_01));
        }
    }

    @Test
    void validar_conArchivoMasGrandeQueElMaximo_lanzaErrDoc02() {
        byte[] grande = Arrays.copyOf(PDF, 1025);

        assertThatThrownBy(() -> reglas.validar(new ArchivoSubido("a.pdf", "application/pdf", grande)))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(DocumentoPostulacionErrorCodes.ERR_DOC_02));
    }

    @Test
    void validar_conArchivoDelTamanoExactoDelMaximo_loAcepta() {
        byte[] exacto = Arrays.copyOf(PDF, 1024);

        assertThat(reglas.validar(new ArchivoSubido("a.pdf", "application/pdf", exacto)).mime())
                .isEqualTo("application/pdf");
    }

    @Test
    void validar_conContenidoDesconocidoAunqueLaExtensionDigaPdf_lanzaErrDoc03() {
        byte[] texto = "MZ ejecutable disfrazado".getBytes();

        assertThatThrownBy(() -> reglas.validar(new ArchivoSubido("trampa.pdf", "application/pdf", texto)))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(DocumentoPostulacionErrorCodes.ERR_DOC_03));
    }

    @Test
    void validar_conContenidoQueNoCoincideConElTipoDeclarado_lanzaErrDoc03() {
        assertThatThrownBy(() -> reglas.validar(new ArchivoSubido("a.png", "image/png", JPEG)))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(DocumentoPostulacionErrorCodes.ERR_DOC_03))
                .hasMessageContaining("no corresponde");
    }

    @Test
    void validar_conFormatoReconocidoPeroNoPermitido_lanzaErrDoc03ConLosPermitidos() {
        ReglasArchivoDocumento soloPdf = new ReglasArchivoDocumento(1024, Set.of("application/pdf"), 5);

        assertThatThrownBy(() -> soloPdf.validar(new ArchivoSubido("a.png", "image/png", PNG)))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(DocumentoPostulacionErrorCodes.ERR_DOC_03))
                .hasMessageContaining("PDF").hasMessageNotContaining("PNG");
    }

    @Test
    void validar_conCabeceraTruncada_loRechaza() {
        assertThatThrownBy(() -> reglas.validar(new ArchivoSubido("a", "image/png", new byte[] {(byte) 0x89, 'P'})))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(DocumentoPostulacionErrorCodes.ERR_DOC_03));
    }

    @Test
    void validar_elMensajeDeTamanoDiceElMaximoEnMegas() {
        ReglasArchivoDocumento cincoMegas = new ReglasArchivoDocumento(5 * 1024 * 1024, TODOS, 5);
        byte[] grande = Arrays.copyOf(PDF, 5 * 1024 * 1024 + 1);

        assertThatThrownBy(() -> cincoMegas.validar(new ArchivoSubido("a.pdf", "application/pdf", grande)))
                .hasMessageContaining("5 MB");
    }
}
