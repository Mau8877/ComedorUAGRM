package com.comedoruagrm.backend.features.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.dashboard.dto.BiResponse;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse.GestionResumen;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse.IndicadorMensual;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse.Totales;
import com.comedoruagrm.backend.features.dashboard.service.BiExcelService.ExcelGenerado;
import com.comedoruagrm.backend.features.usuarios.model.Rol;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

/** El Excel del dashboard BI (CU12): se genera de verdad con Apache POI y se vuelve a leer (sin base de datos). */
class BiExcelServiceTest {

    private final IndicadoresService indicadores = mock(IndicadoresService.class);
    private final BiService bi = mock(BiService.class);
    private final UsuarioService usuarios = mock(UsuarioService.class);
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-13T15:30:00Z"), ZoneOffset.UTC);
    private final BiExcelService servicio = new BiExcelService(indicadores, bi, usuarios, reloj);

    private final UUID actor = UUID.randomUUID();
    private final UUID gestion = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        Usuario directivo = new Usuario(mock(Rol.class), "0000002", "dir@uagrm.test", "hash", "Dña. Ñusta",
                "Peña Añez");
        when(usuarios.obtenerPorCodigo(actor)).thenReturn(directivo);
        when(bi.obtener(any(), any())).thenReturn(IndicadoresPdfServiceTest.analisis());
    }

    private IndicadoresResponse datos(boolean sinDatos) {
        return new IndicadoresResponse(new GestionResumen(gestion, "2026-II"), null,
                new Totales(120, 30, 12, 75.5),
                List.of(new IndicadorMensual("2026-09", 80, 0, 0, null), new IndicadorMensual("2026-10", 40, 30, 12,
                        75.5)),
                Instant.parse("2026-11-13T06:00:00Z"), sinDatos);
    }

    private static Workbook leer(byte[] contenido) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(contenido));
    }

    @Test
    void generar_creaUnaHojaPorBloque_conLosValoresDeLaPantalla() throws IOException {
        when(indicadores.obtener(gestion, null)).thenReturn(datos(false));

        ExcelGenerado excel = servicio.generar(actor, gestion, null);

        assertThat(excel.nombreArchivo()).isEqualTo("bi-2026-ii-20261113.xlsx");
        try (Workbook libro = leer(excel.contenido())) {
            assertThat(List.of(0, 1, 2, 3, 4, 5, 6, 7)).extracting(libro::getSheetName).containsExactly("Resumen",
                    "Por mes", "Por facultad", "Por carrera", "Convocatorias", "Puntajes", "Asistencia", "Tendencia");
            Sheet resumen = libro.getSheet("Resumen");
            assertThat(resumen.getRow(0).getCell(1).getStringCellValue()).isEqualTo("2026-II");
            assertThat(resumen.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Todas las convocatorias");
            assertThat(resumen.getRow(4).getCell(1).getStringCellValue()).isEqualTo("Dña. Ñusta Peña Añez");

            Sheet facultades = libro.getSheet("Por facultad");
            assertThat(facultades.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Facultad de Tecnología");
            assertThat(facultades.getRow(1).getCell(1).getNumericCellValue()).isEqualTo(80.0);
            assertThat(facultades.getRow(2).getCell(0).getStringCellValue()).isEqualTo("Facultad de Humanidades");

            Sheet meses = libro.getSheet("Por mes");
            assertThat(meses.getRow(2).getCell(4).getNumericCellValue()).isEqualTo(75.5);
            // Sin asistencia ese mes la celda queda vacia, no en cero.
            assertThat(meses.getRow(1).getCell(4)).isNull();

            Sheet convocatorias = libro.getSheet("Convocatorias");
            assertThat(convocatorias.getRow(2).getCell(3).getNumericCellValue()).isEqualTo(40.0);
            assertThat(convocatorias.getRow(1).getCell(3)).isNull();
        }
    }

    @Test
    void generar_sinDatos_noHayArchivo_errDas02() {
        when(indicadores.obtener(gestion, null)).thenReturn(datos(true));

        assertThatThrownBy(() -> servicio.generar(actor, gestion, null)).isInstanceOf(ValidationException.class)
                .hasMessageContaining("No hay datos que exportar");
    }

    @Test
    void nombreArchivo_quitaTildesYEspacios() {
        assertThat(BiExcelService.nombreArchivo("Gestión 2026 II", Instant.parse("2026-11-13T15:30:00Z")))
                .isEqualTo("bi-gestion-2026-ii-20261113.xlsx");
        assertThat(BiExcelService.nombreArchivo("???", Instant.parse("2026-11-13T15:30:00Z")))
                .isEqualTo("bi-gestion-20261113.xlsx");
    }

    @Test
    void generar_exigeRolDirectivo_yLeeEnUnaTransaccionDeSoloLectura() throws NoSuchMethodException {
        Method m = BiExcelService.class.getMethod("generar", UUID.class, UUID.class, UUID.class);

        assertThat(m.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('DIRECTIVO')");
        assertThat(m.getAnnotation(Transactional.class).readOnly()).isTrue();
        assertThat(m.getAnnotations()).noneMatch(a -> a.annotationType().getSimpleName().equals("Auditar"));
        assertThat(BiResponse.class).isNotNull();
    }
}
