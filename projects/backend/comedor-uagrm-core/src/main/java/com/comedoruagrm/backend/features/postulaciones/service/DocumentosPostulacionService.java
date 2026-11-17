package com.comedoruagrm.backend.features.postulaciones.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.storage.AlmacenamientoService;
import com.comedoruagrm.backend.features.postulaciones.dto.AccesoDocumentoResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.DocumentoAdjuntoResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.DocumentoRequeridoResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.DocumentosPostulacionResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.ReglasArchivoResponse;
import com.comedoruagrm.backend.features.postulaciones.enums.EstadoPostulacion;
import com.comedoruagrm.backend.features.postulaciones.exception.DocumentoPostulacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.model.DocumentoAdjunto;
import com.comedoruagrm.backend.features.postulaciones.repository.DocumentoAdjuntoRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.DocumentosPostulacionConsultaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.DocumentosPostulacionConsultaRepository.Adjunto;
import com.comedoruagrm.backend.features.postulaciones.repository.DocumentosPostulacionConsultaRepository.Cabecera;
import com.comedoruagrm.backend.features.postulaciones.repository.DocumentosPostulacionConsultaRepository.Requerido;

import lombok.RequiredArgsConstructor;

/**
 * Documentos de una postulacion (CU16): listar los que exige la convocatoria, adjuntar, reemplazar, eliminar y
 * abrir un archivo. Solo el Postulante dueño de la postulacion.
 *
 * * Se modifica solo con la postulacion en `borrador` o `devuelta` y la convocatoria `ABIERTA`; leer y abrir un
 * * archivo se puede siempre. La postulacion sale del usuario del token: una ajena responde como inexistente.
 * * El archivo se valida por su contenido ({@link ReglasArchivoDocumento}) ANTES de guardarlo. Se sube primero y
 * * se registra la fila despues: si el registro falla, el archivo recien subido se retira, asi no queda una fila
 * * sin archivo (CA13). Un archivo reemplazado o eliminado se retira del almacenamiento DESPUES de confirmar la
 * * transaccion, para no perderlo si esta se revierte.
 * * La ruta del objeto se arma con codigos (`postulaciones/{codigo}/documentos/{uuid}.{ext}`), nunca con el
 * * nombre que trae el archivo. Se bloquea la fila de la postulacion para que dos subidas simultaneas no
 * * superen el maximo por tipo.
 * ! No se loguean nombres de archivo ni rutas, y no se anota en la bitacora (no es un cambio de estado).
 */
