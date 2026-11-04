package com.comedoruagrm.backend.features.usuarios.constant;

/**
 * Valores de `rol.nombre`. Son constantes de texto y no un enum Java porque
 * los roles son filas de la tabla `rol` (ver SEGURIDAD_AUTH_BACKEND.md).
 */
public final class RolNombres {

    public static final String POSTULANTE = "POSTULANTE";
    public static final String ADMINISTRADOR_DUBSS = "ADMINISTRADOR_DUBSS";
    public static final String DIRECTIVO = "DIRECTIVO";
    public static final String PERSONAL_COMEDOR = "PERSONAL_COMEDOR";

    private RolNombres() {
    }
}
