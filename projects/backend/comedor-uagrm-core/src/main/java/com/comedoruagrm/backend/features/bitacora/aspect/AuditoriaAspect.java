package com.comedoruagrm.backend.features.bitacora.aspect;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.comedoruagrm.backend.common.auditoria.Auditable;
import com.comedoruagrm.backend.common.auditoria.Auditar;
import com.comedoruagrm.backend.features.bitacora.service.BitacoraService;

import lombok.RequiredArgsConstructor;

/**
 * Anota en la bitacora los metodos marcados con {@link Auditar} cuando
 * terminan bien. Toma el actor, la entidad y el detalle del {@link Auditable}
 * que devuelve el metodo, y la IP del request en curso.
 *
 * * `@AfterReturning`: si el metodo lanza una excepcion no se anota nada.
 * * El service de bitacora se une a la transaccion de quien llama, asi un
 * * rollback tambien deshace la fila.
 * ! Solo intercepta llamadas entre beans (proxy de Spring), no las internas
 * ! de una misma clase.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditoriaAspect {

    private final BitacoraService bitacoraService;

    @AfterReturning(pointcut = "@annotation(auditar)", returning = "resultado")
    public void anotar(Auditar auditar, Object resultado) {
        if (!(resultado instanceof Auditable auditable) || !auditable.auditar()) {
            return;
        }
        bitacoraService.registrar(auditable.actorId(), auditable.actorCodigo(), auditar.accion(),
                auditar.entidad(), auditable.entidadId(), auditable.detalle(), ipDelRequest());
    }

    /** Null fuera de un request HTTP (un job, un test del service): la bitacora acepta una IP vacia. */
    private String ipDelRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            return atributos.getRequest().getRemoteAddr();
        }
        return null;
    }
}
