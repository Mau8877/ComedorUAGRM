package com.comedoruagrm.backend.features.asistencias.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.comedoruagrm.backend.features.asistencias.model.Asistencia;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    /** La asistencia de esa beca en ese dia, si ya se registro (el borrado logico lo filtra el modelo). */
    Optional<Asistencia> findByBecaIdAndFecha(Long becaId, LocalDate fecha);

    /**
     * Inserta la asistencia salvo que la beca ya tenga una ese dia (RNF-03): devuelve `1` si la creo y `0` si ya
     * existia. Se apoya en el indice unico parcial `uq_asistencia_beca_fecha`, asi que dos peticiones simultaneas
     * tampoco duplican (la segunda espera a la primera y no inserta).
     */
    @Modifying
    @Query(value = """
            INSERT INTO asistencia (beca_id, registrado_por, fecha, hora_registro)
            VALUES (:becaId, :registradoPor, :fecha, :hora)
            ON CONFLICT (beca_id, fecha) WHERE eliminado_en IS NULL DO NOTHING
            """, nativeQuery = true)
    int insertarSiNoExiste(@Param("becaId") long becaId, @Param("registradoPor") long registradoPor,
            @Param("fecha") LocalDate fecha, @Param("hora") Instant hora);
}
