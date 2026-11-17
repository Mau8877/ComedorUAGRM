package com.comedoruagrm.backend.features.postulaciones.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.comedoruagrm.backend.features.postulaciones.dto.DatosComplementarios;
import com.comedoruagrm.backend.features.postulaciones.dto.DatosComplementarios.Infraestructura;
import com.comedoruagrm.backend.features.postulaciones.dto.DatosComplementarios.MiembroGrupoFamiliar;
import com.comedoruagrm.backend.features.postulaciones.dto.DatosComplementarios.Residencia;
import com.comedoruagrm.backend.features.postulaciones.repository.EnvioPostulacionConsultaRepository.Respuesta;

/**
 * Reglas de valor de las respuestas socioeconomicas, compartidas por el envio (CU02) y el borrador (CU17).
 *
 * * Solo revisa los campos que traen valor: un campo nulo no es «invalido», es «falta». Quien envia exige antes que
 * * no falte ninguno (`ERR_ENV_04`); quien guarda un borrador acepta los nulos a proposito.
 * * Los limites y las opciones salen de {@link OpcionesSocioeconomicas}.
 */
final class ReglasRespuestaSocioeconomica {

    private ReglasRespuestaSocioeconomica() {
    }

    /** Los motivos por los que algun valor presente no es valido; vacio si todos lo son. */
    static List<String> invalidos(Respuesta r) {
        List<String> invalidos = new ArrayList<>();
        if (r.ingresosFamiliares() != null && (r.ingresosFamiliares().signum() < 0
                || r.ingresosFamiliares().compareTo(BigDecimal.valueOf(OpcionesSocioeconomicas.INGRESOS_MAXIMO)) > 0
                || r.ingresosFamiliares().scale() > 2)) {
            invalidos.add("los ingresos familiares deben ser un monto positivo con hasta 2 decimales");
        }
        if (r.dependientes() != null
                && (r.dependientes() < 0 || r.dependientes() > OpcionesSocioeconomicas.DEPENDIENTES_MAXIMO)) {
            invalidos.add("los dependientes deben estar entre 0 y " + OpcionesSocioeconomicas.DEPENDIENTES_MAXIMO);
        }
        if (r.procedencia() != null && !OpcionesSocioeconomicas.PROCEDENCIAS.contains(r.procedencia())) {
            invalidos.add("la procedencia no es una opción válida");
        }
        if (r.patrimonio() != null && !OpcionesSocioeconomicas.PATRIMONIOS.contains(r.patrimonio())) {
            invalidos.add("el patrimonio no es una opción válida");
        }
        if (r.estadoVulnerabilidad() != null
                && !OpcionesSocioeconomicas.ESTADOS_VULNERABILIDAD.contains(r.estadoVulnerabilidad())) {
            invalidos.add("el estado de vulnerabilidad no es una opción válida");
        }
        if (r.promedioAcademico() != null && (r.promedioAcademico().signum() < 0
                || r.promedioAcademico().compareTo(BigDecimal.valueOf(OpcionesSocioeconomicas.PROMEDIO_MAXIMO)) > 0
                || r.promedioAcademico().scale() > 2)) {
            invalidos.add("el promedio académico debe estar entre 0 y " + OpcionesSocioeconomicas.PROMEDIO_MAXIMO
                    + " con hasta 2 decimales");
        }
        return invalidos;
    }

