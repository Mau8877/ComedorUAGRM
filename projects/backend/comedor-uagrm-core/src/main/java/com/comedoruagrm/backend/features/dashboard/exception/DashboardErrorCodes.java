package com.comedoruagrm.backend.features.dashboard.exception;

/**
 * Codigos de error del dashboard de indicadores BI (CU12), prefijo DAS. Catalogo vivo en
 * docs/errors/dashboard/ERRORES_DASHBOARD.md. Un valor de `filter` que no es UUID reutiliza ERR_SYS_05.
 */
public final class DashboardErrorCodes {

    /** 404 - La gestion o la convocatoria no existe, o la convocatoria no es de la gestion. */
    public static final String ERR_DAS_01 = "ERR_DAS_01";

    /** 400 - No hay datos que exportar a PDF. */
    public static final String ERR_DAS_02 = "ERR_DAS_02";

    private DashboardErrorCodes() {
    }
}
