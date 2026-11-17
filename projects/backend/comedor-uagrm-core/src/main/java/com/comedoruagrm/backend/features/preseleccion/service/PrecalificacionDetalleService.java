package com.comedoruagrm.backend.features.preseleccion.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.features.preseleccion.dto.AporteVariableResponse;
import com.comedoruagrm.backend.features.preseleccion.dto.PasoTrazaResponse;
import com.comedoruagrm.backend.features.preseleccion.dto.PrecalificacionResponse;
import com.comedoruagrm.backend.features.preseleccion.exception.ModeloPreseleccionErrorCodes;
import com.comedoruagrm.backend.features.preseleccion.repository.PrecalificacionConsultaRepository;
import com.comedoruagrm.backend.features.preseleccion.repository.PrecalificacionConsultaRepository.CabeceraPrecalificacion;
import com.comedoruagrm.backend.features.preseleccion.repository.PrecalificacionConsultaRepository.CambioEstado;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Consulta de la precalificacion con IA de una postulacion (CU03): el puntaje vigente, cuanto aporto cada variable y
 * la traza del proceso. Solo el Administrador DUBSS. Es una lectura: no cambia nada ni se anota en la bitacora.
 *
 * * El maximo de cada variable se calcula como lo hace la IA: su ponderacion sobre la suma de las ponderaciones de
 * * las variables que si tienen dato, llevada a la escala del puntaje (0 a 70).
 * * Del `detalle` guardado se lee solo lo que se muestra; un JSON que no se puede leer se trata como «sin desglose»
 * * y no tumba la pantalla.
 */
@Service
@RequiredArgsConstructor
public class PrecalificacionDetalleService {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final BigDecimal MAXIMO_POR_DEFECTO = new BigDecimal("70");

    private static final Map<String, String> PASOS = Map.of(
            "pendiente", "Postulación enviada",
            "calificada", "Precalificación automática",
            "devuelta", "Devuelta para corrección",
            "archivada", "Archivada",
            "adjudicada", "Adjudicada",
            "no_adjudicada", "No adjudicada");

    private final PrecalificacionConsultaRepository consulta;

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public PrecalificacionResponse obtener(UUID postulacionCodigo) {
        CabeceraPrecalificacion c = consulta.cabecera(postulacionCodigo).orElseThrow(
                () -> new NotFoundException(ModeloPreseleccionErrorCodes.ERR_MOD_08, "La postulación no existe"));

        List<AporteVariableResponse> variables = new ArrayList<>();
        List<String> advertencias = new ArrayList<>();
        BigDecimal maximo = MAXIMO_POR_DEFECTO;
        leerDetalle(c.detalle(), variables, advertencias);
        JsonNode detalle = leer(c.detalle());
        if (detalle != null && detalle.path("puntaje_maximo").isNumber()) {
            maximo = detalle.path("puntaje_maximo").decimalValue();
        }
        List<AporteVariableResponse> conMaximo = conMaximo(variables, maximo);

        Long posicion = "calificada".equals(c.estado()) ? consulta.posicion(c.id()).orElse(null) : null;
        return new PrecalificacionResponse(c.codigo(), c.nombres(), c.apellidos(), c.ci(), c.convocatoria(),
                c.estado(), c.puntaje(), c.puntaje() == null ? null : maximo, c.modelo(), c.fechaCalculo(), posicion,
                conMaximo, advertencias, c.revisionDesde() != null, c.revisionDesde(), c.intentos(),
                traza(c, posicion));
    }

    private static JsonNode leer(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readTree(json);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void leerDetalle(String json, List<AporteVariableResponse> variables, List<String> advertencias) {
        JsonNode detalle = leer(json);
        if (detalle == null) {
            return;
        }
        detalle.path("variables").properties().forEach(e -> {
            JsonNode v = e.getValue();
            variables.add(new AporteVariableResponse(e.getKey(), decimal(v.path("nivel")),
                    decimal(v.path("ponderacion")), decimal(v.path("puntos")), null));
        });
        detalle.path("advertencias").forEach(a -> advertencias.add(a.asString()));
    }

    private static BigDecimal decimal(JsonNode nodo) {
        return nodo.isNumber() ? nodo.decimalValue() : null;
    }

    private static List<AporteVariableResponse> conMaximo(List<AporteVariableResponse> variables,
            BigDecimal puntajeMaximo) {
        BigDecimal suma = variables.stream().filter(v -> v.nivel() != null && v.ponderacion() != null)
                .map(AporteVariableResponse::ponderacion).reduce(BigDecimal.ZERO, BigDecimal::add);
        return variables.stream().map(v -> new AporteVariableResponse(v.clave(), v.nivel(), v.ponderacion(),
                v.puntos(), v.nivel() == null || v.ponderacion() == null || suma.signum() <= 0 ? null
                        : v.ponderacion().multiply(puntajeMaximo).divide(suma, 2, RoundingMode.HALF_UP))).toList();
    }

    private List<PasoTrazaResponse> traza(CabeceraPrecalificacion c, Long posicion) {
        List<PasoTrazaResponse> pasos = new ArrayList<>();
        for (CambioEstado cambio : consulta.historial(c.id())) {
            String paso = PASOS.getOrDefault(cambio.estadoNuevo(), cambio.estadoNuevo());
            // El motivo que repite el nombre del paso no aporta nada.
            String motivo = paso.equals(cambio.motivo()) ? null : cambio.motivo();
            pasos.add(new PasoTrazaResponse(paso, motivo, cambio.fecha()));
        }
        if (c.revisionDesde() != null) {
            pasos.add(new PasoTrazaResponse("Marcada para revisión",
                    "La IA rechazó los datos " + c.intentos() + " veces; ya no se reintenta sola",
                    c.revisionDesde()));
        }
        if (posicion != null) {
            pasos.add(new PasoTrazaResponse("Ubicación en la lista priorizada", "Posición " + posicion, null));
        }
        return pasos;
    }
}
