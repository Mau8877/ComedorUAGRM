---
globs: projects/backend/**/*
---

# Guía de errores — Backend

Complementa a [EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md) (que define
la jerarquía de excepciones y el **formato** del código, `ERR_{MODULO}_{NUMERO}`)
con el **proceso obligatorio** para cuando se crea un código de error
nuevo: dónde se documenta, con qué formato, y por qué.

## Regla estricta: todo código nuevo se documenta antes de mergear

Cuando se lanza una excepción de negocio con un `errorCode` que todavía no
existía (una constante nueva en el `XxxErrorCodes` del módulo, ver
[EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md#catálogo-de-códigos-de-error)),
se agrega una entrada nueva en
[`docs/errors/CATALOGO_ERRORES.md`](../../../docs/errors/CATALOGO_ERRORES.md)
**en el mismo cambio**, no en un commit aparte "para después". Un código
que existe en el código Java pero no en ese catálogo es, a todo efecto
práctico, un código sin documentar.

### Por qué un catálogo aparte, si el código ya vive en `XxxErrorCodes.java`

La constante Java le sirve al código (el `service` que la lanza). El
catálogo en `docs/errors/` le sirve a las **personas** que no van a andar
grepeando el código fuente para entender qué significa `ERR_PED_04` cuando
lo ven en un log de producción o en la respuesta de un endpoint: soporte,
quien está armando el frontend/mobile contra un endpoint que todavía no
conoce a fondo, o el mismo equipo de backend seis meses después. Es la
misma razón por la que existe un menú en un restaurante aparte de que la
cocina sepa preparar cada plato — la cocina no es a quien hay que
consultarle qué lleva cada plato.

### Qué se documenta de cada código

Una fila en la tabla de `docs/errors/CATALOGO_ERRORES.md`, con estas
columnas obligatorias:

| Columna | Contenido |
| --- | --- |
| Código | El código exacto, ej. `ERR_PED_04` |
| HTTP | El status code que devuelve (viene dado por qué excepción lo lanza — ver la tabla de [EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md)) |
| Módulo | Nombre del feature (`pedidos`, `usuarios`, `auth`, `sistema`) |
| Excepción | La clase que lo lanza (`NotFoundException`, `ConflictException`, etc., o "ninguna" si es un caso especial como el rate limit) |
| Significado | Una frase corta y concreta de cuándo pasa — no el mensaje textual que ve el usuario, sino la causa real |
| Dónde se lanza | Archivo y método donde vive el `throw` (ej. `PedidoService.aprobar()`) — para no tener que buscarlo a mano |

No se documentan los `400` genéricos que dispara Bean Validation
automático (`@NotBlank`, `@Email`, etc. sobre un DTO) — esos no pasan por
un `errorCode` propio, caen en `ERR_SYS_01` (ver
[EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md#manejo-global)), que sí
tiene su propia fila única en el catálogo.

### Cuándo se actualiza una fila existente

Si cambia el significado real de un código ya catalogado (no el texto del
mensaje al usuario, sino la condición de negocio que lo dispara), se
actualiza esa misma fila en el mismo cambio que modifica el `service`. No
se agrega una fila nueva para lo mismo, y no se deja la fila vieja
describiendo un comportamiento que ya no existe.

### Cuándo se borra una fila

Nunca. Igual que un `NUMERO` de código no se reutiliza aunque el error que
lo usaba desaparezca (ver
[EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md#catálogo-de-códigos-de-error)),
la fila del catálogo tampoco se borra — se marca como **retirado** en la
columna de Significado (ej. "Retirado: el módulo de pedidos ya no soporta
este estado desde 2026-XX"). Alguien que encuentre ese código viejo en un
log histórico necesita poder buscarlo y encontrar qué significaba, aunque
ya no se produzca más.

## Un catálogo por proyecto, no por módulo

`docs/errors/CATALOGO_ERRORES.md` es un único archivo con todos los
códigos de todos los módulos (ordenados por prefijo de módulo), no un
archivo por feature — así alguien que recibe un código y no sabe de qué
módulo es puede ir a un solo lugar a buscarlo, en vez de tener que
adivinar en qué archivo mirar primero.
