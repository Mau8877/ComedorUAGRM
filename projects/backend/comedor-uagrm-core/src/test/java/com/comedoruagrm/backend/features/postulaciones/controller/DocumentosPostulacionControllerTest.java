package com.comedoruagrm.backend.features.postulaciones.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.postulaciones.dto.AccesoDocumentoResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.DocumentoAdjuntoResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.DocumentoRequeridoResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.DocumentosPostulacionResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.ReglasArchivoResponse;
import com.comedoruagrm.backend.features.postulaciones.exception.DocumentoPostulacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.service.ArchivoSubido;
import com.comedoruagrm.backend.features.postulaciones.service.DocumentosPostulacionService;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/**
 * Contrato HTTP de los documentos de la postulacion (CU16) con el service simulado. El `403` real por rol lo da
 * `@PreAuthorize` en el service y se prueba de verdad en un test de integracion.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DocumentosPostulacionControllerTest {

    private static final Instant AHORA = Instant.parse("2026-07-16T15:00:00Z");
    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1'};

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentosPostulacionService service;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID postulante = UUID.randomUUID();
    private final UUID postulacion = UUID.randomUUID();
    private final UUID tipo = UUID.randomUUID();
    private final UUID documento = UUID.randomUUID();

    @BeforeEach
    void autenticar() {
        when(provider.buscarPorCodigo(postulante))
                .thenReturn(Optional.of(new DatosUsuario(postulante, "POSTULANTE", true)));
    }

    private RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(postulante.toString()));
    }

    private String ruta() {
        return "/api/v1/postulaciones/" + postulacion + "/documentos";
    }

    private DocumentoAdjuntoResponse adjunto() {
        return new DocumentoAdjuntoResponse(documento, "carnet.pdf", "application/pdf", 6L, AHORA);
    }

    private MockMultipartFile parteArchivo() {
        return new MockMultipartFile("archivo", "carnet.pdf", "application/pdf", PDF);
    }

    @Test
    void listar_da200ConLosRequeridosYLasReglas_sinRutasDeAlmacenamiento() throws Exception {
        when(service.listar(postulante, postulacion)).thenReturn(new DocumentosPostulacionResponse(true,
                List.of(new DocumentoRequeridoResponse(tipo, "Carnet", "Ambas caras", true, true,
                        List.of(adjunto()))),
                new ReglasArchivoResponse(5242880L, List.of("application/pdf"), 5)));

        mockMvc.perform(get(ruta()).with(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.puedeEditar").value(true))
                .andExpect(jsonPath("$.data.requeridos[0].tipoDocumentoCodigo").value(tipo.toString()))
                .andExpect(jsonPath("$.data.requeridos[0].adjuntado").value(true))
                .andExpect(jsonPath("$.data.requeridos[0].archivos[0].codigo").value(documento.toString()))
                .andExpect(jsonPath("$.data.requeridos[0].archivos[0].storagePath").doesNotExist())
                .andExpect(jsonPath("$.data.reglas.tamanoMaximoBytes").value(5242880))
                .andExpect(jsonPath("$.data.reglas.maximoArchivosPorTipo").value(5))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void listar_conPostulacionAjena_da404ErrDoc04() throws Exception {
        when(service.listar(postulante, postulacion)).thenThrow(
                new NotFoundException(DocumentoPostulacionErrorCodes.ERR_DOC_04, "La postulación no existe"));

        mockMvc.perform(get(ruta()).with(sesion())).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_DOC_04"));
    }

    @Test
    void listar_sinSesion_da401() throws Exception {
        mockMvc.perform(get(ruta())).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void adjuntar_da201YPasaElArchivoYElTipoAlService() throws Exception {
        when(service.adjuntar(eq(postulante), eq(postulacion), eq(tipo), any(ArchivoSubido.class)))
                .thenReturn(adjunto());

        mockMvc.perform(multipart(ruta()).file(parteArchivo()).param("tipoDocumento", tipo.toString())
                        .with(sesion()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.codigo").value(documento.toString()))
                .andExpect(jsonPath("$.data.nombreArchivo").value("carnet.pdf"));

        ArgumentCaptor<ArchivoSubido> archivo = ArgumentCaptor.forClass(ArchivoSubido.class);
        verify(service).adjuntar(eq(postulante), eq(postulacion), eq(tipo), archivo.capture());
        assertThat(archivo.getValue().nombre()).isEqualTo("carnet.pdf");
        assertThat(archivo.getValue().contentType()).isEqualTo("application/pdf");
        assertThat(archivo.getValue().contenido()).isEqualTo(PDF);
    }

    @Test
    void adjuntar_sinElArchivo_da400ErrSys05() throws Exception {
        mockMvc.perform(multipart(ruta()).param("tipoDocumento", tipo.toString()).with(sesion()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        verifyNoInteractions(service);
    }

    @Test
    void adjuntar_conFormatoInvalido_da400ConElCodigoDelService() throws Exception {
        when(service.adjuntar(any(), any(), any(), any())).thenThrow(new ValidationException(
                DocumentoPostulacionErrorCodes.ERR_DOC_03, "El formato del archivo no está permitido"));

        mockMvc.perform(multipart(ruta()).file(parteArchivo()).param("tipoDocumento", tipo.toString())
                        .with(sesion()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_DOC_03"));
    }

    @Test
    void adjuntar_conPostulacionEnviada_da409ErrDoc06() throws Exception {
        when(service.adjuntar(any(), any(), any(), any())).thenThrow(
                new ConflictException(DocumentoPostulacionErrorCodes.ERR_DOC_06, "Ya fue enviada"));

        mockMvc.perform(multipart(ruta()).file(parteArchivo()).param("tipoDocumento", tipo.toString())
                        .with(sesion()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_DOC_06"));
    }

    @Test
    void reemplazar_da200ConElDocumentoActualizado() throws Exception {
        when(service.reemplazar(eq(postulante), eq(postulacion), eq(documento), any(ArchivoSubido.class)))
                .thenReturn(adjunto());

        mockMvc.perform(multipart(HttpMethod.PUT, ruta() + "/" + documento).file(parteArchivo()).with(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.codigo").value(documento.toString()));
    }

    @Test
    void eliminar_da200ConDataNula() throws Exception {
        mockMvc.perform(delete(ruta() + "/" + documento).with(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(service).eliminar(postulante, postulacion, documento);
    }

    @Test
    void acceso_da200ConUnaUrlTemporal() throws Exception {
        when(service.acceso(postulante, postulacion, documento))
                .thenReturn(new AccesoDocumentoResponse("https://archivos.test/firmada", AHORA));

        mockMvc.perform(get(ruta() + "/" + documento + "/acceso").with(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value("https://archivos.test/firmada"))
                .andExpect(jsonPath("$.data.expiraEn").exists());
    }
}
