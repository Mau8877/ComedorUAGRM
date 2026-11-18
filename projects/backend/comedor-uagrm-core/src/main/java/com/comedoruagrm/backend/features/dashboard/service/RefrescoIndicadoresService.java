package com.comedoruagrm.backend.features.dashboard.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Refresca por lote las vistas del dashboard (CU12): `mv_indicadores_convocatoria` y las `mv_bi_*`.
 *
 * * `refrescar` usa `REFRESH ... CONCURRENTLY` (la vista tiene un indice unico para eso): quien consulta el
 * * dashboard no se bloquea mientras tanto. Un fallo se loguea como ERROR y no se propaga: la aplicacion sigue en
 * * pie y la vista conserva los datos del refresco anterior.
 * * `refrescarSiEstaVacia` cubre el primer arranque: en una vista vacia el modo concurrente no sirve, asi que
 * * hace un refresco normal.
 * * Una instancia del backend: con varias, cada una refrescaria (`CONCURRENTLY` tolera que coincidan); si algun dia
 * * hay replicas se coordina con un bloqueo.
 * ! Ni el log lleva datos de postulantes: solo la duracion y el tipo de error.
 */
@Service
@RequiredArgsConstructor
public class RefrescoIndicadoresService {

    private static final Logger LOG = LoggerFactory.getLogger(RefrescoIndicadoresService.class);

    static final String VISTA = "mv_indicadores_convocatoria";

    /**
     * Todas las vistas del dashboard, en el orden en que se refrescan: `mv_bi_convocatoria` lee de
     * `mv_bi_asistencia_dia` (becas en riesgo), asi que esa va primero.
     */
    public static final List<String> VISTAS = List.of(VISTA, "mv_bi_asistencia_dia", "mv_bi_convocatoria",
            "mv_bi_carrera_convocatoria", "mv_bi_puntaje_convocatoria");

    private final JdbcTemplate jdbc;

    /** Refresca la vista sin bloquear las lecturas. Devuelve si salio bien; un fallo no se propaga. */
    public boolean refrescar() {
        return refrescarTodas("REFRESH MATERIALIZED VIEW CONCURRENTLY ");
    }

    /** Refresca la vista solo si no tiene ninguna fila (primer arranque). Devuelve si hizo el refresco y salio bien. */
    public boolean refrescarSiEstaVacia() {
        try {
            Long filas = jdbc.queryForObject("SELECT count(*) FROM " + VISTA, Long.class);
            if (filas != null && filas > 0) {
                return false;
            }
        } catch (RuntimeException e) {
            LOG.error("No se pudo revisar si la vista de indicadores esta vacia", kv("module", "dashboard"),
                    kv("error", e.getClass().getSimpleName()));
            return false;
        }
        return refrescarTodas("REFRESH MATERIALIZED VIEW ");
    }

    /** Refresca cada vista aunque una falle (cada una conserva su dato anterior); es exito solo si todas salen bien. */
    private boolean refrescarTodas(String prefijo) {
        boolean todas = true;
        for (String vista : VISTAS) {
            todas &= ejecutar(prefijo + vista);
        }
        return todas;
    }

    private boolean ejecutar(String sentencia) {
        long inicio = System.nanoTime();
        try {
            jdbc.execute(sentencia);
            LOG.info("Vista de indicadores refrescada", kv("module", "dashboard"),
                    kv("duracionMs", (System.nanoTime() - inicio) / 1_000_000));
            return true;
        } catch (RuntimeException e) {
            LOG.error("Fallo el refresco de la vista de indicadores", kv("module", "dashboard"),
                    kv("error", e.getClass().getSimpleName()));
            return false;
        }
    }
}
