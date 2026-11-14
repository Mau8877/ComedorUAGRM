package com.comedoruagrm.backend.features.usuarios.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

/**
 * Genera la contraseña temporal que se le entrega a un funcionario al crear su
 * cuenta (CU14).
 *
 * * 12 caracteres de letras y digitos, sin los que se confunden al leerlos o
 * * dictarlos (`0`/`O`, `1`/`l`/`I`); siempre al menos una letra y un digito.
 * * Cumple de sobra la politica del sistema (6 a 72 caracteres).
 * ! Nunca se loguea lo que devuelve (ni en DEBUG): solo existe en claro en la
 * ! respuesta del alta; en la base queda su hash BCrypt.
 */
@Component
public class GeneradorContrasenaTemporal {

    static final int LONGITUD = 12;

    static final String LETRAS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    static final String DIGITOS = "23456789";
    private static final String ALFABETO = LETRAS + DIGITOS;

    private final SecureRandom random = new SecureRandom();

    public String generar() {
        char[] caracteres = new char[LONGITUD];
        // Una letra y un digito garantizados, el resto del alfabeto completo.
        caracteres[0] = elegir(LETRAS);
        caracteres[1] = elegir(DIGITOS);
        for (int i = 2; i < LONGITUD; i++) {
            caracteres[i] = elegir(ALFABETO);
        }
        mezclar(caracteres);
        return new String(caracteres);
    }

    private char elegir(String conjunto) {
        return conjunto.charAt(random.nextInt(conjunto.length()));
    }

    // Fisher-Yates: sin esto la letra y el digito garantizados quedarian siempre al principio.
    private void mezclar(char[] caracteres) {
        for (int i = caracteres.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char auxiliar = caracteres[i];
            caracteres[i] = caracteres[j];
            caracteres[j] = auxiliar;
        }
    }
}
