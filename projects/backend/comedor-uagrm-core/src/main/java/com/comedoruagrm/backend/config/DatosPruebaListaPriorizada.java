package com.comedoruagrm.backend.config;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;

/**
 * Datos de prueba para ver y probar la lista priorizada (CU06) mientras no existan el CU13, el
 * CU02 y el CU03 que crean convocatorias, postulaciones y puntajes de verdad.
 *
 * * Solo con el perfil `dev` y por defecto activo; se apaga con `app.seed.priorizacion.enabled=false`.
 * * `app.seed.priorizacion.postulaciones` (por defecto 60) fija cuantos postulantes se crean; con
 * * 20000 se mide el rendimiento. Es idempotente: todo lo que crea lleva la marca `[PRUEBA]` /
 * * `@prueba.local`, y si ya existe esa convocatoria no hace nada.
 * * Los postulantes de prueba tienen `password_hash = '!'`: no pueden iniciar sesion. Necesita al
 * * menos un Administrador DUBSS (lo crea DatosInicialesAdmin); si no hay, no carga nada.
 * * Incluye casos dificiles a proposito: empates de puntaje, postulaciones con mas de un puntaje
 * * (el historico no vigente), postulaciones en otros estados y una convocatoria vacia.
 * ! NUNCA en produccion: crea cuentas y datos ficticios.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.priorizacion.enabled", havingValue = "true", matchIfMissing = true)
@Order(2)
@RequiredArgsConstructor
public class DatosPruebaListaPriorizada implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DatosPruebaListaPriorizada.class);

    static final String MARCA = "[PRUEBA]";
    private static final String[] NOMBRES = {"Ana", "Luis", "Carla", "Diego", "Rosa", "Mario", "Lucía", "Jorge",
        "Elena", "Pablo", "Sofía", "Marcos", "Valeria", "Andrés", "Camila", "Rodrigo", "Daniela", "Fabián"};
    private static final String[] APELLIDOS = {"Rojas Vaca", "Mamani Quispe", "Peña Suárez", "Suárez Molina",
        "Flores Terrazas", "Egüez Landívar", "Justiniano Roca", "Vargas Pinto", "Cuéllar Soliz", "Añez Chávez",
        "Salvatierra Gil", "Durán Arteaga", "Cabrera Ribera", "Montaño Oliva", "Zambrana Heredia"};
    private static final int TRAMO = 1000;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacciones;

    @Value("${app.seed.priorizacion.postulaciones:60}")
    private int cantidad;

    @Override
    public void run(String... args) {
        Integer existentes = jdbc.queryForObject(
                "SELECT count(*) FROM convocatoria WHERE nombre LIKE ?", Integer.class, MARCA + "%");
        if (existentes != null && existentes > 0) {
            return;
        }
        List<Long> administradores = jdbc.queryForList("""
                SELECT u.id FROM usuario u JOIN rol r ON r.id = u.rol_id
                WHERE r.nombre = 'ADMINISTRADOR_DUBSS' AND u.eliminado_en IS NULL AND u.activo
                ORDER BY u.id LIMIT 1
                """, Long.class);
        if (administradores.isEmpty()) {
            LOG.warn("No se cargaron los datos de prueba de la lista priorizada: no hay un Administrador DUBSS");
            return;
        }
        long inicio = System.currentTimeMillis();
        transacciones.executeWithoutResult(estado -> cargar(administradores.get(0)));
        LOG.info("Datos de prueba de la lista priorizada cargados: {} postulantes en {} ms",
                cantidad, System.currentTimeMillis() - inicio);
    }

    private void cargar(long administradorId) {
        Random azar = new Random(42);
        long rolPostulante = jdbc.queryForObject(
                "SELECT id FROM rol WHERE nombre = 'POSTULANTE' AND eliminado_en IS NULL", Long.class);
        List<Long> carreras = jdbc.queryForList(
                "SELECT id FROM carrera WHERE eliminado_en IS NULL ORDER BY id", Long.class);

        long gestionA = insertarGestion(MARCA + " 2026-II", "2026-07-01", "2026-12-31");
        long gestionB = insertarGestion(MARCA + " 2027-I", "2027-01-01", "2027-06-30");
        long modeloViejo = insertarModelo(administradorId, MARCA + " v1", "Modelo de prueba (anterior)", false);
        long modeloNuevo = insertarModelo(administradorId, MARCA + " v2", "Modelo de prueba (reprocesado)", true);

        long convocatoriaGrande = insertarConvocatoria(gestionA, administradorId,
                MARCA + " Beca Comedor 2026-II", "2026-07-01", "2026-08-15", "CERRADA");
        long convocatoriaAbierta = insertarConvocatoria(gestionB, administradorId,
                MARCA + " Beca Comedor 2027-I", "2027-01-05", "2027-12-31", "ABIERTA");
        insertarConvocatoria(gestionA, administradorId,
                MARCA + " Convocatoria sin postulaciones", "2027-02-01", "2027-03-01", "BORRADOR");

        List<Long> postulantes = crearPostulantes(rolPostulante, carreras, azar);

        Instant base = OffsetDateTime.of(2026, 7, 10, 8, 0, 0, 0, ZoneOffset.UTC).toInstant();
        crearPostulaciones(convocatoriaGrande, postulantes, postulantes.size(), base, modeloViejo, modeloNuevo,
                azar, true);
        int pocas = Math.min(10, postulantes.size());
        crearPostulaciones(convocatoriaAbierta, postulantes, pocas, base.plus(200, ChronoUnit.DAYS),
                modeloViejo, modeloNuevo, azar, false);
    }

    private long insertarGestion(String etiqueta, String desde, String hasta) {
        return jdbc.queryForObject("""
                INSERT INTO gestion_academica (etiqueta, fecha_inicio, fecha_fin)
                VALUES (?, CAST(? AS date), CAST(? AS date)) RETURNING id
                """, Long.class, etiqueta, desde, hasta);
    }

    private long insertarModelo(long creadoPor, String version, String descripcion, boolean quiereActivo) {
        // Un solo modelo activo en toda la base: solo se activa si no hay otro.
        return jdbc.queryForObject("""
                INSERT INTO modelo_preseleccion (creado_por, version, descripcion, activo)
                VALUES (?, ?, ?, ? AND NOT EXISTS (SELECT 1 FROM modelo_preseleccion WHERE activo
                                                  AND eliminado_en IS NULL))
                RETURNING id
                """, Long.class, creadoPor, version, descripcion, quiereActivo);
    }

    private long insertarConvocatoria(long gestion, long creadoPor, String nombre, String apertura,
            String cierre, String estado) {
        return jdbc.queryForObject("""
                INSERT INTO convocatoria (gestion_id, creado_por, nombre, fecha_apertura, fecha_cierre, cupos,
                                          puntaje_minimo, estado)
                VALUES (?, ?, ?, CAST(? AS timestamptz), CAST(? AS timestamptz), 900, 60,
                        CAST(? AS estado_convocatoria))
                RETURNING id
                """, Long.class, gestion, creadoPor, nombre, apertura + "T00:00:00Z", cierre + "T23:59:59Z", estado);
    }

    private List<Long> crearPostulantes(long rolPostulante, List<Long> carreras, Random azar) {
        List<Object[]> usuarios = new ArrayList<>();
        for (int i = 1; i <= cantidad; i++) {
            String numero = String.format("%07d", i);
            usuarios.add(new Object[] {rolPostulante, "P" + numero, "postulante." + numero + "@prueba.local",
                NOMBRES[azar.nextInt(NOMBRES.length)], APELLIDOS[azar.nextInt(APELLIDOS.length)]});
        }
        jdbc.batchUpdate("""
                INSERT INTO usuario (rol_id, ci, correo, password_hash, nombres, apellidos)
                VALUES (?, ?, ?, '!', ?, ?)
                """, usuarios, TRAMO, (ps, fila) -> {
                    ps.setLong(1, (Long) fila[0]);
                    ps.setString(2, (String) fila[1]);
                    ps.setString(3, (String) fila[2]);
                    ps.setString(4, (String) fila[3]);
                    ps.setString(5, (String) fila[4]);
                });
        List<Long> ids = jdbc.queryForList(
                "SELECT id FROM usuario WHERE correo LIKE 'postulante.%@prueba.local' AND eliminado_en IS NULL"
                + " ORDER BY ci", Long.class);

        List<Object[]> postulantes = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            postulantes.add(new Object[] {ids.get(i), carreras.get(azar.nextInt(carreras.size())),
                String.format("PRB-%07d", i + 1), "7" + String.format("%07d", i + 1)});
        }
        jdbc.batchUpdate("""
                INSERT INTO postulante (usuario_id, carrera_id, registro_universitario, telefono)
                VALUES (?, ?, ?, ?)
                """, postulantes, TRAMO, (ps, fila) -> {
                    ps.setLong(1, (Long) fila[0]);
                    ps.setLong(2, (Long) fila[1]);
                    ps.setString(3, (String) fila[2]);
                    ps.setString(4, (String) fila[3]);
                });
        return ids;
    }

    /**
     * Crea la postulacion de los primeros `cuantos` postulantes con una distribucion de estados y
     * sus puntajes. Con `variada` el reparto es 70 % calificada, 15 % pendiente y el resto en otros
     * estados; si no, 7 de cada 10 calificadas y el resto pendientes.
     */
    private void crearPostulaciones(long convocatoria, List<Long> postulantes, int cuantos, Instant base,
            long modeloViejo, long modeloNuevo, Random azar, boolean variada) {
        List<Object[]> filas = new ArrayList<>();
        for (int i = 0; i < cuantos; i++) {
            String estado = variada ? estadoVariado(azar.nextInt(100)) : (i % 10 < 7 ? "calificada" : "pendiente");
            filas.add(new Object[] {convocatoria, postulantes.get(i), estado,
                Timestamp.from(base.plus(i, ChronoUnit.MINUTES))});
        }
        jdbc.batchUpdate("""
                INSERT INTO postulacion (convocatoria_id, postulante_id, estado, fecha_envio, numero_envios)
                VALUES (?, ?, CAST(? AS estado_postulacion), ?, 1)
                """, filas, TRAMO, (ps, fila) -> {
                    ps.setLong(1, (Long) fila[0]);
                    ps.setLong(2, (Long) fila[1]);
                    ps.setString(3, (String) fila[2]);
                    ps.setTimestamp(4, (Timestamp) fila[3]);
                });

        // Todas menos las `pendiente` tienen puntaje (las de otros estados lo tuvieron antes de
        // cambiar de estado: sirve para comprobar que no entran en la lista).
        List<long[]> conPuntaje = new ArrayList<>();
        jdbc.query("""
                SELECT id FROM postulacion WHERE convocatoria_id = ? AND estado <> 'pendiente'
                ORDER BY postulante_id
                """, rs -> {
                    conPuntaje.add(new long[] {rs.getLong("id")});
                }, convocatoria);

        Instant calculo = base.plus(60, ChronoUnit.DAYS);
        List<Object[]> puntajes = new ArrayList<>();
        for (int i = 0; i < conPuntaje.size(); i++) {
            long postulacion = conPuntaje.get(i)[0];
            // Un decimal y saltos de 0.5: asi hay muchos empates a proposito.
            double valor = Math.round((40 + azar.nextDouble() * 55) * 2) / 2.0;
            if (i % 5 == 0) {
                // Un puntaje anterior (no vigente) y el nuevo, de otro modelo: el reproceso.
                puntajes.add(new Object[] {modeloViejo, postulacion, Math.max(0, valor - 7.5),
                    Timestamp.from(calculo.minus(30, ChronoUnit.DAYS)), false});
                puntajes.add(new Object[] {modeloNuevo, postulacion, valor, Timestamp.from(calculo), true});
            } else {
                puntajes.add(new Object[] {modeloViejo, postulacion, valor, Timestamp.from(calculo), true});
            }
        }
        jdbc.batchUpdate("""
                INSERT INTO puntaje_postulacion (modelo_id, postulacion_id, puntaje_total, fecha_calculo, vigente)
                VALUES (?, ?, ?, ?, ?)
                """, puntajes, TRAMO, (ps, fila) -> {
                    ps.setLong(1, (Long) fila[0]);
                    ps.setLong(2, (Long) fila[1]);
                    ps.setDouble(3, (Double) fila[2]);
                    ps.setTimestamp(4, (Timestamp) fila[3]);
                    ps.setBoolean(5, (Boolean) fila[4]);
                });
    }

    private static String estadoVariado(int tirada) {
        if (tirada < 70) {
            return "calificada";
        }
        if (tirada < 85) {
            return "pendiente";
        }
        if (tirada < 90) {
            return "devuelta";
        }
        if (tirada < 95) {
            return "archivada";
        }
        return tirada < 98 ? "adjudicada" : "no_adjudicada";
    }
}
