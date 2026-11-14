package com.comedoruagrm.backend.features.bitacora.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.features.bitacora.model.Bitacora;
import com.comedoruagrm.backend.features.bitacora.repository.BitacoraRepository;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BitacoraService {

    private static final Logger LOG = LoggerFactory.getLogger(BitacoraService.class);

    // Solo IPs literales (v4/v6): InetAddress.getByName con un nombre haria una
    // consulta DNS, y la IP viene de request.getRemoteAddr() que ya es literal.
    private static final Pattern IP_LITERAL = Pattern.compile("^[0-9a-fA-F:.]+$");

    private final BitacoraRepository bitacoraRepository;
    private final EntityManager entityManager;

    /**
     * Anota un evento en la bitacora, dentro de la transaccion de quien llama.
     * El log no incluye datos sensibles: solo la accion y el codigo del usuario.
     *
     * * Para los eventos de un metodo de service se prefiere `@Auditar`; esta
     * * llamada directa queda para los que no encajan en esa anotacion.
     */
    @Transactional
    public void registrar(Usuario usuario, String accion, String entidad, Long entidadId,
            Map<String, Object> detalle, String ip) {
        guardar(usuario, accion, entidad, entidadId, detalle, ip,
                usuario != null ? usuario.getCodigo() : null);
    }

    /**
     * Igual que {@link #registrar(Usuario, String, String, Long, Map, String)} pero
     * con el actor identificado por su `id`: usa una referencia sin consultar la base.
     * Es la que usa el aspecto de `@Auditar`.
     */
    @Transactional
    public void registrar(Long actorId, UUID actorCodigo, String accion, String entidad, Long entidadId,
            Map<String, Object> detalle, String ip) {
        Usuario actor = actorId != null ? entityManager.getReference(Usuario.class, actorId) : null;
        guardar(actor, accion, entidad, entidadId, detalle, ip, actorCodigo);
    }

    private void guardar(Usuario usuario, String accion, String entidad, Long entidadId,
            Map<String, Object> detalle, String ip, UUID actorCodigo) {
        bitacoraRepository.save(new Bitacora(usuario, accion, entidad, entidadId, detalle, aInetAddress(ip)));
        LOG.info("Evento registrado en la bitacora",
                kv("module", "bitacora"),
                kv("accion", accion),
                kv("userId", actorCodigo));
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
