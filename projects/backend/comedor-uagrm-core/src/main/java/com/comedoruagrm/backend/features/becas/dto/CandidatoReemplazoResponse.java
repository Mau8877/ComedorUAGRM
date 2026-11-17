package com.comedoruagrm.backend.features.becas.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.comedoruagrm.backend.features.becas.repository.PadronConsultaRepository.Candidato;

/** El postulante que se propone para ocupar un cupo liberado (CU23, paso 13). `posicion` es la de la lista priorizada. */
public record CandidatoReemplazoResponse(UUID postulanteCodigo, String nombres, String apellidos, String ci,
        String carrera, int posicion, BigDecimal puntajeTotal) {

    public static CandidatoReemplazoResponse de(Candidato c) {
        return new CandidatoReemplazoResponse(c.postulanteCodigo(), c.nombres(), c.apellidos(), c.ci(),
                c.carrera(), c.posicion(), c.puntajeTotal());
    }
}
