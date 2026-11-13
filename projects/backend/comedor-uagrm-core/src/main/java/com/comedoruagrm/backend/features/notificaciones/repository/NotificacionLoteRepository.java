package com.comedoruagrm.backend.features.notificaciones.repository;

import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.comedoruagrm.backend.features.notificaciones.enums.TipoNotificacion;

import lombok.RequiredArgsConstructor;

/**
 * Alta de avisos de la bandeja por lotes (CU22): una sola sentencia `INSERT` de varias filas por lote, en vez
 * de una por aviso. JPA no puede agrupar altas con `IDENTITY`, por eso este camino va por JDBC.
 *
 * * Corre dentro de la transaccion de quien llama. Devuelve el `id` y el codigo publico de cada fila, en el
 * * mismo orden en que se pidieron, para despachar el push y el correo despues del commit.
 */
@Repository
@RequiredArgsConstructor
public class NotificacionLoteRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** Un aviso a guardar. `postulacionId` es opcional. */
    public record NuevoAviso(long usuarioId, String titulo, String mensaje, TipoNotificacion tipo,
            Long postulacionId) {
    }

    /** El aviso ya guardado. */
    public record AvisoGuardado(long id, UUID codigo, long usuarioId) {
    }

    /** Los codigos publicos de varias postulaciones en una sola consulta (para el `data` del push). */
    public Map<Long, UUID> codigosDePostulaciones(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, UUID> codigos = new HashMap<>();
        jdbc.query("SELECT id, codigo_postulacion FROM postulacion WHERE id IN (:ids) AND eliminado_en IS NULL",
                new MapSqlParameterSource("ids", ids),
                rs -> {
                    codigos.put(rs.getLong("id"), rs.getObject("codigo_postulacion", UUID.class));
                });
        return codigos;
    }

    /** Inserta todos los avisos con una sola sentencia; el llamador parte la lista en lotes. */
    public List<AvisoGuardado> insertar(List<NuevoAviso> avisos) {
        if (avisos.isEmpty()) {
            return List.of();
        }
        StringBuilder sql = new StringBuilder(
                "INSERT INTO notificacion (usuario_id, titulo, mensaje, tipo, postulacion_id) VALUES ");
        MapSqlParameterSource p = new MapSqlParameterSource();
        for (int i = 0; i < avisos.size(); i++) {
            NuevoAviso a = avisos.get(i);
            sql.append(i == 0 ? "" : ", ").append("(:u").append(i).append(", :t").append(i).append(", :m")
                    .append(i).append(", CAST(:tp").append(i).append(" AS tipo_notificacion), :p").append(i)
                    .append(')');
            p.addValue("u" + i, a.usuarioId());
            p.addValue("t" + i, a.titulo(), Types.VARCHAR);
            p.addValue("m" + i, a.mensaje(), Types.VARCHAR);
            p.addValue("tp" + i, a.tipo().name(), Types.VARCHAR);
            p.addValue("p" + i, a.postulacionId(), Types.BIGINT);
        }
        sql.append(" RETURNING id, codigo_notificacion, usuario_id");
        List<AvisoGuardado> guardados = jdbc.query(sql.toString(), p,
                (rs, n) -> new AvisoGuardado(rs.getLong("id"), rs.getObject("codigo_notificacion", UUID.class),
                        rs.getLong("usuario_id")));
        if (guardados.size() != avisos.size()) {
            throw new IllegalStateException("El lote de avisos no se guardo completo");
        }
        return guardados;
    }
}