@Service
@RequiredArgsConstructor
public class DocumentosPostulacionService {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentosPostulacionService.class);
    private static final int LARGO_MAXIMO_NOMBRE = 255;

    private final DocumentosPostulacionConsultaRepository consulta;
    private final DocumentoAdjuntoRepository documentos;
    private final AlmacenamientoService almacenamiento;
    private final ReglasArchivoDocumento reglas;
    private final Clock reloj;

    /** La lista de documentos que exige la convocatoria, cada uno con su estado y sus archivos. */
    @PreAuthorize("hasRole('POSTULANTE')")
    @Transactional(readOnly = true)
    public DocumentosPostulacionResponse listar(UUID usuarioCodigo, UUID postulacionCodigo) {
        Cabecera cabecera = cabecera(usuarioCodigo, postulacionCodigo);
        Map<Long, List<DocumentoAdjuntoResponse>> porTipo = new HashMap<>();
        for (Adjunto a : consulta.adjuntos(cabecera.postulacionId())) {
            porTipo.computeIfAbsent(a.tipoDocumentoId(), k -> new ArrayList<>()).add(new DocumentoAdjuntoResponse(
                    a.codigo(), a.nombreArchivo(), a.mimeType(), a.tamanoBytes(), a.subidoEn()));
        }
        List<DocumentoRequeridoResponse> requeridos = new ArrayList<>();
        for (Requerido r : consulta.requeridos(cabecera.convocatoriaId())) {
            List<DocumentoAdjuntoResponse> archivos = porTipo.getOrDefault(r.tipoDocumentoId(), List.of());
            requeridos.add(new DocumentoRequeridoResponse(r.tipoDocumentoCodigo(), r.nombre(), r.descripcion(),
                    r.obligatorio(), !archivos.isEmpty(), archivos));
        }
        return new DocumentosPostulacionResponse(puedeEditar(cabecera), requeridos, new ReglasArchivoResponse(
                reglas.tamanoMaximoBytes(), reglas.formatosPermitidos().stream().sorted().toList(),
                reglas.maximoArchivosPorTipo()));
    }

    /** Adjunta un archivo a un documento requerido. */
    @PreAuthorize("hasRole('POSTULANTE')")
    @Transactional
    public DocumentoAdjuntoResponse adjuntar(UUID usuarioCodigo, UUID postulacionCodigo, UUID tipoDocumentoCodigo,
            ArchivoSubido archivo) {
        Cabecera cabecera = cabecera(usuarioCodigo, postulacionCodigo);
        exigirEditable(cabecera);
        Long tipoId = consulta.tipoRequerido(cabecera.convocatoriaId(), tipoDocumentoCodigo).orElseThrow(
                () -> new NotFoundException(DocumentoPostulacionErrorCodes.ERR_DOC_05,
                        "El tipo de documento no figura en la lista de la convocatoria"));
        ReglasArchivoDocumento.Formato formato = reglas.validar(archivo);
        consulta.bloquearPostulacion(cabecera.postulacionId());
        if (documentos.countByPostulacionIdAndTipoDocumentoId(cabecera.postulacionId(), tipoId)
                >= reglas.maximoArchivosPorTipo()) {
            throw new ConflictException(DocumentoPostulacionErrorCodes.ERR_DOC_08,
                    "Ya adjuntaste el máximo de " + reglas.maximoArchivosPorTipo() + " archivos de este documento");
        }

        String ruta = rutaNueva(cabecera.postulacionCodigo(), formato);
        almacenamiento.subir(ruta, archivo.contenido(), formato.mime());
        try {
            DocumentoAdjunto d = documentos.saveAndFlush(new DocumentoAdjunto(cabecera.postulacionId(), tipoId,
                    nombreSeguro(archivo.nombre(), formato), formato.mime(), ruta, archivo.contenido().length,
                    reloj.instant()));
            LOG.info("Documento adjuntado a la postulacion", kv("module", "postulaciones"),
                    kv("userId", usuarioCodigo));
            return aRespuesta(d);
        } catch (RuntimeException e) {
            retirarSinFallar(ruta);
            throw e;
        }
    }

    /** Cambia el archivo de un documento ya adjuntado; el nuevo reemplaza al anterior en la misma fila. */
    @PreAuthorize("hasRole('POSTULANTE')")
    @Transactional
    public DocumentoAdjuntoResponse reemplazar(UUID usuarioCodigo, UUID postulacionCodigo, UUID documentoCodigo,
            ArchivoSubido archivo) {
        Cabecera cabecera = cabecera(usuarioCodigo, postulacionCodigo);
        exigirEditable(cabecera);
        ReglasArchivoDocumento.Formato formato = reglas.validar(archivo);
        consulta.bloquearPostulacion(cabecera.postulacionId());
        DocumentoAdjunto d = documento(cabecera, documentoCodigo);

        String anterior = d.getStoragePath();
        String ruta = rutaNueva(cabecera.postulacionCodigo(), formato);
        almacenamiento.subir(ruta, archivo.contenido(), formato.mime());
        try {
            d.reemplazarArchivo(nombreSeguro(archivo.nombre(), formato), formato.mime(), ruta,
                    archivo.contenido().length, reloj.instant());
            documentos.saveAndFlush(d);
        } catch (RuntimeException e) {
            retirarSinFallar(ruta);
            throw e;
        }
        LOG.info("Documento de la postulacion reemplazado", kv("module", "postulaciones"),
                kv("userId", usuarioCodigo));
        retirarTrasConfirmar(anterior);
        return aRespuesta(d);
    }

    /** Elimina un documento adjuntado (borrado logico) y retira su archivo. */
    @PreAuthorize("hasRole('POSTULANTE')")
    @Transactional
    public void eliminar(UUID usuarioCodigo, UUID postulacionCodigo, UUID documentoCodigo) {
        Cabecera cabecera = cabecera(usuarioCodigo, postulacionCodigo);
        exigirEditable(cabecera);
        consulta.bloquearPostulacion(cabecera.postulacionId());
        DocumentoAdjunto d = documento(cabecera, documentoCodigo);

        d.setEliminadoEn(reloj.instant());
        documentos.saveAndFlush(d);
        LOG.info("Documento de la postulacion eliminado", kv("module", "postulaciones"),
                kv("userId", usuarioCodigo));
        retirarTrasConfirmar(d.getStoragePath());
    }

    /** Una direccion temporal firmada para abrir el archivo; no consulta el almacenamiento. */
    @PreAuthorize("hasRole('POSTULANTE')")
    @Transactional(readOnly = true)
    public AccesoDocumentoResponse acceso(UUID usuarioCodigo, UUID postulacionCodigo, UUID documentoCodigo) {
        Cabecera cabecera = cabecera(usuarioCodigo, postulacionCodigo);
        DocumentoAdjunto d = documento(cabecera, documentoCodigo);
        String url = almacenamiento.urlTemporal(d.getStoragePath(), AlmacenamientoService.VIGENCIA_ACCESO);
        return new AccesoDocumentoResponse(url, reloj.instant().plus(AlmacenamientoService.VIGENCIA_ACCESO));
    }

    private Cabecera cabecera(UUID usuarioCodigo, UUID postulacionCodigo) {
        return consulta.cabecera(postulacionCodigo, usuarioCodigo).orElseThrow(() -> new NotFoundException(
                DocumentoPostulacionErrorCodes.ERR_DOC_04, "La postulación no existe"));
    }

    private DocumentoAdjunto documento(Cabecera cabecera, UUID documentoCodigo) {
        return documentos.findByCodigoAndPostulacionId(documentoCodigo, cabecera.postulacionId()).orElseThrow(
                () -> new NotFoundException(DocumentoPostulacionErrorCodes.ERR_DOC_09, "El documento no existe"));
    }

    private boolean puedeEditar(Cabecera c) {
        return estadoEditable(c) && convocatoriaAbierta(c);
    }

    private static boolean estadoEditable(Cabecera c) {
        return EstadoPostulacion.borrador.name().equals(c.estado())
                || EstadoPostulacion.devuelta.name().equals(c.estado());
    }

    private boolean convocatoriaAbierta(Cabecera c) {
        return "ABIERTA".equals(c.convocatoriaEstado()) && !reloj.instant().isAfter(c.convocatoriaCierre());
    }

    private void exigirEditable(Cabecera c) {
        if (!estadoEditable(c)) {
            throw new ConflictException(DocumentoPostulacionErrorCodes.ERR_DOC_06,
                    "La postulación ya fue enviada: sus documentos no se pueden modificar");
        }
        if (!convocatoriaAbierta(c)) {
            throw new ConflictException(DocumentoPostulacionErrorCodes.ERR_DOC_07,
                    "La convocatoria ya no está abierta");
        }
    }

    private static String rutaNueva(UUID postulacionCodigo, ReglasArchivoDocumento.Formato formato) {
        return "postulaciones/" + postulacionCodigo + "/documentos/" + UUID.randomUUID() + "." + formato.extension();
    }

    /** El nombre para mostrar: sin carpetas ni caracteres de control, de hasta 255 caracteres. */
    static String nombreSeguro(String nombre, ReglasArchivoDocumento.Formato formato) {
        String limpio = nombre == null ? "" : nombre.replace('\\', '/');
        limpio = limpio.substring(limpio.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").trim();
        if (limpio.isEmpty()) {
            limpio = "documento." + formato.extension();
        }
        return limpio.length() > LARGO_MAXIMO_NOMBRE ? limpio.substring(0, LARGO_MAXIMO_NOMBRE) : limpio;
    }

    private static DocumentoAdjuntoResponse aRespuesta(DocumentoAdjunto d) {
        return new DocumentoAdjuntoResponse(d.getCodigo(), d.getNombreArchivo(), d.getMimeType(),
                d.getTamanoBytes(), d.getSubidoEn());
    }

    /** Retira un archivo ya subido cuando fallo el registro; un fallo del almacenamiento solo se loguea. */
    private void retirarSinFallar(String ruta) {
        try {
            almacenamiento.eliminar(ruta);
        } catch (RuntimeException e) {
            LOG.warn("No se pudo retirar un archivo huerfano", kv("module", "postulaciones"));
        }
    }

    /** Retira un archivo cuando la transaccion se confirma (o ya, si no hay transaccion). */
    private void retirarTrasConfirmar(String ruta) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            retirarSinFallar(ruta);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                retirarSinFallar(ruta);
            }
        });
    }
}
