package com.comedoruagrm.backend.features.postulaciones.service;

import java.util.List;

/**
 * Opciones del formulario socioeconomico (CU02) que se muestran como lista desplegable y que el servicio acepta.
 *
 * * Los valores son los del mockup del documento de grado. El catalogo definitivo esta POR DEFINIR con la DUBSS
 * * (el diseño logico lo marca asi): cuando lo fijen, se cambia SOLO esta clase; el web y el movil las leen del
 * * backend, no las repiten.
 * * Los topes numericos son de las columnas de `respuesta_socioeconomica`.
 */
public final class OpcionesSocioeconomicas {

    public static final List<String> PROCEDENCIAS = List.of("Capital", "Provincia", "Otro departamento",
            "Área rural");
    public static final List<String> PATRIMONIOS = List.of("Sin vivienda propia", "Vivienda propia",
            "Vivienda alquilada", "Vivienda en anticrético");
    public static final List<String> ESTADOS_VULNERABILIDAD = List.of("Sin situación especial",
            "Hogar monoparental", "Persona con discapacidad en el hogar", "Ingresos irregulares",
            "Dependiente de otro familiar");

    // * Las del formulario oficial de la DUBSS (FORM-DUBS-001, Becas IDH 2026), en el mismo texto que el papel.
    public static final List<String> DEPENDENCIAS = List.of("Independiente", "Solo de padre o madre",
            "Otro familiar", "De la pareja", "Ambos padres");
    public static final List<String> OCUPACIONES_SOSTEN = List.of("Asalariado formal", "Asalariado informal",
            "Comerciante mayorista", "Rentista", "Comerciante minorista", "Agricultor");
    public static final List<String> RANGOS_INGRESOS = List.of("Menor de Bs. 2500", "De Bs. 2501 a Bs. 4000",
            "De Bs. 4001 a Bs. 6000", "Más de Bs. 6000");
    public static final List<String> CANTIDADES_HIJOS = List.of("1", "Más de 1");
    public static final List<String> TENENCIAS_VIVIENDA = List.of("Herencia", "De los padres", "Cedida",
            "Anticrético", "Alquiler");

    public static final int GRUPO_FAMILIAR_MAXIMO = 10;
    public static final int EDAD_MAXIMA = 120;
    public static final int AMBIENTES_MAXIMO = 20;
    public static final int TEXTO_CORTO_MAXIMO = 100;
    public static final int TEXTO_DETALLE_MAXIMO = 150;
    public static final int COMENTARIO_MAXIMO = 500;

    /** `numeric(12,2)`: hasta 10 enteros. */
    public static final long INGRESOS_MAXIMO = 9_999_999_999L;
    public static final int DEPENDIENTES_MAXIMO = 50;
    /** La escala del promedio esta por definir (el diseño solo dice 0-100). */
    public static final int PROMEDIO_MAXIMO = 100;

    private OpcionesSocioeconomicas() {
    }
}
