package com.comedoruagrm.backend.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inyecta en un parametro `UUID` de controller el `codigo` del usuario
 * autenticado (el identificador publico, nunca el `id` interno). Ver CurrentUserIdArgumentResolver.
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {
}
