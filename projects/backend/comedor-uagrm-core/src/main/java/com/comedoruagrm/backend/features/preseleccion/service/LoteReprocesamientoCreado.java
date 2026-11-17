package com.comedoruagrm.backend.features.preseleccion.service;

/**
 * Se publica dentro de la transaccion que crea el lote (CU26): el reprocesamiento arranca recien cuando esa
 * transaccion confirma (`ReprocesamientoEjecucionService`), para que el lote ya sea visible.
 */
public record LoteReprocesamientoCreado(long loteId) {
}
