package com.comedoruagrm.backend.features.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class TokenEnlaceTest {

    private final TokenEnlace tokenEnlace = new TokenEnlace();

    @Test
    void generar_dosTokens_sonDistintos() {
        assertThat(tokenEnlace.generar()).isNotEqualTo(tokenEnlace.generar());
    }

    @Test
    void generar_1000Tokens_noSeRepiten() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            tokens.add(tokenEnlace.generar());
        }
        assertThat(tokens).hasSize(1000);
    }

    @Test
    void generar_devuelveSoloCaracteresDeUrlYLargoDe256Bits() {
        String token = tokenEnlace.generar();

        // 32 bytes en Base64 URL sin relleno = 43 caracteres
        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void hash_esDeterministaYDe64CaracteresHexadecimales() {
        String hash = tokenEnlace.hash("un-token");

        assertThat(hash).isEqualTo(tokenEnlace.hash("un-token")).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    void hash_tokensDistintos_dan_hashDistintos() {
        assertThat(tokenEnlace.hash("uno")).isNotEqualTo(tokenEnlace.hash("dos"));
    }

    @Test
    void hash_coincideConElSha256DeUnValorConocido() {
        // SHA-256("abc")
        assertThat(tokenEnlace.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void hash_nuncaEsElToken() {
        String token = tokenEnlace.generar();

        assertThat(tokenEnlace.hash(token)).isNotEqualTo(token);
    }
}
