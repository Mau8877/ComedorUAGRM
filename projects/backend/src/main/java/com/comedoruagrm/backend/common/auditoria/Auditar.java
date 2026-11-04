package com.comedoruagrm.backend.common.auditoria;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anota en la bitacora el evento de negocio que produce un metodo de `service`
 * cuando termina bien. El metodo tiene que devolver un {@link Auditable}: de ahi
 * salen el actor, la entidad afectada y el detalle. La IP sale del request en curso.
 *
 * * Solo se anota si el metodo no lanza una excepcion; corre dentro de la
 * * transaccion de quien llama, asi un rollback tambien deshace la fila.
 * ! Funciona por proxy de Spring: solo intercepta llamadas que vienen de OTRO
 * ! bean. Una llamada de un metodo a otro de la misma clase no se audita.
 * ? Un evento que no encaja (no devuelve un Auditable) se anota a mano con
 * ? BitacoraService.registrar(...).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auditar {

    /** Valor de `bitacora.accion`, una constante de BitacoraAcciones. */
    String accion();

    /** Valor de `bitacora.entidad`: la tabla afectada, ej. "usuario". */
    String entidad();
}
