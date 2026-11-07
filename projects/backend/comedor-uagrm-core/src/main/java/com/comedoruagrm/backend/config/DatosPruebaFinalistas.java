package com.comedoruagrm.backend.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;

/**
 * Datos de ejemplo para ver y probar el CU21 (finalistas, avisos y comunicados) en desarrollo.
 *
 * * Solo con el perfil `dev`, por defecto activo; se apaga con `app.seed.finalistas.enabled=false`.
 * * Corre despues del cargador del CU06 (que crea la convocatoria `[PRUEBA]` y sus 10 finalistas) y no
 * * hace nada si no los encuentra. Es idempotente: si ya existe la cuenta de prueba, no hace nada.
 * * Habilita el inicio de sesion de UN finalista ya notificado (el de la posicion 2), cambiandole el
 * * correo a `postulante.finalista@prueba.local` y poniendole la contraseña de prueba (solo su hash BCrypt),
 * * para poder ver la bandeja como Postulante. Deja un aviso en la bandeja de los tres finalistas
 * * notificados (el de la posicion 3 sin `enviada_en`, para ver «Fallida») y un comunicado `[PRUEBA]` con
 * * su aviso para los primeros postulantes de la convocatoria.
 * * No manda push ni correo reales y no cambia el estado de ninguna postulacion.
 * ! NUNCA en produccion: la contraseña es de prueba y nunca se loguea.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.finalistas.enabled", havingValue = "true", matchIfMissing = true)
@Order(3)
@RequiredArgsConstructor
public class DatosPruebaFinalistas implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DatosPruebaFinalistas.class);

    static final String CORREO = "postulante.finalista@prueba.local";
    private static final String CONVOCATORIA = DatosPruebaListaPriorizada.MARCA + " Beca Comedor 2026-II";
    private static final String TITULO_COMUNICADO = DatosPruebaListaPriorizada.MARCA
            + " Calendario de la auditoría física";
    private static final int DESTINATARIOS_COMUNICADO = 15;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacciones;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.finalistas.contrasena:123123123}")
    private String contrasena;

    @Override
    public void run(String... args) {
        Integer existente = jdbc.queryForObject("SELECT count(*) FROM usuario WHERE correo = ?", Integer.class, CORREO);
        if (existente != null && existente > 0) {
            return;
        }
        List<Long> convocatorias = jdbc.queryForList(
                "SELECT id FROM convocatoria WHERE nombre = ? AND eliminado_en IS NULL", Long.class, CONVOCATORIA);
        List<Long> administradores = jdbc.queryForList("""
                SELECT u.id FROM usuario u JOIN rol r ON r.id = u.rol_id
                WHERE r.nombre = 'ADMINISTRADOR_DUBSS' AND u.eliminado_en IS NULL AND u.activo
                ORDER BY u.id LIMIT 1
                """, Long.class);
        if (convocatorias.isEmpty() || administradores.isEmpty()) {
            LOG.warn("No se cargaron los datos de prueba de finalistas: falta la convocatoria de prueba o un "
                    + "Administrador DUBSS");
            return;
        }
        Integer finalistas = jdbc.queryForObject("""
                SELECT count(*) FROM finalista f JOIN postulacion po ON po.id = f.postulacion_id
                WHERE po.convocatoria_id = ? AND f.eliminado_en IS NULL
                """, Integer.class, convocatorias.get(0));
        if (finalistas == null || finalistas == 0) {
            LOG.warn("No se cargaron los datos de prueba de finalistas: la convocatoria de prueba no tiene finalistas");
            return;
        }
        transacciones.executeWithoutResult(estado -> cargar(convocatorias.get(0), administradores.get(0)));
        LOG.info("Datos de prueba de finalistas cargados: cuenta {} (contraseña de prueba de desarrollo)", CORREO);
    }

    private void cargar(long convocatoria, long administradorId) {
        // El finalista de la posicion 2 (notificado y sin entrega) pasa a ser la cuenta con sesion.
        List<Long> cuenta = jdbc.queryForList("""
                SELECT po.postulante_id FROM finalista f JOIN postulacion po ON po.id = f.postulacion_id
                WHERE po.convocatoria_id = ? AND f.posicion = 2 AND f.eliminado_en IS NULL
                """, Long.class, convocatoria);
        if (!cuenta.isEmpty()) {
            jdbc.update("UPDATE usuario SET correo = ?, password_hash = ? WHERE id = ?",
                    CORREO, passwordEncoder.encode(contrasena), cuenta.get(0));
        }

        // Aviso de cada finalista notificado; el de la posicion 3 queda sin `enviada_en` («Fallida»).
        jdbc.update("""
                INSERT INTO notificacion (usuario_id, postulacion_id, titulo, mensaje, tipo, leida, enviada_en)
                SELECT po.postulante_id, po.id, 'Entrega de documentación física - Beca Comedor',
                       'Hola, quedaste como finalista de la Beca Comedor. Debes entregar tu documentación física '
                       || 'hasta el ' || to_char(f.plazo_entrega, 'DD/MM/YYYY')
                       || '. Lugar y horario: DUBSS, oficina 3, lunes a viernes de 8:00 a 12:00. '
                       || 'Documentos a presentar: carnet de identidad y certificado de ingresos.',
                       'INFO', false, CASE WHEN f.posicion = 3 THEN NULL ELSE now() - interval '2 days' END
                FROM finalista f JOIN postulacion po ON po.id = f.postulacion_id
                WHERE po.convocatoria_id = ? AND f.notificado_en IS NOT NULL AND f.eliminado_en IS NULL
                """, convocatoria);

        // Un comunicado a todos los postulantes de la convocatoria, con su aviso para los primeros.
        long comunicado = jdbc.queryForObject("""
                INSERT INTO comunicado (convocatoria_id, publicado_por, titulo, contenido, destinatarios, publicado_en)
                VALUES (?, ?, ?, 'La auditoría física de los finalistas se realiza en la DUBSS. Revisa tu bandeja '
                       || 'por si fuiste notificado como finalista.', CAST('TODOS' AS destinatarios_comunicado),
                        now() - interval '1 day')
                RETURNING id
                """, Long.class, convocatoria, administradorId, TITULO_COMUNICADO);
        jdbc.update("""
                INSERT INTO notificacion (usuario_id, comunicado_id, titulo, mensaje, tipo, leida, enviada_en)
                SELECT t.postulante_id, ?, c.titulo, c.contenido, 'INFO', false, now() - interval '1 day'
                FROM comunicado c,
                     (SELECT po.postulante_id FROM postulacion po
                      WHERE po.convocatoria_id = ? AND po.estado <> 'borrador' AND po.eliminado_en IS NULL
                      ORDER BY po.id LIMIT ?) t
                WHERE c.id = ?
                """, comunicado, convocatoria, DESTINATARIOS_COMUNICADO, comunicado);
    }
}
