package com.comedoruagrm.backend.features.cotejos.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Catalogo de errores del cotejo de asistencia (CU11): formato, unicidad y documentacion en docs/errors/. */
class CotejosErrorCodesTest {

    private static List<String> codigos() throws IllegalAccessException {
        List<String> codigos = new ArrayList<>();
        for (Field campo : CotejosErrorCodes.class.getDeclaredFields()) {
            if (Modifier.isPublic(campo.getModifiers()) && campo.getType() == String.class) {
                codigos.add((String) campo.get(null));
            }
        }
        return codigos;
    }

    @Test
    void codigos_sonCuatro_unicosYConElFormatoDelCatalogo() throws IllegalAccessException {
        List<String> codigos = codigos();

        assertThat(codigos).containsExactlyInAnyOrder("ERR_COT_01", "ERR_COT_02", "ERR_COT_03", "ERR_COT_04")
                .doesNotHaveDuplicates();
        assertThat(codigos).allMatch(c -> c.matches("ERR_COT_[0-9]{2}"));
    }

    @Test
    void codigos_estanDocumentadosYEnlazadosDesdeElIndice() throws IllegalAccessException, IOException {
        Path docs = Path.of("..", "..", "..", "docs", "errors");
        if (!Files.exists(docs.resolve("README.md"))) {
            return; // se corre fuera del repositorio completo: no hay documentacion que revisar
        }
        String catalogo = Files.readString(docs.resolve("cotejos").resolve("ERRORES_COTEJOS.md"),
                StandardCharsets.UTF_8);
        String indice = Files.readString(docs.resolve("README.md"), StandardCharsets.UTF_8);

        for (String codigo : codigos()) {
            assertThat(catalogo).contains("## `" + codigo + "`");
            assertThat(indice).contains("cotejos/ERRORES_COTEJOS.md#" + codigo.toLowerCase());
        }
    }
}
