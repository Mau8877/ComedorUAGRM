package com.comedoruagrm.backend.features.preseleccion.service;

import java.math.BigDecimal;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.comedoruagrm.backend.features.preseleccion.dto.PuntajeCalculado;
import com.comedoruagrm.backend.features.preseleccion.dto.SolicitudPuntaje;
import com.comedoruagrm.backend.features.preseleccion.exception.IaDatosInvalidosException;
import com.comedoruagrm.backend.features.preseleccion.exception.IaNoDisponibleException;

/**
 * Cliente HTTP del modulo de IA (`comedor-uagrm-ia`, FastAPI): `POST /api/v1/puntaje` con una
 * {@link SolicitudPuntaje} y respuesta {@link PuntajeCalculado}.
 *
 * * La direccion y el tiempo de espera salen de `app.ia.base-url` y `app.ia.timeout-segundos` (el calculo
 * * individual debe completarse en 5 s).
 * * Contrato con la tarea B3 (la API del modelo): ver docs/Implementacion/CU02. Mientras B3 no exista, el modulo
 * * de IA no tiene esa ruta: la llamada falla, la postulacion queda `pendiente` y el reintento automatico la
 * * toma cuando la ruta aparezca.
 * ! No se loguea el cuerpo de la solicitud ni el de la respuesta.
 */
@Component
public class IaHttpPrecalificacionClient implements PrecalificacionClient {

    private static final String RUTA = "/api/v1/puntaje";
    /** El tope del puntaje de la IA: la base lo limita a 0-70 (el auditor suma hasta 30 mas en el CU07). */
    private static final BigDecimal MAXIMO = BigDecimal.valueOf(70);

    private final RestClient cliente;

    public IaHttpPrecalificacionClient(@Value("${app.ia.base-url:http://localhost:8000}") String baseUrl,
            @Value("${app.ia.timeout-segundos:5}") long timeoutSegundos) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(Math.min(2, timeoutSegundos)));
        fabrica.setReadTimeout(Duration.ofSeconds(timeoutSegundos));
        this.cliente = RestClient.builder().baseUrl(baseUrl).requestFactory(fabrica).build();
    }

    @Override
    public PuntajeCalculado calcular(SolicitudPuntaje solicitud) {
        PuntajeCalculado respuesta;
        try {
            respuesta = cliente.post().uri(RUTA).body(solicitud).retrieve().body(PuntajeCalculado.class);
        } catch (HttpClientErrorException e) {
            // 4xx de la IA: o rechazo los datos (422) o la ruta aun no existe (404, B3 pendiente).
            if (e.getStatusCode().value() == 404) {
                throw new IaNoDisponibleException("El modulo de IA todavia no expone el calculo del puntaje", e);
            }
            throw new IaDatosInvalidosException("El modulo de IA rechazo los datos (HTTP "
                    + e.getStatusCode().value() + ")");
        } catch (RestClientException e) {
            throw new IaNoDisponibleException("El modulo de IA no respondio", e);
        }
        if (respuesta == null || respuesta.puntaje() == null || respuesta.modelo() == null
                || respuesta.puntaje().signum() < 0 || respuesta.puntaje().compareTo(MAXIMO) > 0) {
            throw new IaDatosInvalidosException("El modulo de IA devolvio una respuesta sin puntaje valido");
        }
        return respuesta;
    }
}
