package com.comedoruagrm.backend.support;

import java.math.BigDecimal;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Filas minimas para probar el expediente (CU07) con una base real: un postulante con su postulacion,
 * su finalista, documentos y auditorias. Cada metodo devuelve el `id` interno de lo que inserta.
 *
 * * Los tests lo usan dentro de una transaccion que se revierte (@DataJpaTest) o limpian lo que commitean.
 * * Los `ci` y etiquetas que se pasan tienen que ser distintos entre tests que commitean.
 */
public final class ExpedienteFixtures {

    private final JdbcTemplate jdbc;

    public ExpedienteFixtures(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Un usuario con el rol dado (`POSTULANTE` o `ADMINISTRADOR_DUBSS`). */
    public long usuario(String ci, String rol) {
        return jdbc.queryForObject("""
                INSERT INTO usuario (rol_id, ci, correo, password_hash, nombres, apellidos)
                SELECT id, ?, ?, '!', 'Ana', 'Rojas' FROM rol WHERE nombre = ? RETURNING id""",
                Long.class, ci, ci + "@prueba.test", rol);
    }

    /** Un usuario postulante con su fila de `postulante`. */
    public long postulante(String ci) {
        long usuario = usuario(ci, "POSTULANTE");
        jdbc.update("""
                INSERT INTO postulante (usuario_id, carrera_id, registro_universitario, telefono)
                VALUES (?, (SELECT min(id) FROM carrera), ?, '70000000')""", usuario, "RU-" + ci);
        return usuario;
    }

    /** Una convocatoria cerrada con su gestion. */
    public long convocatoria(String etiqueta) {
        long gestion = jdbc.queryForObject("""
                INSERT INTO gestion_academica (etiqueta, fecha_inicio, fecha_fin)
                VALUES (?, DATE '2026-07-01', DATE '2026-12-31') RETURNING id""", Long.class, "G-" + etiqueta);
        return jdbc.queryForObject("""
                INSERT INTO convocatoria (gestion_id, nombre, fecha_apertura, fecha_cierre, estado)
                VALUES (?, ?, '2026-07-01T00:00:00Z', '2026-08-31T00:00:00Z',
                        CAST('CERRADA' AS estado_convocatoria)) RETURNING id""", Long.class, gestion, "Conv " + etiqueta);
    }

    /** Una convocatoria cerrada con cupos, puntaje minimo y cantidad de finalistas (CU05); cualquiera puede ser nulo. */
    public long convocatoriaConCupos(String etiqueta, Integer cupos, String puntajeMinimo, Integer cantidadFinalistas) {
        long id = convocatoria(etiqueta);
        jdbc.update("UPDATE convocatoria SET cupos = ?, puntaje_minimo = ?, cantidad_finalistas = ? WHERE id = ?",
                cupos, puntajeMinimo == null ? null : new BigDecimal(puntajeMinimo), cantidadFinalistas, id);
        return id;
    }

    /** Cambia el estado de una convocatoria (`ABIERTA`, `CERRADA`, `BORRADOR`). */
    public void estadoConvocatoria(long convocatoria, String estado) {
        jdbc.update("UPDATE convocatoria SET estado = CAST(? AS estado_convocatoria) WHERE id = ?", estado,
                convocatoria);
    }

    /** Fija la fecha de envio de una postulacion (desempate de la lista priorizada). */
    public void fechaEnvio(long postulacion, String instanteIso) {
        jdbc.update("UPDATE postulacion SET fecha_envio = CAST(? AS timestamptz) WHERE id = ?", instanteIso,
                postulacion);
    }

    /** Una decision de adjudicacion ya registrada (`ADJUDICADA` o `NO_ADJUDICADA`). */
    public long adjudicacion(long postulacion, String resultado, String puntajeFinal, long decididoPor) {
        return jdbc.queryForObject("""
                INSERT INTO adjudicacion (postulacion_id, resultado, puntaje_final, decidido_por)
                VALUES (?, CAST(? AS resultado_adjudicacion), ?, ?) RETURNING id""", Long.class, postulacion,
                resultado, puntajeFinal == null ? null : new BigDecimal(puntajeFinal), decididoPor);
    }

    /** Una postulacion en el estado dado (`calificada`, `devuelta`...). */
    public long postulacion(long convocatoria, long postulanteUsuario, String estado) {
        return jdbc.queryForObject("""
                INSERT INTO postulacion (convocatoria_id, postulante_id, estado, numero_envios)
                VALUES (?, ?, CAST(? AS estado_postulacion), 1) RETURNING id""", Long.class,
                convocatoria, postulanteUsuario, estado);
    }

    public long finalista(long postulacion, int posicion) {
        return jdbc.queryForObject("INSERT INTO finalista (postulacion_id, posicion) VALUES (?, ?) RETURNING id",
                Long.class, postulacion, posicion);
    }

    /** Marca la entrega fisica como recibida hoy. */
    public void entregaRecibida(long finalista) {
        jdbc.update("UPDATE finalista SET documentacion_recibida_en = now() WHERE id = ?", finalista);
    }

    /** Un puntaje de la IA vigente (crea el modelo de preseleccion si hace falta). */
    public void puntajeIa(long postulacion, String total, String detalleJson) {
        Long modelo = jdbc.query("SELECT id FROM modelo_preseleccion WHERE eliminado_en IS NULL LIMIT 1",
                rs -> rs.next() ? rs.getLong(1) : null);
        if (modelo == null) {
            modelo = jdbc.queryForObject("""
                    INSERT INTO modelo_preseleccion (version, descripcion, activo)
                    VALUES ('test-' || floor(random() * 1000000)::int, 'Modelo de prueba', false) RETURNING id""",
                    Long.class);
        }
        jdbc.update("""
                INSERT INTO puntaje_postulacion (modelo_id, postulacion_id, puntaje_total, detalle, fecha_calculo, vigente)
                VALUES (?, ?, ?, CAST(? AS jsonb), now(), true)""", modelo, postulacion, new BigDecimal(total), detalleJson);
    }

    public long tipoDocumento(String nombre) {
        return jdbc.queryForObject("INSERT INTO tipo_documento (nombre) VALUES (?) RETURNING id", Long.class, nombre);
    }

    public void exigeDocumento(long convocatoria, long tipoDocumento, boolean obligatorio) {
        jdbc.update("INSERT INTO convocatoria_documento (convocatoria_id, tipo_documento_id, obligatorio) "
                + "VALUES (?, ?, ?)", convocatoria, tipoDocumento, obligatorio);
    }

    public long documentoAdjunto(long postulacion, long tipoDocumento, String nombreArchivo, String storagePath) {
        return jdbc.queryForObject("""
                INSERT INTO documento_adjunto (postulacion_id, tipo_documento_id, nombre_archivo, mime_type,
                                               storage_path, tamano_bytes)
                VALUES (?, ?, ?, 'application/pdf', ?, 1024) RETURNING id""", Long.class,
                postulacion, tipoDocumento, nombreArchivo, storagePath);
    }

    public void respuestaSocioeconomica(long postulacion, String promedioAcademico) {
        jdbc.update("""
                INSERT INTO respuesta_socioeconomica (postulacion_id, ingresos_familiares, dependientes, patrimonio,
                                                      procedencia, promedio_academico, estado_vulnerabilidad)
                VALUES (?, 1800.00, 4, 'Vivienda alquilada', 'Montero', ?, 'ALTA')""",
                postulacion, new BigDecimal(promedioAcademico));
    }

    /** Una auditoria; `fecha` es un instante ISO (ej. `2026-11-06T10:00:00Z`) para ordenarlas a mano. */
    public long auditoria(long finalista, long auditor, String resultado, String observaciones,
            String puntajeAcademico, String fecha) {
        return jdbc.queryForObject("""
                INSERT INTO auditoria_expediente (finalista_id, auditor_id, resultado, observaciones,
                                                  puntaje_academico, fecha_auditoria)
                VALUES (?, ?, CAST(? AS resultado_auditoria), ?, ?, CAST(? AS timestamptz)) RETURNING id""",
                Long.class, finalista, auditor, resultado, observaciones,
                puntajeAcademico == null ? null : new BigDecimal(puntajeAcademico), fecha);
    }
}
