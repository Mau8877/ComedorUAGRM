package com.comedoruagrm.backend.features.cotejos.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.features.cotejos.dto.CotejoResponse;
import com.comedoruagrm.backend.features.cotejos.dto.DatosCotejoResponse;
import com.comedoruagrm.backend.features.cotejos.dto.RegistroDigitalResponse;
import com.comedoruagrm.backend.features.cotejos.exception.CotejosErrorCodes;
import com.comedoruagrm.backend.features.cotejos.repository.CotejosConsultaRepository;
import com.comedoruagrm.backend.features.cotejos.repository.CotejosConsultaRepository.DatosPlanilla;
import com.comedoruagrm.backend.features.cotejos.repository.CotejosConsultaRepository.ResumenDigital;

import lombok.RequiredArgsConstructor;

/**
 * Lo que el Administrador DUBSS consulta para cotejar una planilla (CU11): los datos de la planilla con el conteo de
 * registros digitales de su periodo, la lista de esos registros y los cotejos ya registrados. Solo lectura; nada de
 * esto se anota en la bitacora.
 *
 * * Una planilla sin registros digitales no es un error (A1): `registrosDigitales` vale 0 y la pantalla lo informa.
 * ! Las respuestas no llevan el CI ni ids internos.
 */
@Service
@RequiredArgsConstructor
public class ConsultaCotejosService {

    private final CotejosConsultaRepository consulta;

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public DatosCotejoResponse datos(UUID planillaCodigo) {
        DatosPlanilla p = exigirPlanilla(planillaCodigo);
        ResumenDigital resumen = consulta.resumenDigital(p.codigo());
        return new DatosCotejoResponse(p.codigo(), p.convocatoria(), p.convocatoriaNombre(), p.fechaDesde(),
                p.fechaHasta(), p.totalBecados(), p.generadaEn(), resumen.registros(), resumen.becados());
    }

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public Page<RegistroDigitalResponse> registros(UUID planillaCodigo, Pageable pageable) {
        return consulta.registros(exigirPlanilla(planillaCodigo).codigo(), pageable);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public Page<CotejoResponse> cotejos(UUID planillaCodigo, Pageable pageable) {
        return consulta.cotejos(exigirPlanilla(planillaCodigo).codigo(), pageable);
    }

    private DatosPlanilla exigirPlanilla(UUID codigo) {
        return consulta.planilla(codigo)
                .orElseThrow(() -> new NotFoundException(CotejosErrorCodes.ERR_COT_01, "La planilla no existe"));
    }
}
