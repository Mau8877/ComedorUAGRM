package com.comedoruagrm.backend.features.preseleccion.constant;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Las variables del modelo de preseleccion (CU26): la clave de `parametro_modelo.clave` y su nombre para mostrar.
 *
 * * Las seis primeras son las del mockup del CU26 (las respuestas del formulario socioeconomico del CU02); las
 * * otras cuatro salen del formulario oficial de la DUBSS. La carrera tambien viaja a la IA pero no tiene
 * * ponderacion. Las ponderaciones de cada una suman 100 (puntos maximos).
 * * El modelo `v1.0` que siembra la migracion `sembrar_modelo_preseleccion_v1_0` y el modelo por defecto del
 * * modulo de IA llevan estas mismas diez claves: si cambian, cambian en los tres lugares.
 * ? Que variables puntuan y con cuanto peso lo define la DUBSS en el CU26.
 */
public final class VariablesModelo {

    public static final String INGRESOS_FAMILIARES = "ingresos_familiares";
    public static final String DEPENDIENTES = "dependientes";
    public static final String PROCEDENCIA = "procedencia";
    public static final String PATRIMONIO = "patrimonio";
    public static final String ESTADO_VULNERABILIDAD = "estado_vulnerabilidad";
    public static final String PROMEDIO_ACADEMICO = "promedio_academico";
    // Del formulario socioeconomico oficial de la DUBSS (FORM-DUBS-001), guardados en `datos_extra`.
    public static final String DEPENDENCIA_ECONOMICA = "dependencia_economica";
    public static final String OCUPACION_SOSTEN = "ocupacion_sosten";
    public static final String DISCAPACIDAD = "discapacidad";
    public static final String HIJOS = "hijos";

    /** Clave → nombre, en el orden del mockup. */
    public static final Map<String, String> NOMBRES = nombres();

    /** Lo que deben sumar las ponderaciones (por definir con la DUBSS si cambia). */
    public static final int SUMA_PONDERACIONES = 100;

    private VariablesModelo() {
    }

    private static Map<String, String> nombres() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(INGRESOS_FAMILIARES, "Ingresos familiares");
        m.put(DEPENDIENTES, "Dependientes");
        m.put(PROCEDENCIA, "Procedencia");
        m.put(PATRIMONIO, "Patrimonio");
        m.put(ESTADO_VULNERABILIDAD, "Estado de vulnerabilidad");
        m.put(PROMEDIO_ACADEMICO, "Promedio académico");
        m.put(DEPENDENCIA_ECONOMICA, "Dependencia económica");
        m.put(OCUPACION_SOSTEN, "Ocupación de quien sostiene");
        m.put(DISCAPACIDAD, "Discapacidad");
        m.put(HIJOS, "Hijos");
        return Collections.unmodifiableMap(m);
    }
}
