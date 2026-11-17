package com.comedoruagrm.backend.features.convocatorias.mapper;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.features.convocatorias.dto.ConvocatoriaResponse;
import com.comedoruagrm.backend.features.convocatorias.model.Convocatoria;
import com.comedoruagrm.backend.features.convocatorias.repository.ConvocatoriaDocumentoRepository;
import com.comedoruagrm.backend.features.postulaciones.service.PostulacionesDeConvocatoriaService;

import lombok.RequiredArgsConstructor;

/**
 * Arma el detalle de una convocatoria (sus documentos requeridos y si ya tiene postulaciones). No es un
 * mapeo 1:1 de campos: consulta dos fuentes, por eso es un mapper aparte que comparten los servicios del CU13.
 */
@Component
@RequiredArgsConstructor
public class ConvocatoriaMapper {

    private final ConvocatoriaDocumentoRepository documentoRepository;
    private final PostulacionesDeConvocatoriaService postulaciones;

    @Transactional(readOnly = true)
    public ConvocatoriaResponse detalle(Convocatoria convocatoria) {
        return ConvocatoriaResponse.de(convocatoria, documentoRepository.findByConvocatoriaId(convocatoria.getId()),
                postulaciones.existenPara(convocatoria.getId()));
    }
}
