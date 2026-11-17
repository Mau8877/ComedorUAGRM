package com.comedoruagrm.backend.features.mibeca.exception;

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

/** Catalogo de errores de Mi beca (CU24): formato, unicidad y documentacion en docs/errors/. */
class MiBecaErrorCodesTest {

    private static List<String> codigos() throws IllegalAccessException {
        List<String> codigos = new ArrayList<>();
        for (Field campo : MiBecaErrorCodes.class.getDeclaredFields()) {
            if (Modifier.isPublic(campo.getModifiers()) && campo.getType() == String.class) {
                codigos.add((String) campo.get(null));
            }
        }
        return codigos;
    }

    @Test
    void codigos_sonTres_unicosYConElFormatoDelCatalogo() throws IllegalAccessException {
        List<String> codigos = codigos();

        assertThat(codigos).containsExactlyInAnyOrder("ERR_MBE_01", "ERR_MBE_02", "ERR_MBE_03")
                .doesNotHaveDuplicates();
        assertThat(codigos).allMatch(c -> c.matches("ERR_MBE_[0-9]{2}"));
    }

    @Test
    void codigos_estanDocumentadosYEnlazadosDesdeElIndice() throws IllegalAccessException, IOException {
        Path docs = Path.of("..", "..", "..", "docs", "errors");
        if (!Files.exists(docs.resolve("README.md"))) {
            return; // se corre fuera del repositorio completo: no hay documentacion que revisar
        }
        String catalogo = Files.readString(docs.resolve("mibeca").resolve("ERRORES_MIBECA.md"),
                StandardCharsets.UTF_8);
        String indice = Files.readString(docs.resolve("README.md"), StandardCharsets.UTF_8);

        for (String codigo : codigos()) {
            assertThat(catalogo).contains("## `" + codigo + "`");
            assertThat(indice).contains("mibeca/ERRORES_MIBECA.md#" + codigo.toLowerCase());
        }
    }
}
