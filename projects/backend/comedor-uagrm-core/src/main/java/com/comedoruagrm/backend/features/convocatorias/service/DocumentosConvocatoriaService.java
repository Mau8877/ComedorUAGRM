package com.comedoruagrm.backend.features.convocatorias.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.convocatorias.dto.DocumentoRequeridoRequest;
import com.comedoruagrm.backend.features.convocatorias.exception.ConvocatoriaErrorCodes;
import com.comedoruagrm.backend.features.convocatorias.model.Convocatoria;
import com.comedoruagrm.backend.features.convocatorias.model.ConvocatoriaDocumento;
import com.comedoruagrm.backend.features.convocatorias.model.TipoDocumento;
import com.comedoruagrm.backend.features.convocatorias.repository.ConvocatoriaDocumentoRepository;
import com.comedoruagrm.backend.features.convocatorias.repository.TipoDocumentoRepository;

import lombok.RequiredArgsConstructor;

/**
 * La lista de documentos requeridos de una convocatoria (CU13): validarla y guardarla.
 *
 * * No tiene `@PreAuthorize`: lo usa `ConvocatoriaService`, que ya exige el rol.
 * ! La clave de `convocatoria_documento` incluye a las filas eliminadas: volver a agregar un tipo que se habia
 * ! quitado reactiva la fila, no inserta otra.
 */
@Service
@RequiredArgsConstructor
public class DocumentosConvocatoriaService {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentosConvocatoriaService.class);

    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final ConvocatoriaDocumentoRepository documentoRepository;

    /**
     * Valida la lista y devuelve cada tipo de documento con su `obligatorio`, en el orden recibido. Un tipo
     * repetido es 400 `ERR_CV_06`; ninguno obligatorio, 400 `ERR_CV_05`; un codigo inexistente, 404 `ERR_CV_03`.
     */
    @Transactional(readOnly = true)
    public Map<TipoDocumento, Boolean> validar(List<DocumentoRequeridoRequest> documentos) {
        Set<UUID> codigos = new HashSet<>();
        for (DocumentoRequeridoRequest documento : documentos) {
            if (!codigos.add(documento.tipoDocumentoCodigo())) {
                throw rechazar(new ValidationException(ConvocatoriaErrorCodes.ERR_CV_06,
                        "Un tipo de documento no puede repetirse en la lista"));
            }
        }
        if (documentos.stream().noneMatch(DocumentoRequeridoRequest::obligatorio)) {
            throw rechazar(new ValidationException(ConvocatoriaErrorCodes.ERR_CV_05,
                    "Indica al menos un documento obligatorio"));
        }

        Map<UUID, TipoDocumento> tipos = new LinkedHashMap<>();
        tipoDocumentoRepository.findByCodigoIn(codigos).forEach(tipo -> tipos.put(tipo.getCodigo(), tipo));
        Map<TipoDocumento, Boolean> resultado = new LinkedHashMap<>();
        for (DocumentoRequeridoRequest documento : documentos) {
            TipoDocumento tipo = tipos.get(documento.tipoDocumentoCodigo());
            if (tipo == null) {
                throw rechazar(new NotFoundException(ConvocatoriaErrorCodes.ERR_CV_03,
                        "Alguno de los tipos de documento indicados no existe"));
            }
            resultado.put(tipo, documento.obligatorio());
        }
        return resultado;
    }

    /** Si la lista nueva es distinta de la vigente de la convocatoria (otros tipos u otra obligatoriedad). */
    @Transactional(readOnly = true)
    public boolean difiere(Convocatoria convocatoria, Map<TipoDocumento, Boolean> nuevos) {
        Map<Long, Boolean> actuales = new LinkedHashMap<>();
        documentoRepository.findByConvocatoriaId(convocatoria.getId())
                .forEach(d -> actuales.put(d.getTipoDocumento().getId(), d.isObligatorio()));
        Map<Long, Boolean> pedidos = new LinkedHashMap<>();
        nuevos.forEach((tipo, obligatorio) -> pedidos.put(tipo.getId(), obligatorio));
        return !actuales.equals(pedidos);
    }

    /**
     * Deja la lista de la convocatoria igual a `nuevos`: quita (borrado logico) los tipos que ya no estan,
     * actualiza la obligatoriedad de los que siguen y agrega los nuevos, reactivando el par si habia
     * existido y se habia quitado. Devuelve si algo cambio.
     */
    @Transactional
    public boolean reemplazar(Convocatoria convocatoria, Map<TipoDocumento, Boolean> nuevos) {
        boolean cambio = false;
        Set<Long> actualesIds = new HashSet<>();
        for (ConvocatoriaDocumento actual : documentoRepository.findByConvocatoriaId(convocatoria.getId())) {
            Long tipoId = actual.getTipoDocumento().getId();
            actualesIds.add(tipoId);
            Boolean obligatorio = nuevos.entrySet().stream()
                    .filter(e -> e.getKey().getId().equals(tipoId))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
            if (obligatorio == null) {
                actual.setEliminadoEn(Instant.now());
                documentoRepository.save(actual);
                cambio = true;
            } else if (actual.isObligatorio() != obligatorio) {
                actual.setObligatorio(obligatorio);
                documentoRepository.save(actual);
                cambio = true;
            }
        }
        documentoRepository.flush();

        for (Map.Entry<TipoDocumento, Boolean> nuevo : nuevos.entrySet()) {
            TipoDocumento tipo = nuevo.getKey();
            if (actualesIds.contains(tipo.getId())) {
                continue;
            }
            int reactivados = documentoRepository.reactivar(convocatoria.getId(), tipo.getId(), nuevo.getValue());
            if (reactivados == 0) {
                documentoRepository.saveAndFlush(new ConvocatoriaDocumento(convocatoria, tipo, nuevo.getValue()));
            }
            cambio = true;
        }
        return cambio;
    }

    /** Elimina logicamente todos los documentos de una convocatoria (borrado en cascada hecho por el servicio). */
    @Transactional
    public void eliminarTodos(Convocatoria convocatoria) {
        Instant ahora = Instant.now();
        for (ConvocatoriaDocumento documento : documentoRepository.findByConvocatoriaId(convocatoria.getId())) {
            documento.setEliminadoEn(ahora);
            documentoRepository.save(documento);
        }
        documentoRepository.flush();
    }

    // WARN sin datos libres: solo el codigo de error.
    private <T extends BusinessException> T rechazar(T excepcion) {
        LOG.warn("Documentos requeridos rechazados", kv("module", "convocatorias"),
                kv("errorCode", excepcion.getErrorCode()));
        return excepcion;
    }
}
