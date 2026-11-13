package com.comedoruagrm.backend.features.notificaciones.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.Objects;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.features.notificaciones.enums.EventoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.repository.NotificacionLoteRepository;
import com.comedoruagrm.backend.features.notificaciones.repository.NotificacionLoteRepository.AvisoGuardado;
import com.comedoruagrm.backend.features.notificaciones.repository.NotificacionLoteRepository.NuevoAviso;
import com.comedoruagrm.backend.features.notificaciones.service.PlantillaNotificacion.NotificacionRenderizada;

import lombok.RequiredArgsConstructor;

/**
 * Avisos a muchos usuarios, cada uno con su evento y sus datos (CU22: publicar resultados). Complementa a
 * {@link NotificacionService}, que avisa de a uno.
 *
 * * Guarda los avisos en la bandeja por lotes de 500 (una sentencia por lote) dentro de la transaccion de
 * * quien llama, y publica un evento por lote que `DespachoLoteNotificaciones` atiende despues del commit y
 * * en otro hilo: un rollback no deja avisos ni pushes fantasma y un envio lento no demora la operacion.
 * * Un fallo de envio nunca deshace lo guardado: el aviso queda en la bandeja con `enviada_en` nulo.
 * ! Si la plantilla de un evento esta incompleta se lanza la excepcion: es un error de programacion y no se
 * ! debe publicar a medias.
 */
@Service
@RequiredArgsConstructor
public class NotificacionLoteService {

    /** Cuantos avisos entran en cada sentencia y en cada evento de despacho. */
    static final int LOTE = 500;

    private final NotificacionLoteRepository loteRepository;
    private final PlantillaNotificacion plantilla;
    private final ApplicationEventPublisher publicador;

    /** Un aviso a un usuario: el evento y los valores de los marcadores de su plantilla. */
    public record AvisoParaUsuario(long usuarioId, String correo, EventoNotificacion evento,
            Map<String, String> datos, Long postulacionId) {
    }

    /** Guarda los avisos por lotes y programa su despacho; devuelve cuantos se guardaron. */
    @Transactional
    public int notificarLote(List<AvisoParaUsuario> avisos) {
        int guardados = 0;
        for (int desde = 0; desde < avisos.size(); desde += LOTE) {
            List<AvisoParaUsuario> tramo = avisos.subList(desde, Math.min(desde + LOTE, avisos.size()));
            List<NotificacionRenderizada> textos = new ArrayList<>(tramo.size());
            List<NuevoAviso> nuevos = new ArrayList<>(tramo.size());
            for (AvisoParaUsuario a : tramo) {
                NotificacionRenderizada texto = plantilla.renderizar(a.evento(), a.datos());
                textos.add(texto);
                nuevos.add(new NuevoAviso(a.usuarioId(), texto.titulo(), texto.mensaje(), texto.tipo(),
                        a.postulacionId()));
            }
            List<AvisoGuardado> filas = loteRepository.insertar(nuevos);
            Map<Long, UUID> postulaciones = loteRepository.codigosDePostulaciones(tramo.stream()
                    .map(AvisoParaUsuario::postulacionId).filter(Objects::nonNull).collect(Collectors.toSet()));
            List<NotificacionGuardada> paraDespachar = new ArrayList<>(tramo.size());
            for (int i = 0; i < tramo.size(); i++) {
                AvisoParaUsuario a = tramo.get(i);
                AvisoGuardado fila = filas.get(i);
                paraDespachar.add(new NotificacionGuardada(fila.id(), fila.codigo(), a.usuarioId(), a.correo(),
                        textos.get(i).titulo(), textos.get(i).mensaje(), a.evento(),
                        a.postulacionId() == null ? null : postulaciones.get(a.postulacionId())));
            }
            publicador.publishEvent(new LoteNotificacionesGuardadas(paraDespachar));
            guardados += tramo.size();
        }
        return guardados;
    }
}
