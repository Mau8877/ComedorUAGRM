package com.comedoruagrm.backend.features.mibeca.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.common.util.ParametrosListado;
import com.comedoruagrm.backend.features.mibeca.dto.AsistenciaBecadoResponse;
import com.comedoruagrm.backend.features.mibeca.dto.MiBecaResponse;
import com.comedoruagrm.backend.features.mibeca.service.MiBecaService;
import com.comedoruagrm.backend.features.mibeca.service.MiBecaService.PaginaAsistencias;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * El becado consulta su beca y su historial de asistencia (CU24). Solo lectura: no hay POST, PUT, PATCH ni DELETE.
 * El rol POSTULANTE lo comprueba el `service`; la beca es siempre la del usuario del token (`@CurrentUserId`).
 */
@RestController
@RequestMapping("/api/v1/mi-beca")
@RequiredArgsConstructor
public class MiBecaController {

    private static final Set<String> FILTROS_SOPORTADOS = Set.of("filter[desde]", "filter[hasta]");

    private final MiBecaService service;

    @GetMapping
    public ResponseEntity<ApiResponse<MiBecaResponse>> miBeca(@CurrentUserId UUID usuarioCodigo) {
        return ResponseEntity.ok(ApiResponse.success(service.miBeca(usuarioCodigo), "Beca obtenida correctamente"));
    }

    @GetMapping("/asistencias")
    public ResponseEntity<ApiResponse<List<AsistenciaBecadoResponse>>> asistencias(
            @CurrentUserId UUID usuarioCodigo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(name = "filter[desde]", required = false) String desde,
            @RequestParam(name = "filter[hasta]", required = false) String hasta,
            @RequestParam(required = false) String sort,
            HttpServletRequest request) {
        ParametrosListado.validarFiltros(request, FILTROS_SOPORTADOS);
        PaginaAsistencias pagina = service.asistencias(usuarioCodigo, page, pageSize,
                aFecha(desde, "filter[desde]"), aFecha(hasta, "filter[hasta]"), sort);
        return ResponseEntity.ok(
                ApiResponse.successList(pagina.datos(), "Historial de asistencia obtenido correctamente",
                        pagina.meta()));
    }

    private LocalDate aFecha(String valor, String filtro) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("ERR_SYS_05", filtro + " debe tener el formato AAAA-MM-DD");
        }
    }
}
