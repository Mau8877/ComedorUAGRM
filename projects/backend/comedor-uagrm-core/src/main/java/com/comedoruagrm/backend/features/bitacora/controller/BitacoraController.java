package com.comedoruagrm.backend.features.bitacora.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.common.util.ParametrosListado;
import com.comedoruagrm.backend.features.bitacora.dto.BitacoraResponse;
import com.comedoruagrm.backend.features.bitacora.service.BitacoraConsultaService;
import com.comedoruagrm.backend.features.bitacora.service.BitacoraConsultaService.PaginaBitacora;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Consulta de la bitacora (CU25). Solo lectura: no hay POST, PUT, PATCH ni DELETE. El rol
 * ADMINISTRADOR_DUBSS lo comprueba el `service`.
 */
@RestController
@RequestMapping("/api/v1/bitacora")
@RequiredArgsConstructor
public class BitacoraController {

    private static final Set<String> FILTROS_SOPORTADOS = Set.of("filter[accion]", "filter[desde]", "filter[hasta]");

    private final BitacoraConsultaService consultaService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BitacoraResponse>>> listar(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(name = "filter[accion]", required = false) String accion,
            @RequestParam(name = "filter[desde]", required = false) String desde,
            @RequestParam(name = "filter[hasta]", required = false) String hasta,
            @RequestParam(required = false) String sort,
            HttpServletRequest request) {
        ParametrosListado.validarFiltros(request, FILTROS_SOPORTADOS);
        PaginaBitacora pagina = consultaService.listar(page, pageSize, search, accion,
                aFecha(desde, "filter[desde]"), aFecha(hasta, "filter[hasta]"), sort);
        return ResponseEntity.ok(
                ApiResponse.successList(pagina.datos(), "Bitácora obtenida correctamente", pagina.meta()));
    }

    /** Las acciones distintas registradas, para el filtro de acciones. */
    @GetMapping("/acciones")
    public ResponseEntity<ApiResponse<List<String>>> acciones() {
        return ResponseEntity.ok(ApiResponse.success(consultaService.acciones(), "Acciones obtenidas correctamente"));
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