    /**
     * Los datos complementarios con los textos recortados (en blanco = nulo) y sin las filas vacias del grupo
     * familiar; nulo si no queda nada. Asi lo que se valida y se guarda es lo que la persona de verdad escribio.
     */
    static DatosComplementarios limpiar(DatosComplementarios d) {
        if (d == null) {
            return null;
        }
        List<MiembroGrupoFamiliar> grupo = d.grupoFamiliar() == null ? null : d.grupoFamiliar().stream()
                .filter(java.util.Objects::nonNull)
                .map(m -> new MiembroGrupoFamiliar(limpio(m.nombre()), limpio(m.parentesco()), m.edad(),
                        limpio(m.ocupacion()), limpio(m.observacion())))
                .filter(m -> m.nombre() != null || m.parentesco() != null || m.edad() != null
                        || m.ocupacion() != null || m.observacion() != null)
                .toList();
        Residencia residencia = d.residencia() == null ? null : new Residencia(limpio(d.residencia().provincia()),
                limpio(d.residencia().zonaAnillo()), limpio(d.residencia().barrio()), limpio(d.residencia().calle()));
        // * El detalle solo tiene sentido si la respuesta fue «si»: no se arrastra un texto de una respuesta vieja.
        String otroDetalle = Boolean.TRUE.equals(d.otroBeneficio()) ? limpio(d.otroBeneficioDetalle()) : null;
        String discapacidadDetalle = Boolean.TRUE.equals(d.discapacidad()) ? limpio(d.discapacidadDetalle()) : null;
        String cantidadHijos = Boolean.FALSE.equals(d.tieneHijos()) ? null : limpio(d.cantidadHijos());
        DatosComplementarios limpio = new DatosComplementarios(limpio(d.dependenciaEconomica()),
                limpio(d.ocupacionSosten()), limpio(d.rangoIngresos()), d.tieneHijos(), cantidadHijos,
                grupo == null || grupo.isEmpty() ? null : grupo, residencia, limpio(d.tenenciaVivienda()),
                d.infraestructura(), d.otroBeneficio(), otroDetalle, d.discapacidad(), discapacidadDetalle,
                limpio(d.comentario()));
        return vacio(limpio) ? null : limpio;
    }

    /** Los motivos por los que algun valor presente de los datos complementarios no es valido. */
    static List<String> invalidosComplementarios(DatosComplementarios d) {
        List<String> invalidos = new ArrayList<>();
        if (d == null) {
            return invalidos;
        }
        opcion(invalidos, d.dependenciaEconomica(), OpcionesSocioeconomicas.DEPENDENCIAS, "la dependencia económica");
        opcion(invalidos, d.ocupacionSosten(), OpcionesSocioeconomicas.OCUPACIONES_SOSTEN, "la ocupación");
        opcion(invalidos, d.rangoIngresos(), OpcionesSocioeconomicas.RANGOS_INGRESOS, "el rango de ingresos");
        opcion(invalidos, d.cantidadHijos(), OpcionesSocioeconomicas.CANTIDADES_HIJOS, "la cantidad de hijos");
        opcion(invalidos, d.tenenciaVivienda(), OpcionesSocioeconomicas.TENENCIAS_VIVIENDA,
                "la tenencia de la vivienda");
        if (d.grupoFamiliar() != null) {
            if (d.grupoFamiliar().size() > OpcionesSocioeconomicas.GRUPO_FAMILIAR_MAXIMO) {
                invalidos.add("el grupo familiar admite hasta " + OpcionesSocioeconomicas.GRUPO_FAMILIAR_MAXIMO
                        + " personas");
            }
            for (MiembroGrupoFamiliar m : d.grupoFamiliar()) {
                if (m == null) {
                    continue;
                }
                largo(invalidos, m.nombre(), OpcionesSocioeconomicas.TEXTO_DETALLE_MAXIMO, "el nombre del familiar");
                largo(invalidos, m.parentesco(), OpcionesSocioeconomicas.TEXTO_CORTO_MAXIMO, "el parentesco");
                largo(invalidos, m.ocupacion(), OpcionesSocioeconomicas.TEXTO_CORTO_MAXIMO,
                        "la ocupación del familiar");
                largo(invalidos, m.observacion(), OpcionesSocioeconomicas.TEXTO_DETALLE_MAXIMO,
                        "la observación del familiar");
                if (m.edad() != null && (m.edad() < 0 || m.edad() > OpcionesSocioeconomicas.EDAD_MAXIMA)) {
                    invalidos.add("la edad de un familiar debe estar entre 0 y " + OpcionesSocioeconomicas.EDAD_MAXIMA);
                }
            }
        }
        if (d.residencia() != null) {
            largo(invalidos, d.residencia().provincia(), OpcionesSocioeconomicas.TEXTO_CORTO_MAXIMO, "la provincia");
            largo(invalidos, d.residencia().zonaAnillo(), OpcionesSocioeconomicas.TEXTO_CORTO_MAXIMO,
                    "la zona o anillo");
            largo(invalidos, d.residencia().barrio(), OpcionesSocioeconomicas.TEXTO_CORTO_MAXIMO, "el barrio");
            largo(invalidos, d.residencia().calle(), OpcionesSocioeconomicas.TEXTO_CORTO_MAXIMO, "la calle");
        }
        if (d.infraestructura() != null) {
            for (Integer cantidad : List.of(nz(d.infraestructura().dormitorios()), nz(d.infraestructura().banos()),
                    nz(d.infraestructura().comedor()), nz(d.infraestructura().sala()),
                    nz(d.infraestructura().patio()))) {
                if (cantidad < 0 || cantidad > OpcionesSocioeconomicas.AMBIENTES_MAXIMO) {
                    invalidos.add("la cantidad de cada ambiente de la vivienda debe estar entre 0 y "
                            + OpcionesSocioeconomicas.AMBIENTES_MAXIMO);
                    break;
                }
            }
        }
        largo(invalidos, d.otroBeneficioDetalle(), OpcionesSocioeconomicas.TEXTO_DETALLE_MAXIMO,
                "el detalle del otro beneficio");
        largo(invalidos, d.discapacidadDetalle(), OpcionesSocioeconomicas.TEXTO_DETALLE_MAXIMO,
                "el tipo y grado de discapacidad");
        largo(invalidos, d.comentario(), OpcionesSocioeconomicas.COMENTARIO_MAXIMO, "el comentario");
        return invalidos;
    }

