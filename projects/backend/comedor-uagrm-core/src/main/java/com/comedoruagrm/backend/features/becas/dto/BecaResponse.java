package com.comedoruagrm.backend.features.becas.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.comedoruagrm.backend.features.becas.repository.PadronConsultaRepository.FilaBeca;

/**
 * Una beca del padron (CU23) con los datos del becado. `becado.codigo` es el codigo publico del postulante;
 * nunca se expone un id interno. `fechaFin` es nula mientras ningun CU la defina.
 */
public record BecaResponse(UUID codigo, BecadoResponse becado, String estado, LocalDate fechaInicio,
        LocalDate fechaFin) {

    public record BecadoResponse(UUID codigo, String nombres, String apellidos, String ci, String carrera) {
    }

    public static BecaResponse de(FilaBeca fila) {
        return new BecaResponse(fila.codigo(), new BecadoResponse(fila.becadoCodigo(), fila.nombres(),
                fila.apellidos(), fila.ci(), fila.carrera()), fila.estado(), fila.fechaInicio(), fila.fechaFin());
    }
}
