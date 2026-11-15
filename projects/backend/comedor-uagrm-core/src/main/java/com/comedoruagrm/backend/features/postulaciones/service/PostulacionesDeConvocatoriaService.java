package com.comedoruagrm.backend.features.postulaciones.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.features.postulaciones.repository.PostulacionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Consulta de postulaciones por convocatoria para otros modulos.
 *
 * * El modulo `convocatorias` (CU13) no usa el repositorio de `postulaciones` directo: pasa por este servicio
 * * (regla de dependencia de ARQUITECTURA_BACKEND.md). No tiene `@PreAuthorize`: lo llaman otros servicios, que
 * * son los que comprueban el rol.
 */
@Service
@RequiredArgsConstructor
public class PostulacionesDeConvocatoriaService {

    private final PostulacionRepository postulacionRepository;

    /** Si la convocatoria (por su `id` interno) ya recibio alguna postulacion. */
    @Transactional(readOnly = true)
    public boolean existenPara(Long convocatoriaId) {
        return postulacionRepository.existsByConvocatoriaId(convocatoriaId);
    }
}
