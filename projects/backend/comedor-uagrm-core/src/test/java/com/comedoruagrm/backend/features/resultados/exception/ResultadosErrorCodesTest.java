package com.comedoruagrm.backend.features.resultados.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Catalogo de errores de la publicacion de resultados (CU22): formato y unicidad de los codigos. */
class ResultadosErrorCodesTest {

    @Test
    void codigos_sonCuatro_unicosYConElFormatoDelCatalogo() throws IllegalAccessException {
        List<String> codigos = new ArrayList<>();
        for (Field campo : ResultadosErrorCodes.class.getDeclaredFields()) {
            if (Modifier.isPublic(campo.getModifiers()) && campo.getType() == String.class) {
                codigos.add((String) campo.get(null));
            }
        }

        assertThat(codigos).containsExactlyInAnyOrder("ERR_PUB_01", "ERR_PUB_02", "ERR_PUB_03", "ERR_PUB_04")
                .doesNotHaveDuplicates();
        assertThat(codigos).allMatch(c -> c.matches("ERR_PUB_\\d{2}"));
    }
}
