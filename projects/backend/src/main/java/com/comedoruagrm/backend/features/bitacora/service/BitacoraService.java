package com.comedoruagrm.backend.features.bitacora.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.features.bitacora.model.Bitacora;
import com.comedoruagrm.backend.features.bitacora.repository.BitacoraRepository;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BitacoraService {

    private static final Logger LOG = LoggerFactory.getLogger(BitacoraService.class);

    // Solo IPs literales (v4/v6): InetAddress.getByName con un nombre haria una
    // consulta DNS, y la IP viene de request.getRemoteAddr() que ya es literal.
    private static final Pattern IP_LITERAL = Pattern.compile("^[0-9a-fA-F:.]+$");

    private final BitacoraRepository bitacoraRepository;

    /**
     * Anota un evento en la bitacora, dentro de la transaccion de quien llama.
     * El log no incluye datos sensibles: solo la accion y el codigo del usuario.
     */
    @Transactional
    public void registrar(Usuario usuario, String accion, String entidad, Long entidadId,
            Map<String, Object> detalle, String ip) {
        bitacoraRepository.save(new Bitacora(usuario, accion, entidad, entidadId, detalle, aInetAddress(ip)));
        LOG.info("Evento registrado en la bitacora",
                kv("module", "bitacora"),
                kv("accion", accion),
                kv("userId", usuario != null ? usuario.getCodigo() : null));
    }

    private InetAddress aInetAddress(String ip) {
        if (ip == null || !IP_LITERAL.matcher(ip).matches()) {
            return null;
        }
        try {
            return InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
