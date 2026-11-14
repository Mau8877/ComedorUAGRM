package com.comedoruagrm.backend.features.notificaciones.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.features.notificaciones.enums.PlataformaDispositivo;
import com.comedoruagrm.backend.features.notificaciones.model.DispositivoFcm;
import com.comedoruagrm.backend.features.notificaciones.repository.DispositivoFcmRepository;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

import lombok.RequiredArgsConstructor;

/**
 * Telefonos registrados para recibir push (CU-EXTRA-01): registro, baja y limpieza de tokens.
 *
 * * Un token pertenece a un solo usuario a la vez: registrar uno que ya esta a nombre de otra
 * * cuenta lo reasigna (el telefono se compartio). La baja es idempotente.
 * ! Nunca se escribe un token en el log: solo el usuario y cantidades.
 */
@Service
@RequiredArgsConstructor
public class DispositivoFcmService {

    private static final Logger LOG = LoggerFactory.getLogger(DispositivoFcmService.class);

    private final DispositivoFcmRepository dispositivoRepository;
    private final UsuarioService usuarioService;

    /** El telefono registrado, para la respuesta; `creado` distingue el primer registro de uno repetido. */
    public record DispositivoRegistrado(UUID codigo, PlataformaDispositivo plataforma, boolean creado) {
    }

    @Transactional
    public DispositivoRegistrado registrar(UUID usuarioCodigo, String token, PlataformaDispositivo plataforma) {
        Usuario usuario = usuarioService.obtenerPorCodigo(usuarioCodigo);
        DispositivoFcm existente = dispositivoRepository.findByToken(token).orElse(null);

        if (existente == null) {
            DispositivoFcm nuevo = dispositivoRepository.save(new DispositivoFcm(usuario, token, plataforma));
            LOG.info("Telefono registrado para push", kv("module", "notificaciones"),
                    kv("userId", usuario.getCodigo()));
            return new DispositivoRegistrado(nuevo.getCodigo(), nuevo.getPlataforma(), true);
        }
        if (!existente.getUsuario().getId().equals(usuario.getId())) {
            existente.setUsuario(usuario);
            dispositivoRepository.save(existente);
            LOG.info("Telefono reasignado a otra cuenta", kv("module", "notificaciones"),
                    kv("userId", usuario.getCodigo()));
        }
        return new DispositivoRegistrado(existente.getCodigo(), existente.getPlataforma(), false);
    }

    /** Elimina el registro de ese token si es de este usuario; si no existe o es de otro, no hace nada. */
    @Transactional
    public void darDeBaja(UUID usuarioCodigo, String token) {
        Usuario usuario = usuarioService.obtenerPorCodigo(usuarioCodigo);
        dispositivoRepository.findByToken(token)
                .filter(dispositivo -> dispositivo.getUsuario().getId().equals(usuario.getId()))
                .ifPresent(dispositivo -> {
                    dispositivo.setEliminadoEn(Instant.now());
                    dispositivoRepository.save(dispositivo);
                    LOG.info("Telefono dado de baja", kv("module", "notificaciones"),
                            kv("userId", usuario.getCodigo()));
                });
    }

    /** Tokens activos del usuario (uso interno del despacho de avisos). */
    @Transactional(readOnly = true)
    public List<String> tokensDe(Long usuarioId) {
        return dispositivoRepository.findAllByUsuarioId(usuarioId).stream()
                .map(DispositivoFcm::getToken)
                .toList();
    }

    /** Tokens activos de varios usuarios en una sola consulta; los usuarios sin telefono no aparecen en el mapa. */
    @Transactional(readOnly = true)
    public Map<Long, List<String>> tokensDe(Collection<Long> usuarioIds) {
        if (usuarioIds.isEmpty()) {
            return Map.of();
        }
        return dispositivoRepository.findAllByUsuarioIdIn(usuarioIds).stream()
                .collect(Collectors.groupingBy(d -> d.getUsuario().getId(),
                        Collectors.mapping(DispositivoFcm::getToken, Collectors.toList())));
    }

    /** Elimina los tokens que Firebase declaro invalidos (app desinstalada o token vencido). */
    @Transactional
    public void eliminarInvalidos(Collection<String> tokens) {
        if (tokens.isEmpty()) {
            return;
        }
        Instant ahora = Instant.now();
        List<DispositivoFcm> invalidos = dispositivoRepository.findAllByTokenIn(tokens);
        invalidos.forEach(dispositivo -> dispositivo.setEliminadoEn(ahora));
        dispositivoRepository.saveAll(invalidos);
        LOG.info("Telefonos eliminados por token invalido", kv("module", "notificaciones"),
                kv("cantidad", invalidos.size()));
    }
}
