package com.comedoruagrm.backend.features.planillas.dto;

import java.time.LocalDate;

/**
 * Lo que la pantalla necesita antes de generar (CU09): cuantos becados vigentes entrarian en la planilla y los
 * limites del periodo (la gestion academica de la convocatoria).
 */
public record ResumenPlanillaResponse(
        long becadosVigentes,
        String gestion,
        LocalDate gestionDesde,
        LocalDate gestionHasta) {
}
