# Caso de uso

| Id   | Nombre                         | Módulo                      | Prioridad | Estado      |
| ---- | ------------------------------ | --------------------------- | --------- | ----------- |
| CU25 | Consultar bitácora de acciones | P6. Reportes y trazabilidad | Media     | Hecho       |

**Actor principal:** Administrador DUBSS
**Actores secundarios:** —
**Objetivo:** Que el Administrador consulte el registro de las acciones relevantes del sistema para garantizar la trazabilidad de las decisiones.
**Plataforma:** Web
**RF asociados:** RF-20

**Descripción:**
El Administrador DUBSS entra a «Bitácora» y el sistema le muestra, de la más reciente a la más antigua, las acciones registradas con fecha y hora, usuario, acción, entidad afectada e IP. Puede filtrar por usuario, tipo de acción y rango de fechas. Es una consulta de **solo lectura**: la bitácora no se edita ni se elimina desde el sistema. Quién escribe en ella son los demás CU ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md) y siguientes); este CU solo la consulta.

---

## Precondiciones

- El Administrador está autenticado ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)) con el rol `ADMINISTRADOR_DUBSS` y su cuenta está activa.

## Flujo principal

1. El Administrador selecciona la opción «Bitácora».
2. El sistema muestra la primera página de acciones, de la más reciente a la más antigua.
3. *(Opcional)* El Administrador filtra por usuario, acción y/o rango de fechas y pulsa «Buscar».
4. El sistema muestra las acciones que cumplen todos los filtros, paginadas.

## Flujos alternativos y excepciones

- **A1 (paso 2 o 4):** no hay acciones que cumplan → el sistema muestra la lista vacía con el mensaje «Sin resultados».
- **A2 (paso 3):** la fecha «desde» es posterior a «hasta» → `400` `ERR_BIT_02`.
- **A3 (paso 2 o 4):** el usuario no tiene el rol Administrador DUBSS → `403` `ERR_AUTH_07`.
- **A4 (paso 2 o 4):** la sesión venció → `401`; el cliente renueva o lleva al login ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)).

## Reglas de negocio

- Solo lectura: no existe ninguna operación para editar ni eliminar una fila (RF-20).
- El filtro por usuario busca por nombres, apellidos o correo; los filtros se combinan (todos deben cumplirse).
- Las fechas son días de calendario en hora de Bolivia y ambos extremos están incluidos.
- Consultar la bitácora no se anota en la bitácora.
- La respuesta no incluye el id interno de la fila afectada (ver [PERSISTENCIA_BD_BACKEND.md](../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)).

## Postcondiciones

- El sistema no cambia: la consulta no modifica ningún dato.

## Criterios de aceptación

- **CA01:** Dado que hay acciones registradas, el sistema debe listarlas de la más reciente a la más antigua, paginadas.
- **CA02:** Dado que se filtra por usuario, acción o rango de fechas, el sistema debe mostrar solo las acciones que cumplen todos los filtros.
- **CA03:** Dado que «desde» es posterior a «hasta», el sistema debe responder `400` con `ERR_BIT_02`.
- **CA04:** Dado que el usuario no es Administrador DUBSS, el sistema debe responder `403`.
- **CA05:** Dado el módulo de bitácora, el sistema no debe ofrecer ninguna operación para modificar o eliminar acciones.

## Desarrollador

Leonel Barrios
