package com.comedoruagrm.backend.features.postulaciones.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.response.PageMeta;
import com.comedoruagrm.backend.features.postulaciones.dto.ConvocatoriaPriorizableResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.PostulacionPriorizadaResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.ResumenListaPriorizadaResponse;
import com.comedoruagrm.backend.features.postulaciones.exception.PriorizacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.repository.ListaPriorizadaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Lista priorizada de postulantes (CU06): convocatorias consultables, resumen y la lista paginada
 * ordenada por puntaje vigente. Solo el Administrador DUBSS la consulta.
 *
 * * Son lecturas: no se loguean ni se anotan en la bitacora (solo la exportacion, ver
 * * ListaPriorizadaPdfService) y no modifican el estado de ninguna postulacion.
 * * El orden es fijo (puntaje descendente): un `sort` recibido se rechaza (ERR_PRI_02).
 */
@Service
@RequiredArgsConstructor
public class ListaPriorizadaService {

    private final ListaPriorizadaRepository repository;

    /** Una pagina de la lista junto con su bloque `meta`. */
    public record PaginaListaPriorizada(List<PostulacionPriorizadaResponse> datos, PageMeta meta) {
    }

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public List<ConvocatoriaPriorizableResponse> listarConvocatorias() {
        return repository.listarConvocatorias();
    }

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public ResumenListaPriorizadaResponse resumir(UUID convocatoriaCodigo) {
        exigirConvocatoria(convocatoriaCodigo);
        return new ResumenListaPriorizadaResponse(
                repository.contar(convocatoriaCodigo, null, null),
                repository.contarPendientes(convocatoriaCodigo));
    }

    /** `page` es 1-indexed. `carreraCodigo` y `search` son opcionales. */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public PaginaListaPriorizada listar(UUID convocatoriaCodigo, UUID carreraCodigo, String search, int page,
            int pageSize, String sort) {
        if (page < 1 || pageSize < 1) {
            throw new ValidationException("ERR_SYS_01", "La página y el tamaño de página deben ser mayores a cero");
        }
        if (sort != null && !sort.isBlank()) {
            throw new ValidationException(PriorizacionErrorCodes.ERR_PRI_02,
                    "La lista priorizada siempre va ordenada por puntaje de mayor a menor: no admite «sort»");
        }
        exigirConvocatoria(convocatoriaCodigo);

        String busqueda = patronDeBusqueda(search);
        long total = repository.contar(convocatoriaCodigo, carreraCodigo, busqueda);
        long desplazamiento = (long) (page - 1) * pageSize;
        List<PostulacionPriorizadaResponse> datos = desplazamiento >= total
                ? List.of()
                : repository.buscar(convocatoriaCodigo, carreraCodigo, busqueda, pageSize, desplazamiento);
        int totalPaginas = (int) ((total + pageSize - 1) / pageSize);
        return new PaginaListaPriorizada(datos, new PageMeta(page, pageSize, total, totalPaginas));
    }

    void exigirConvocatoria(UUID convocatoriaCodigo) {
        if (repository.buscarConvocatoria(convocatoriaCodigo).isEmpty()) {
            throw new NotFoundException(PriorizacionErrorCodes.ERR_PRI_01, "La convocatoria no existe");
        }
    }

    /**
     * `%texto%` en minusculas, con `%`, `_` y `\` escapados para que el texto buscado se tome
     * literal; o null si no hay texto. Package-private: lo usa tambien la exportacion a PDF.
     */
    static String patronDeBusqueda(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String texto = search.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + texto + "%";
    }
}
