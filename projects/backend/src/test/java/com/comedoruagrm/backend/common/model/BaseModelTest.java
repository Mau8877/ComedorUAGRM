package com.comedoruagrm.backend.common.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.Instant;

import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;

/**
 * Fija el contrato de BaseModel con el diseño lógico de la base de datos
 * (PERSISTENCIA_BD_BACKEND.md): nombres de columna en español, borrado lógico
 * por eliminado_en y codigo como UUID v7. No necesita base de datos.
 */
class BaseModelTest {

    private static Column columnaDe(String campo) throws NoSuchFieldException {
        return BaseModel.class.getDeclaredField(campo).getAnnotation(Column.class);
    }

    @Test
    void lasColumnasComunesUsanLosNombresDelDisenoLogico() throws Exception {
        assertThat(columnaDe("id").name()).isEqualTo("id");
        assertThat(columnaDe("creadoEn").name()).isEqualTo("creado_en");
        assertThat(columnaDe("actualizadoEn").name()).isEqualTo("actualizado_en");
        assertThat(columnaDe("eliminadoEn").name()).isEqualTo("eliminado_en");
    }

    @Test
    void elCodigoEsUnUuidV7ObligatorioUnicoYNoActualizable() throws Exception {
        Field codigo = BaseModel.class.getDeclaredField("codigo");

        assertThat(codigo.getAnnotation(UuidGenerator.class).style())
                .isEqualTo(UuidGenerator.Style.VERSION_7);
        Column columna = codigo.getAnnotation(Column.class);
        assertThat(columna.nullable()).isFalse();
        assertThat(columna.unique()).isTrue();
        assertThat(columna.updatable()).isFalse();
    }

    @Test
    void elBorradoLogicoFiltraPorEliminadoEn() {
        assertThat(BaseModel.class.getAnnotation(SQLRestriction.class).value())
                .isEqualTo("eliminado_en IS NULL");
    }

    @Test
    void soloEliminadoEnTieneSetter() {
        assertThat(BaseModel.class.getDeclaredMethods())
                .extracting(m -> m.getName())
                .contains("setEliminadoEn")
                .doesNotContain("setCodigo", "setCreadoEn", "setActualizadoEn", "setId");

        class Modelo extends BaseModel { }
        Modelo m = new Modelo();
        Instant ahora = Instant.now();
        m.setEliminadoEn(ahora);
        assertThat(m.getEliminadoEn()).isEqualTo(ahora);
    }
}
