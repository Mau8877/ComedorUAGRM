package com.comedoruagrm.backend.features.usuarios.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.features.usuarios.repository.UsuarioRepository;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;

import lombok.RequiredArgsConstructor;

/**
 * Implementa para `security/` la consulta del usuario por `firebase_uid`. Es
 * transaccional porque corre dentro de un filtro, antes de que exista una
 * sesion de persistencia abierta por la peticion, y el rol se carga de forma
 * perezosa.
 */
@Service
@RequiredArgsConstructor
public class UsuarioAutenticableAdapter implements UsuarioAutenticableProvider {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<DatosUsuario> buscarPorFirebaseUid(String firebaseUid) {
        return usuarioRepository.findByFirebaseUid(firebaseUid)
                .map(u -> new DatosUsuario(u.getCodigo(), u.getRol().getNombre(), u.isActivo()));
    }
}
