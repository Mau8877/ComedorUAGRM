package com.comedoruagrm.backend.features.postulaciones.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import com.comedoruagrm.backend.features.convocatorias.dto.ConvocatoriaResponse;
import com.comedoruagrm.backend.features.convocatorias.dto.ConvocatoriaResumenResponse;
import com.comedoruagrm.backend.features.convocatorias.model.Convocatoria;

/**
 * `convocatoria.cantidad_finalistas` es un dato solo del Administrador (CU05): ningun DTO de consulta del
 * postulante ni publico la entrega, y ninguno expone la entidad `Convocatoria` completa. Solo
 * `PanelAdjudicacionResponse` la lleva, mas las dos respuestas de la gestion de convocatorias del CU13
 * (`ConvocatoriaResponse` y `ConvocatoriaResumenResponse`), que solo salen por endpoints del Administrador DUBSS.
 * Cuando exista un endpoint de convocatorias para el postulante (CU02), su propio CU repite esta comprobacion.
 */
class CantidadFinalistasPrivadaTest {

    private static final String PAQUETE = "com.comedoruagrm.backend";

    /** Las respuestas que pueden llevarla: todas salen solo por endpoints del Administrador DUBSS. */
    private static final List<Class<?>> SOLO_ADMINISTRADOR = List.of(PanelAdjudicacionResponse.class,
            ConvocatoriaResponse.class, ConvocatoriaResumenResponse.class);

    private List<Class<?>> respuestas() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider escaner = new ClassPathScanningCandidateComponentProvider(false);
        escaner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*Response(\\$.*)?")));
        List<Class<?>> clases = new ArrayList<>();
        for (var definicion : escaner.findCandidateComponents(PAQUETE)) {
            clases.add(Class.forName(definicion.getBeanClassName()));
        }
        return clases;
    }

    @Test
    void soloLasRespuestasDelAdministradorLlevanLaCantidadDeFinalistas() throws ClassNotFoundException {
        List<Class<?>> clases = respuestas();
        assertThat(clases).isNotEmpty().contains(PanelAdjudicacionResponse.class, ConvocatoriaPriorizableResponse.class);

        for (Class<?> clase : clases) {
            boolean lleva = false;
            for (var campo : clase.getDeclaredFields()) {
                String nombre = campo.getName().toLowerCase();
                if (nombre.contains("cantidadfinalistas") || nombre.contains("cantidad_finalistas")) {
                    lleva = true;
                }
            }
            if (SOLO_ADMINISTRADOR.contains(clase)) {
                assertThat(lleva).as(clase.getSimpleName() + " (de Administrador) debe llevarla").isTrue();
            } else {
                assertThat(lleva).as(clase.getSimpleName() + " no debe llevar la cantidad de finalistas").isFalse();
            }
        }
    }

    @Test
    void ningunaRespuestaExponeLaEntidadConvocatoria() throws ClassNotFoundException {
        for (Class<?> clase : respuestas()) {
            for (var campo : clase.getDeclaredFields()) {
                assertThat(campo.getType()).as(clase.getSimpleName() + "." + campo.getName())
                        .isNotEqualTo(Convocatoria.class);
            }
        }
    }
}
