package com.comedoruagrm.backend.features.postulaciones.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * El detalle de una postulacion propia (CU04): el resumen, la linea de tiempo y la informacion asociada al
 * estado visible. Los bloques que no corresponden al estado vienen en `null`.
 *
 * * `observaciones`: solo si esta «devuelta». `motivo`: solo el del archivo o rechazo del CU20, nunca el de la
 * * decision final del CU05. `finalista`: solo si es finalista. `resultado` (`ADJUDICADA` o `NO_ADJUDICADA`):
 * * solo con los resultados publicados. `puedeCorregir`: solo si esta «devuelta».
 * ! Ningun campo lleva ids internos, CI, correo ni quien hizo un cambio.
 */
public record MiPostulacionDetalleResponse(
        UUID codigo,
        ConvocatoriaResumenResponse convocatoria,
        GestionAcademicaResumenResponse gestionAcademica,
        Instant fechaEnvio,
        String estado,
        short numeroEnvios,
        List<PasoLineaTiempoResponse> lineaDeTiempo,
        ObservacionesResponse observaciones,
        String motivo,
        FinalistaPostulanteResponse finalista,
        String resultado,
        boolean puedeCorregir) {
}