    /**
     * Lo obligatorio de los datos complementarios al enviar (CU02), con el nombre que ve la persona.
     *
     * * Son las preguntas del formulario en papel que tienen una sola respuesta que marcar. El resto (grupo
     * * familiar, residencia, vivienda, comentario) es opcional.
     * ? Cuales son obligatorios esta por definir con la DUBSS.
     */
    static List<String> faltantesComplementarios(DatosComplementarios d) {
        List<String> faltantes = new ArrayList<>();
        if (d == null || d.dependenciaEconomica() == null) {
            faltantes.add("de quién depende");
        }
        if (d == null || d.ocupacionSosten() == null) {
            faltantes.add("ocupación");
        }
        if (d == null || d.rangoIngresos() == null) {
            faltantes.add("rango de ingresos");
        }
        if (d == null || d.tieneHijos() == null) {
            faltantes.add("si tiene hijos");
        } else if (d.tieneHijos() && d.cantidadHijos() == null) {
            faltantes.add("cuántos hijos tiene");
        }
        if (d == null || d.tenenciaVivienda() == null) {
            faltantes.add("tenencia de la vivienda");
        }
        if (d == null || d.otroBeneficio() == null) {
            faltantes.add("si tiene otro beneficio en la universidad");
        }
        if (d == null || d.discapacidad() == null) {
            faltantes.add("si tiene alguna discapacidad");
        }
        return faltantes;
    }

    private static void opcion(List<String> invalidos, String valor, List<String> opciones, String que) {
        if (valor != null && !opciones.contains(valor)) {
            invalidos.add(que + " no es una opción válida");
        }
    }

    private static void largo(List<String> invalidos, String valor, int maximo, String que) {
        if (valor != null && valor.length() > maximo) {
            invalidos.add(que + " no puede superar " + maximo + " caracteres");
        }
    }

    private static int nz(Integer n) {
        return n == null ? 0 : n;
    }

    private static boolean vacio(DatosComplementarios d) {
        Infraestructura i = d.infraestructura();
        boolean sinAmbientes = i == null || (i.dormitorios() == null && i.banos() == null && i.comedor() == null
                && i.sala() == null && i.patio() == null);
        Residencia r = d.residencia();
        boolean sinResidencia = r == null || (r.provincia() == null && r.zonaAnillo() == null
                && r.barrio() == null && r.calle() == null);
        return d.dependenciaEconomica() == null && d.ocupacionSosten() == null && d.rangoIngresos() == null
                && d.tieneHijos() == null && d.cantidadHijos() == null && d.grupoFamiliar() == null
                && sinResidencia && d.tenenciaVivienda() == null && sinAmbientes && d.otroBeneficio() == null
                && d.otroBeneficioDetalle() == null && d.discapacidad() == null && d.discapacidadDetalle() == null
                && d.comentario() == null;
    }

    /** Un texto sin espacios sobrantes; vacio o en blanco es nulo. */
    static String limpio(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
