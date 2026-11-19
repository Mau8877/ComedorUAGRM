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
 * Escenarios de depuracion: una convocatoria por cada etapa del flujo, con un NOMBRE que dice que datos tiene.
 *
 * * Solo con el perfil `dev`, por defecto activo; se apaga con `app.seed.escenarios.enabled=false`.
 * * Corre al final (despues de volumen) y reutiliza los postulantes de prueba de {@link DatosPruebaListaPriorizada};
 * * necesita al menos {@link #POSTULANTES_MINIMOS}. Es idempotente: si ya existe la convocatoria E01, no hace nada.
 * * El nombre empieza por `[PRUEBA] E0N ESTADO - ...` para que se ordenen por etapa en cualquier selector.
 * * E01 abierta con postulaciones en cada estado previo; E02 cerrada con calificadas (sin finalistas); E03
 * * finalistas sin notificar; E04 finalistas con cada resultado de auditoria; E05 decisiones sin publicar; E06
 * * publicada con adjudicados SIN beca (para «Dar de alta»); E07 padron mixto con un candidato de reemplazo; E08
 * * becas vigentes con asistencias y sin planillas.
 * * Deja ademas las cuentas con clave (por defecto `123123123`): `personal.comedor@prueba.local` (registra
 * * asistencia en la app) y `becado.vigente|suspendido|baja@prueba.local` (para «Mi beca» en cada estado).
 * * Las postulaciones llevan puntaje pero NO respuestas del formulario: no sirven para reprocesar (CU26).
 * ! NUNCA en produccion: crea decisiones, becas, asistencias y cuentas con clave de prueba.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.escenarios.enabled", havingValue = "true", matchIfMissing = true)
@Order(10)
@RequiredArgsConstructor
public class DatosPruebaEscenarios implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DatosPruebaEscenarios.class);

    static final int POSTULANTES_MINIMOS = 8;
    static final String MARCA = DatosPruebaListaPriorizada.MARCA;
    static final String GESTION = MARCA + " Escenarios";

    static final String E01 = MARCA + " E01 ABIERTA - 5 postulaciones: 1 borrador, 1 pendiente, 1 devuelta, "
            + "2 calificadas";
    static final String E02 = MARCA + " E02 CERRADA - 6 calificadas con puntaje de 65 a 35, sin finalistas";
    static final String E03 = MARCA + " E03 CERRADA - 6 calificadas y 4 finalistas sin notificar ni auditar";
    static final String E04 = MARCA + " E04 CERRADA - 4 finalistas: 1 validado, 1 observado, 1 no valido, "
            + "1 sin entrega ni auditoria";
    static final String E05 = MARCA + " E05 CERRADA - decididas sin publicar: 2 adjudicadas, 1 no adjudicada, "
            + "1 archivada";
    static final String E06 = MARCA + " E06 PUBLICADA - 3 adjudicadas SIN beca (dar de alta) y 1 no adjudicada";
    static final String E07 = MARCA + " E07 PUBLICADA - padron: 3 vigentes, 1 suspendida, 1 baja y 1 candidato "
            + "de reemplazo";
    static final String E08 = MARCA + " E08 PUBLICADA - 4 becas vigentes con asistencias de octubre, sin "
            + "planillas ni cotejos";

    static final String CORREO_PERSONAL = "personal.comedor@prueba.local";
    static final String CORREO_VIGENTE = "becado.vigente@prueba.local";
    static final String CORREO_SUSPENDIDO = "becado.suspendido@prueba.local";
    static final String CORREO_BAJA = "becado.baja@prueba.local";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacciones;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.escenarios.contrasena:123123123}")
    private String contrasena;

    @Override
    public void run(String... args) {
        Integer existentes = jdbc.queryForObject(
                "SELECT count(*) FROM convocatoria WHERE nombre = ? AND eliminado_en IS NULL", Integer.class, E01);
        if (existentes != null && existentes > 0) {
            return;
        }
        List<Long> administradores = jdbc.queryForList("""
                SELECT u.id FROM usuario u JOIN rol r ON r.id = u.rol_id
                WHERE r.nombre = 'ADMINISTRADOR_DUBSS' AND u.eliminado_en IS NULL AND u.activo
                ORDER BY u.id LIMIT 1
                """, Long.class);
        List<Long> pool = jdbc.queryForList("""
                SELECT u.id FROM usuario u JOIN postulante p ON p.usuario_id = u.id AND p.eliminado_en IS NULL
                WHERE u.correo LIKE 'postulante.%@prueba.local' AND u.eliminado_en IS NULL ORDER BY u.ci LIMIT 12
                """, Long.class);
        List<Long> modelos = jdbc.queryForList("""
                SELECT id FROM modelo_preseleccion WHERE eliminado_en IS NULL
                ORDER BY (version = 'v1.0') DESC, id LIMIT 1""", Long.class);
        if (administradores.isEmpty() || pool.size() < POSTULANTES_MINIMOS || modelos.isEmpty()) {
            LOG.warn("No se cargaron los escenarios de prueba: falta un Administrador DUBSS, un modelo de "
                    + "preseleccion o {} postulantes de prueba", POSTULANTES_MINIMOS);
            return;
        }
        transacciones.executeWithoutResult(estado -> new Carga(administradores.get(0), pool, modelos.get(0)).cargar());
        LOG.info("Escenarios de prueba cargados: 8 convocatorias [PRUEBA] E01 a E08 y las cuentas {}, {}, {}, {} "
                + "(contraseña de prueba de desarrollo)", CORREO_PERSONAL, CORREO_VIGENTE, CORREO_SUSPENDIDO,
                CORREO_BAJA);
    }

    /** Una pasada de carga: guarda los datos que comparten todos los escenarios. */
    private final class Carga {

        private final long admin;
        private final List<Long> pool;
        private final long modelo;
        private long gestion;
        private long personal;

        Carga(long admin, List<Long> pool, long modelo) {
            this.admin = admin;
            this.pool = pool;
            this.modelo = modelo;
        }

        void cargar() {
            jdbc.execute("SELECT setseed(0.42)");
            gestion = jdbc.queryForObject("""
                    INSERT INTO gestion_academica (etiqueta, fecha_inicio, fecha_fin)
                    VALUES (?, DATE '2026-07-01', DATE '2026-12-31') RETURNING id""", Long.class, GESTION);
            personal = cuenta("PERSONAL_COMEDOR", "E-PERSONAL-01", CORREO_PERSONAL, "Personal", "Comedor Prueba", 0);
            long vigente = cuenta("POSTULANTE", "E-BECADO-01", CORREO_VIGENTE, "Becado", "Vigente Prueba", 1);
            long suspendido = cuenta("POSTULANTE", "E-BECADO-02", CORREO_SUSPENDIDO, "Becado", "Suspendido Prueba", 2);
            long baja = cuenta("POSTULANTE", "E-BECADO-03", CORREO_BAJA, "Becado", "De Baja Prueba", 3);

            e01();
            e02();
            e03();
            e04();
            e05();
            e06();
            e07(vigente, suspendido, baja);
            e08(vigente);
        }

        // ---- Escenarios ----

        private void e01() {
            long c = convocatoria(E01, "ABIERTA", 5);
            postulacion(c, pool.get(0), "borrador", 0);
            long pendiente = postulacion(c, pool.get(1), "pendiente", 4);
            historial(pendiente, null, "borrador", "pendiente", null, 4);
            long devuelta = postulacion(c, pool.get(2), "devuelta", 10);
            historial(devuelta, null, "borrador", "pendiente", null, 10);
            historial(devuelta, admin, "pendiente", "calificada", null, 9);
            historial(devuelta, admin, "calificada", "devuelta", "Falta el certificado de notas legalizado.", 3);
            puntaje(devuelta, "52.00");
            calificada(postulacion(c, pool.get(3), "calificada", 6), "55.00");
            calificada(postulacion(c, pool.get(4), "calificada", 5), "48.50");
        }

        private void e02() {
            long c = convocatoria(E02, "CERRADA", 3);
            String[] puntajes = {"65.00", "58.00", "52.00", "47.00", "41.00", "35.00"};
            for (int i = 0; i < puntajes.length; i++) {
                calificada(postulacion(c, pool.get(i), "calificada", 30 - i), puntajes[i]);
            }
        }

        private void e03() {
            long c = convocatoria(E03, "CERRADA", 3);
            String[] puntajes = {"65.00", "58.00", "52.00", "47.00", "41.00", "35.00"};
            for (int i = 0; i < puntajes.length; i++) {
                long p = calificada(postulacion(c, pool.get(i), "calificada", 30 - i), puntajes[i]);
                if (i < 4) {
                    finalista(p, i + 1, false, false);
                }
            }
        }

        private void e04() {
            long c = convocatoria(E04, "CERRADA", 3);
            String[] resultados = {"VALIDADO", "OBSERVADO", "NO_VALIDO", null};
            for (int i = 0; i < resultados.length; i++) {
                long p = calificada(postulacion(c, pool.get(i), "calificada", 30 - i), String.valueOf(65 - i * 5) + ".00");
                long f = finalista(p, i + 1, true, resultados[i] != null);
                if (resultados[i] != null) {
                    auditoria(f, resultados[i]);
                }
            }
        }

        private void e05() {
            long c = convocatoria(E05, "CERRADA", 2);
            adjudicada(c, pool.get(0), "70.00", 1);
            adjudicada(c, pool.get(1), "64.00", 2);
            long no = postulacion(c, pool.get(2), "no_adjudicada", 20);
            puntaje(no, "50.00");
            auditoria(finalista(no, 3, true, true), "OBSERVADO");
            adjudicacion(no, "NO_ADJUDICADA", null);
            historial(no, admin, "calificada", "no_adjudicada", "No cumple los requisitos.", 2);
            long archivada = postulacion(c, pool.get(3), "archivada", 20);
            puntaje(archivada, "45.00");
            finalista(archivada, 4, true, false);
            historial(archivada, admin, "calificada", "archivada", "No entregó su documentación física.", 2);
        }

        private void e06() {
            long c = convocatoria(E06, "PUBLICADA", 3);
            adjudicada(c, pool.get(0), "70.00", 1);
            adjudicada(c, pool.get(1), "64.00", 2);
            adjudicada(c, pool.get(2), "60.00", 3);
            long no = postulacion(c, pool.get(3), "no_adjudicada", 20);
            puntaje(no, "44.00");
            auditoria(finalista(no, 4, true, true), "OBSERVADO");
            adjudicacion(no, "NO_ADJUDICADA", null);
            historial(no, admin, "calificada", "no_adjudicada", "No cumple los requisitos.", 2);
            publicar(c);
        }

        private void e07(long vigente, long suspendido, long baja) {
            long c = convocatoria(E07, "PUBLICADA", 5);
            long[] adjudicaciones = new long[5];
            long[] usuarios = {vigente, pool.get(0), pool.get(1), suspendido, baja};
            for (int i = 0; i < usuarios.length; i++) {
                adjudicaciones[i] = adjudicada(c, usuarios[i], String.valueOf(70 - i * 2) + ".00", i + 1)[1];
            }
            long b0 = beca(c, usuarios[0], adjudicaciones[0], "VIGENTE");
            long b1 = beca(c, usuarios[1], adjudicaciones[1], "VIGENTE");
            long b2 = beca(c, usuarios[2], adjudicaciones[2], "VIGENTE");
            long b3 = beca(c, usuarios[3], adjudicaciones[3], "SUSPENDIDA");
            long b4 = beca(c, usuarios[4], adjudicaciones[4], "BAJA");
            movimiento(b3, "SUSPENSION", "Suspensión de prueba por inasistencias");
            movimiento(b4, "BAJA", "Baja de prueba por retiro del postulante");
            for (long b : new long[] {b0, b1, b2}) {
                asistencias(b);
            }
            // El candidato: finalista validado, aun «calificada» y sin beca, con puntaje total sobre el minimo.
            long candidato = calificada(postulacion(c, pool.get(2 + 1), "calificada", 25), "50.00");
            auditoria(finalista(candidato, 6, true, true), "VALIDADO");
            publicar(c);
        }

        private void e08(long vigente) {
            long c = convocatoria(E08, "PUBLICADA", 4);
            long[] usuarios = {vigente, pool.get(4), pool.get(5), pool.get(6)};
            for (int i = 0; i < usuarios.length; i++) {
                long adjudicacion = adjudicada(c, usuarios[i], String.valueOf(70 - i * 2) + ".00", i + 1)[1];
                asistencias(beca(c, usuarios[i], adjudicacion, "VIGENTE"));
            }
            publicar(c);
        }

        // ---- Piezas ----

        private long convocatoria(String nombre, String estado, int cupos) {
            return jdbc.queryForObject("""
                    INSERT INTO convocatoria (gestion_id, creado_por, nombre, fecha_apertura, fecha_cierre, cupos,
                                              puntaje_minimo, cantidad_finalistas, estado)
                    VALUES (?, ?, ?, CAST('2026-07-01T00:00:00Z' AS timestamptz),
                            CAST(? AS timestamptz), ?, 40, 4, CAST(? AS estado_convocatoria)) RETURNING id
                    """, Long.class, gestion, admin, nombre,
                    "ABIERTA".equals(estado) ? "2026-12-31T23:59:59Z" : "2026-08-15T23:59:59Z", cupos, estado);
        }

        private long postulacion(long convocatoria, long usuario, String estado, int diasAtras) {
            return jdbc.queryForObject("""
                    INSERT INTO postulacion (convocatoria_id, postulante_id, estado, fecha_envio, numero_envios)
                    VALUES (?, ?, CAST(? AS estado_postulacion),
                            CASE WHEN ? > 0 THEN now() - (? * interval '1 day') END, ?) RETURNING id
                    """, Long.class, convocatoria, usuario, estado, diasAtras, diasAtras,
                    "borrador".equals(estado) ? 0 : 1);
        }

        private void puntaje(long postulacion, String puntaje) {
            jdbc.update("""
                    INSERT INTO puntaje_postulacion (modelo_id, postulacion_id, puntaje_total, fecha_calculo, vigente)
                    VALUES (?, ?, CAST(? AS numeric), now() - interval '20 days', true)
                    """, modelo, postulacion, puntaje);
        }

        /** Marca la postulacion como calificada: su puntaje y el paso del historial. */
        private long calificada(long postulacion, String puntaje) {
            puntaje(postulacion, puntaje);
            historial(postulacion, null, "borrador", "pendiente", null, 20);
            historial(postulacion, admin, "pendiente", "calificada", null, 19);
            return postulacion;
        }

        private void historial(long postulacion, Long actor, String anterior, String nuevo, String motivo,
                int diasAtras) {
            jdbc.update("""
                    INSERT INTO postulacion_historial (postulacion_id, usuario_id, estado_anterior, estado_nuevo,
                                                       motivo, creado_en)
                    VALUES (?, ?, CAST(? AS estado_postulacion), CAST(? AS estado_postulacion), ?,
                            now() - (? * interval '1 day'))
                    """, postulacion, actor, anterior, nuevo, motivo, diasAtras);
        }

        private long finalista(long postulacion, int posicion, boolean notificado, boolean recibida) {
            return jdbc.queryForObject("""
                    INSERT INTO finalista (postulacion_id, posicion, plazo_entrega, notificado_en,
                                           documentacion_recibida_en)
                    VALUES (?, ?, CASE WHEN ? THEN current_date + 7 END,
                            CASE WHEN ? THEN now() - interval '5 days' END,
                            CASE WHEN ? THEN now() - interval '4 days' END) RETURNING id
                    """, Long.class, postulacion, posicion, notificado, notificado, recibida);
        }

        private void auditoria(long finalista, String resultado) {
            boolean validado = "VALIDADO".equals(resultado);
            jdbc.update("""
                    INSERT INTO auditoria_expediente (finalista_id, auditor_id, resultado, observaciones,
                                                      puntaje_academico, fecha_auditoria)
                    VALUES (?, ?, CAST(? AS resultado_auditoria), ?, CAST(? AS numeric), now() - interval '3 days')
                    """, finalista, admin, resultado,
                    validado ? null : "OBSERVADO".equals(resultado) ? "Falta el certificado de notas."
                            : "Los documentos presentados no son válidos.", validado ? "30.00" : null);
        }

        private void adjudicacion(long postulacion, String resultado, String puntajeFinal) {
            jdbc.update("""
                    INSERT INTO adjudicacion (postulacion_id, resultado, puntaje_final, decidido_por,
                                              fecha_adjudicacion)
                    VALUES (?, CAST(? AS resultado_adjudicacion), CAST(? AS numeric), ?, now() - interval '2 days')
                    """, postulacion, resultado, puntajeFinal, admin);
        }

        /** Un finalista adjudicado de punta a punta; devuelve {postulacion, adjudicacion}. */
        private long[] adjudicada(long convocatoria, long usuario, String puntaje, int posicion) {
            long p = postulacion(convocatoria, usuario, "adjudicada", 20);
            puntaje(p, puntaje);
            auditoria(finalista(p, posicion, true, true), "VALIDADO");
            historial(p, admin, "calificada", "adjudicada", null, 2);
            adjudicacion(p, "ADJUDICADA", "95.00");
            long adjudicacion = jdbc.queryForObject(
                    "SELECT id FROM adjudicacion WHERE postulacion_id = ? AND eliminado_en IS NULL", Long.class, p);
            return new long[] {p, adjudicacion};
        }

        private void publicar(long convocatoria) {
            jdbc.update("""
                    INSERT INTO publicacion_resultado (convocatoria_id, publicado_por, publicado_en)
                    VALUES (?, ?, now() - interval '1 day')
                    """, convocatoria, admin);
        }

        private long beca(long convocatoria, long usuario, long adjudicacion, String estado) {
            return jdbc.queryForObject("""
                    INSERT INTO beca (convocatoria_id, postulante_id, adjudicacion_id, estado, fecha_inicio,
                                      fecha_fin)
                    VALUES (?, ?, ?, CAST(? AS estado_beca), DATE '2026-09-01',
                            CASE WHEN ? = 'BAJA' THEN DATE '2026-10-20' END) RETURNING id
                    """, Long.class, convocatoria, usuario, adjudicacion, estado, estado);
        }

        private void movimiento(long beca, String tipo, String motivo) {
            jdbc.update("""
                    INSERT INTO movimiento_beca (beca_id, registrado_por, tipo, fecha, motivo)
                    VALUES (?, ?, CAST(? AS tipo_movimiento_beca), DATE '2026-10-20', ?)
                    """, beca, admin, tipo, motivo);
        }

        /** Asistencia en el 80 % de los dias habiles de octubre; las registra el Personal del Comedor de prueba. */
        private void asistencias(long beca) {
            jdbc.update("""
                    INSERT INTO asistencia (beca_id, registrado_por, fecha, hora_registro)
                    SELECT ?, ?, d.dia, (d.dia + TIME '12:30') AT TIME ZONE 'America/La_Paz'
                    FROM (SELECT CAST(DATE '2026-10-01' + g AS date) AS dia FROM generate_series(0, 29) g) d
                    WHERE EXTRACT(ISODOW FROM d.dia) < 6 AND random() < 0.8
                    """, beca, personal);
        }

        /** Una cuenta con clave; los postulantes llevan su fila de `postulante` (indice > 0 la hace unica). */
        private long cuenta(String rol, String ci, String correo, String nombres, String apellidos, int indice) {
            long usuario = jdbc.queryForObject("""
                    INSERT INTO usuario (rol_id, ci, correo, password_hash, nombres, apellidos)
                    SELECT id, ?, ?, ?, ?, ? FROM rol WHERE nombre = ? RETURNING id
                    """, Long.class, ci, correo, passwordEncoder.encode(contrasena), nombres, apellidos, rol);
            if ("POSTULANTE".equals(rol)) {
                jdbc.update("""
                        INSERT INTO postulante (usuario_id, carrera_id, registro_universitario, telefono)
                        SELECT ?, (SELECT id FROM carrera WHERE eliminado_en IS NULL ORDER BY id LIMIT 1), ?, ?
                        """, usuario, String.format("ESC-%07d", indice), String.format("78%06d", indice));
            }
            return usuario;
        }
    }
}
