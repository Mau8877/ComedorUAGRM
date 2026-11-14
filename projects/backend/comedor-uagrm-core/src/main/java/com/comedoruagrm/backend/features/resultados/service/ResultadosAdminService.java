package com.comedoruagrm.backend.features.resultados.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.features.convocatorias.enums.EstadoConvocatoria;
import com.comedoruagrm.backend.features.postulaciones.exception.PriorizacionErrorCodes;
import com.comedoruagrm.backend.features.resultados.dto.ConvocatoriaResultadosResponse;
import com.comedoruagrm.backend.features.resultados.dto.DecisionResultadoResponse;
import com.comedoruagrm.backend.features.resultados.dto.PaginaResultados;
import com.comedoruagrm.backend.features.resultados.dto.ResumenPublicacionResponse;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.Conteos;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.DatosPublicacion;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.FiltroDecisiones;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.Pagina;

import lombok.RequiredArgsConstructor;

/**
 * Lo que el Administrador DUBSS revisa antes de publicar los resultados (CU22): el resumen y la lista de
 * decisiones. Solo lectura.
 *
 * * No expone el motivo de «no adjudicada» (lo ve solo el Administrador en el CU05) ni CI ni correo.
 * * Las lecturas no se loguean (ver LOGGING_BACKEND.md).
 */
@Service
@RequiredArgsConstructor
public class ResultadosAdminService {

    private final ResultadosConsultaRepository consulta;

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public ResumenPublicacionResponse resumen(UUID convocatoriaCodigo) {
        DatosConvocatoria c = exigirConvocatoria(convocatoriaCodigo);
        Conteos conteos = consulta.contarPorEstado(c.id());
        DatosPublicacion publicacion = consulta.publicacion(c.id()).orElse(null);
        long aAvisar = consulta.destinatariosDelAviso(c.id()).size();
        boolean yaPublicada = publicacion != null;
        boolean puedePublicar = EstadoConvocatoria.CERRADA.name().equals(c.estado()) && !yaPublicada
                && conteos.calificadas() == 0;
        return new ResumenPublicacionResponse(
                new ConvocatoriaResultadosResponse(c.codigo(), c.nombre(), c.estado()),
                conteos.adjudicados(), conteos.noAdjudicados(), conteos.calificadas(), conteos.totalFinalistas(),
                aAvisar, yaPublicada, yaPublicada ? publicacion.publicadoEn() : null,
                yaPublicada ? publicacion.publicadoPor() : null, puedePublicar);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public PaginaResultados<DecisionResultadoResponse> decisiones(UUID convocatoriaCodigo, FiltroDecisiones filtro,
            Pageable pageable) {
        DatosConvocatoria c = exigirConvocatoria(convocatoriaCodigo);
        Pagina<DecisionResultadoResponse> pagina = consulta.decisiones(c.id(), filtro, pageable);
        return PaginaResultados.de(pagina.filas(), pagina.total(), pageable);
    }

    private DatosConvocatoria exigirConvocatoria(UUID codigo) {
        return consulta.convocatoria(codigo).orElseThrow(
                () -> new NotFoundException(PriorizacionErrorCodes.ERR_PRI_01, "La convocatoria no existe"));
    }
}
