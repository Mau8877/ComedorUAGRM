package com.comedoruagrm.backend.features.usuarios.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.response.PageMeta;
import com.comedoruagrm.backend.features.usuarios.dto.CarreraResponse;
import com.comedoruagrm.backend.features.usuarios.model.Carrera;
import com.comedoruagrm.backend.features.usuarios.repository.CarreraRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CarreraService {

    private final CarreraRepository carreraRepository;

    /** Resultado de una pagina de carreras junto con su bloque `meta`. */
    public record PaginaCarreras(List<CarreraResponse> datos, PageMeta meta) {
    }

    /** `page` es 1-indexed; el listado va ordenado por nombre. Las lecturas no se loguean. */
    @Transactional(readOnly = true)
    public PaginaCarreras listar(int page, int pageSize) {
        if (page < 1 || pageSize < 1) {
            throw new ValidationException("ERR_SYS_01", "La página y el tamaño de página deben ser mayores a cero");
        }

        Page<Carrera> pagina = carreraRepository.findAll(
                PageRequest.of(page - 1, pageSize, Sort.sort(Carrera.class).by(Carrera::getNombre)));

        List<CarreraResponse> datos = pagina.getContent().stream()
                .map(c -> new CarreraResponse(c.getCodigo(), c.getNombre(), c.getFacultad()))
                .toList();
        PageMeta meta = new PageMeta(page, pageSize, pagina.getTotalElements(), pagina.getTotalPages());
        return new PaginaCarreras(datos, meta);
    }
}
