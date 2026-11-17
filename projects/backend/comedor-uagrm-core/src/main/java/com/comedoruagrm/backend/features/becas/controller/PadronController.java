package com.comedoruagrm.backend.features.becas.controller;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.common.util.ParametrosListado;
import com.comedoruagrm.backend.features.becas.dto.AltaPadronResponse;
import com.comedoruagrm.backend.features.becas.dto.BecaResponse;
import com.comedoruagrm.backend.features.becas.dto.ResumenPadronResponse;
import com.comedoruagrm.backend.features.becas.enums.EstadoBeca;
import com.comedoruagrm.backend.features.becas.service.PadronService;
import com.comedoruagrm.backend.features.becas.service.PadronService.PaginaPadron;
import com.comedoruagrm.backend.security.CurrentUserId;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * El padron de becados de una convocatoria (CU23): resumen, listado y alta de los adjudicados. El rol
 * ADMINISTRADOR_DUBSS lo comprueba el `service`; el administrador que actua sale del token (`@CurrentUserId`).
 */
@RestController
@RequestMapping("/api/v1/convocatorias/{codigo}/padron")
@RequiredArgsConstructor
public class PadronController {

    private static final Set<String> FILTROS_SOPORTADOS = Set.of("filter[estado]");

    private final PadronService padronService;

    @GetMapping("/resumen")
    public ResponseEntity<ApiResponse<ResumenPadronResponse>> resumen(@PathVariable UUID codigo) {
        return ResponseEntity.ok(ApiResponse.success(padronService.resumen(codigo), "Resumen del padrón obtenido correctamente"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BecaResponse>>> listar(
            @PathVariable UUID codigo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(name = "filter[estado]", required = false) String estado,
            @RequestParam(required = false) String sort,
            HttpServletRequest request) {
        ParametrosListado.validarFiltros(request, FILTROS_SOPORTADOS);
        PaginaPadron pagina = padronService.listar(codigo, page, pageSize, search, aEstado(estado), sort);
        return ResponseEntity.ok(ApiResponse.successList(pagina.datos(), "Padrón obtenido correctamente",
                pagina.meta()));
    }

    @PostMapping("/alta")
    public ResponseEntity<ApiResponse<AltaPadronResponse>> darDeAlta(@CurrentUserId UUID actorCodigo,
            @PathVariable UUID codigo) {
        AltaPadronResponse alta = padronService.darDeAlta(actorCodigo, codigo).datos();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(alta, "Adjudicados dados de alta correctamente"));
    }

    private EstadoBeca aEstado(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return EstadoBeca.valueOf(valor.trim());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("ERR_SYS_05", "filter[estado] debe ser VIGENTE, SUSPENDIDA o BAJA");
        }
    }
}
