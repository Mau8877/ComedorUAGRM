package com.comedoruagrm.backend.features.preseleccion.service;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Numeracion de las versiones del modelo (CU26): `v{M}.{n}`. Guardar parametros crea `v{M}.{n+1}` sobre la mayor
 * version existente; si no hay ninguna, `v1.0`. Una version sin punto (`v1`, la que pudo registrar el modulo de IA)
 * cuenta como `v1.0`; las que no siguen el formato se ignoran.
 */
final class VersionModelo {

    private static final Pattern FORMATO = Pattern.compile("^v(\\d{1,9})(?:\\.(\\d{1,9}))?$");

    private VersionModelo() {
    }

    /** La version que sigue a la mayor de `existentes`. */
    static String siguiente(Collection<String> existentes) {
        long mayor = -1;
        long menor = -1;
        for (String version : existentes) {
            Matcher m = version == null ? null : FORMATO.matcher(version.trim());
            if (m == null || !m.matches()) {
                continue;
            }
            long a = Long.parseLong(m.group(1));
            long b = m.group(2) == null ? 0 : Long.parseLong(m.group(2));
            if (a > mayor || (a == mayor && b > menor)) {
                mayor = a;
                menor = b;
            }
        }
        return mayor < 0 ? "v1.0" : "v" + mayor + "." + (menor + 1);
    }
}
