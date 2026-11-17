-- =====================================================================
-- Modelo de preseleccion inicial `v1.0` (CU03, CU26).
-- Tablas: modelo_preseleccion, parametro_modelo (solo datos)
--
-- El modulo de IA responde con la version `v1.0` cuando el core todavia no le manda ninguna, y el core
-- rechaza un puntaje cuya version no esta registrada: sin esta fila, ninguna postulacion llegaria a
-- `calificada` hasta que un Administrador guardara su primera version en el CU26.
--
-- Se registra INACTIVA a proposito: no es una version guardada por la DUBSS sino el punto de partida
-- del modulo de IA. Mientras no haya una vigente (`activo`), el core manda `modelo` y `parametros` en
-- `null` y la IA aplica sus ponderaciones por defecto, que son estas mismas (`app/config.py` del modulo
-- de IA, y `VariablesModelo` del core). Al guardar la primera version desde el CU26 se crea `v1.1`,
-- activa, y esta queda en el historial. No ocupa el unico lugar de modelo activo.
--
-- Las ponderaciones (diez variables, suman 100) miden necesidad economica y familiar, por dimension:
-- capacidad economica del hogar 45, carga familiar 25, apoyo familiar y situacion personal 20 y costo de
-- estudiar lejos de casa 10. El promedio academico pesa 0: el auditor ya suma hasta 30 puntos de
-- rendimiento academico en el CU07 y no se cuenta dos veces. Son un criterio inicial; los valores
-- definitivos los fija la DUBSS en el CU26.
--
-- Idempotente: si ya existe una `v1.0` (guardada a mano desde el CU26) no se toca.
-- =====================================================================

WITH nuevo AS (
    INSERT INTO modelo_preseleccion (version, descripcion, activo)
    SELECT 'v1.0', 'Modelo inicial con las ponderaciones por defecto (por definir con la DUBSS)', false
    WHERE NOT EXISTS (SELECT 1 FROM modelo_preseleccion WHERE version = 'v1.0' AND eliminado_en IS NULL)
    RETURNING id
)
INSERT INTO parametro_modelo (modelo_id, clave, ponderacion)
SELECT nuevo.id, p.clave, p.ponderacion
FROM nuevo
CROSS JOIN (VALUES
    ('ingresos_familiares', 28.00),
    ('dependientes', 12.00),
    ('procedencia', 10.00),
    ('patrimonio', 9.00),
    ('estado_vulnerabilidad', 7.00),
    ('promedio_academico', 0.00),
    ('dependencia_economica', 10.00),
    ('ocupacion_sosten', 8.00),
    ('discapacidad', 10.00),
    ('hijos', 6.00)
) AS p (clave, ponderacion);
