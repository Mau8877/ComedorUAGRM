package com.comedoruagrm.backend.features.postulaciones.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.features.postulaciones.enums.EstadoPostulacion;
import com.comedoruagrm.backend.features.postulaciones.model.Postulacion;
import com.comedoruagrm.backend.features.postulaciones.model.PostulacionHistorial;
import com.comedoruagrm.backend.features.postulaciones.repository.PostulacionHistorialRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.PostulacionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Pasa una postulacion de «pendiente» a «calificada» cuando el CU03 registra su puntaje, y deja la fila en el
 * historial (sin usuario: lo hizo el sistema). Es el unico punto por el que otro modulo cambia ese estado:
 * el modulo de preseleccion no lee ni escribe el repository de postulaciones.
 *
 * * Se une a la transaccion de quien llama (la que registra el puntaje), asi el puntaje y el estado se
 * * confirman o se deshacen juntos.
 */
@Service
@RequiredArgsConstructor
public class CalificacionPostulacionService {

    static final String MOTIVO = "Precalificación automática";

    private final PostulacionRepository postulaciones;
    private final PostulacionHistorialRepository historial;

    @Transactional
    public void marcarCalificada(long postulacionId) {
        Postulacion postulacion = postulaciones.findById(postulacionId).orElseThrow();
        postulacion.calificar();
        historial.saveAndFlush(new PostulacionHistorial(postulacionId, null, EstadoPostulacion.pendiente,
                EstadoPostulacion.calificada, MOTIVO));
    }
}
