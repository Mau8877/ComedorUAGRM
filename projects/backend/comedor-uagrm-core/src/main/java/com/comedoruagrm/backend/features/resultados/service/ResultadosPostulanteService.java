package com.comedoruagrm.backend.features.resultados.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.features.postulaciones.exception.PriorizacionErrorCodes;
import com.comedoruagrm.backend.features.resultados.dto.AdjudicadoPublicoResponse;
import com.comedoruagrm.backend.features.resultados.dto.ConvocatoriaPublicadaResponse;
import com.comedoruagrm.backend.features.resultados.dto.MiResultadoResponse;
import com.comedoruagrm.backend.features.resultados.dto.PaginaResultados;
import com.comedoruagrm.backend.features.resultados.exception.ResultadosErrorCodes;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.DatosPublicacion;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.Pagina;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.ResultadoPropio;

import lombok.RequiredArgsConstructor;

/**
 * Lo que el postulante consulta una vez publicados los resultados (CU22): las convocatorias publicadas, la
 * lista publica de adjudicados y su propio resultado. Solo lectura; exige sesion valida.
 *
 * * Antes de la publicacion la lista y el resultado propio responden `ERR_PUB_04`: nadie se entera antes de la
 * * publicacion oficial.
 * ! Nunca se devuelven no adjudicados en la lista, ni CI, correo, datos socioeconomicos ni el motivo de «no
 * ! adjudicada»: la consulta del repository ya no los selecciona.
 */
@Service
@RequiredArgsConstructor
public class ResultadosPostulanteService {

    private final ResultadosConsultaRepository consulta;

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public PaginaResultados<ConvocatoriaPublicadaResponse> convocatoriasPublicadas(Pageable pageable) {
        Pagina<ConvocatoriaPublicadaResponse> pagina = consulta.convocatoriasPublicadas(pageable);
        return PaginaResultados.de(pagina.filas(), pagina.total(), pageable);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public PaginaResultados<AdjudicadoPublicoResponse> adjudicados(UUID convocatoriaCodigo, String search,
            Pageable pageable) {
        DatosConvocatoria c = exigirConvocatoria(convocatoriaCodigo);
        exigirPublicacion(c);
        Pagina<AdjudicadoPublicoResponse> pagina = consulta.adjudicadosPublicos(c.id(), search, pageable);
        return PaginaResultados.de(pagina.filas(), pagina.total(), pageable);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public MiResultadoResponse miResultado(UUID convocatoriaCodigo, UUID usuarioCodigo) {
        DatosConvocatoria c = exigirConvocatoria(convocatoriaCodigo);
        DatosPublicacion publicacion = exigirPublicacion(c);
        return consulta.miResultado(c.id(), usuarioCodigo)
                .map((ResultadoPropio r) -> new MiResultadoResponse(r.resultado(), r.nota(),
                        publicacion.publicadoEn()))
                .orElseGet(() -> new MiResultadoResponse(null, null, publicacion.publicadoEn()));
    }

    private DatosConvocatoria exigirConvocatoria(UUID codigo) {
        return consulta.convocatoria(codigo).orElseThrow(
                () -> new NotFoundException(PriorizacionErrorCodes.ERR_PRI_01, "La convocatoria no existe"));
    }

    private DatosPublicacion exigirPublicacion(DatosConvocatoria c) {
        return consulta.publicacion(c.id()).orElseThrow(() -> new NotFoundException(
                ResultadosErrorCodes.ERR_PUB_04, "Los resultados de la convocatoria todavía no fueron publicados"));
    }
}
