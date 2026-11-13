package com.comedoruagrm.backend.features.actas.service;

import java.time.Instant;
import java.time.ZoneId;

import org.springframework.stereotype.Service;

import com.comedoruagrm.backend.features.actas.repository.ActasConsultaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Asigna el numero del acta (CU08): `NNN/AAAA`, correlativo que reinicia cada año, con al menos tres digitos.
 *
 * * `AAAA` es el año de la fecha de generacion en `America/La_Paz` (no en UTC).
 * * `siguiente` toma un bloqueo de transaccion por año y lee el mayor correlativo ya usado: hay que llamarlo
 * * DENTRO de la transaccion de generar, que libera el bloqueo al terminar; asi dos generaciones simultaneas de
 * * convocatorias distintas no calculan el mismo numero. La unicidad de `acta_adjudicacion.numero` es la red de
 * * seguridad.
 * * No hay secuencia persistida: si la generacion falla despues, no queda fila y el siguiente intento recalcula
 * * el mismo numero (no hay huecos). Las actas eliminadas cuentan, para no reutilizar nunca un numero.
 */
@Service
@RequiredArgsConstructor
public class NumeroActaService {

    /** Lo que se imprime en la vista previa en lugar del numero. */
    public static final String BORRADOR = "borrador";

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");

    private final ActasConsultaRepository consulta;

    /** El siguiente numero del año de `ahora`. Solo dentro de la transaccion que registra el acta. */
    public String siguiente(Instant ahora) {
        int anio = ahora.atZone(ZONA).getYear();
        consulta.bloquearNumeracion(anio);
        int correlativo = consulta.mayorCorrelativoDelAnio(anio) + 1;
        return "%03d/%d".formatted(correlativo, anio);
    }

    /** El «numero» de la vista previa: no consulta ni consume nada. */
    public String borrador() {
        return BORRADOR;
    }
}
