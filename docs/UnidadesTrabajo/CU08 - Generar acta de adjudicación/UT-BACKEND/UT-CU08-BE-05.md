# UT-CU08-BE-05 — Número de acta automático

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-BE-05` |
| **Nombre** | Número de acta automático |
| **Caso de uso** | CU08-BE |
| **Depende de** | `UT-CU08-BE-04` |

## Antes de implementar

- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md) y [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

El Administrador no escribe el número del acta: lo asigna el sistema, correlativo por año y sin repetirse.

## Objetivo

Existe un componente que entrega el siguiente número de acta con el formato `NNN/AAAA`.

## Alcance

- `B/features/actas/service/NumeroActaService.java` (`@Service`, `@RequiredArgsConstructor`), con `siguiente(Instant ahora)` → `String`:
  - El año sale de `ahora` en la zona `America/La_Paz`.
  - Adquiere un bloqueo de transacción por año (`pg_advisory_xact_lock` con una clave derivada del año) para que dos generaciones simultáneas de convocatorias distintas no calculen el mismo correlativo; el bloqueo se libera al terminar la transacción de quien llama (por eso se llama **dentro** de la transacción de generar).
  - Lee el mayor correlativo del año en `acta_adjudicacion` (incluidas filas eliminadas, para no reutilizar nunca un número) y devuelve el siguiente. Formato: correlativo con al menos tres dígitos (`001`, `042`, `1000` si pasa de 999), `/` y el año (`001/2026`). El primero del año es `001`.
  - Para la vista previa existe `borrador()` → `"borrador"`: no consulta ni consume nada.
- Un número nunca se reutiliza ni se «devuelve» si la generación falla después: al hacer rollback no queda fila, así que el siguiente intento recalcula el mismo número (no hay huecos porque no hay secuencia persistida).
- Tests unitarios (formato, año nuevo reinicia en `001`, `999` → `1000/AAAA`, año tomado en La Paz aunque UTC ya esté en el año siguiente, `borrador()` no toca la base) y de base real (dos hilos simultáneos obtienen números distintos; la unicidad de `numero` es la red de seguridad).

Archivos esperados (crear): `NumeroActaService` y su test. (Tocar) `ActasConsultaRepository` solo si falta el método del mayor correlativo del año (declararlo allí).

## Fuera de alcance (no tocar)

- Generar el PDF o registrar el acta (UT-06 y UT-07).
- Una tabla de secuencias o una secuencia de PostgreSQL.
- Permitir que el Administrador edite o elija el número.

## Resultado esperado

Cada acta recibe un número `NNN/AAAA` correlativo por año y único aunque se generen a la vez.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
