package com.comedoruagrm.backend.features.usuarios.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.comedoruagrm.backend.features.usuarios.model.Usuario;

import jakarta.persistence.LockModeType;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByCodigo(UUID codigo);

    boolean existsByCi(String ci);

    boolean existsByCorreo(String correo);

    // * "Personal" = cualquier rol distinto de POSTULANTE. En JPQL el literal es
    // * el valor de RolNombres.POSTULANTE (una anotacion no puede leer una
    // * constante de otra clase dentro del texto del query).
    // * `busqueda` ya llega en minusculas y con los comodines (`%texto%`) o null;
    // * se compara contra nombres, apellidos y CI.

    /** Cuentas del personal paginadas, con filtros opcionales (cualquier parametro en null no filtra). */
    @Query("""
            select u from Usuario u
            where u.rol.nombre <> 'POSTULANTE'
              and (:rol is null or u.rol.nombre = :rol)
              and (:activo is null or u.activo = :activo)
              and (:busqueda is null
                   or lower(u.nombres) like :busqueda
                   or lower(u.apellidos) like :busqueda
                   or u.ci like :busqueda)
            """)
    Page<Usuario> buscarPersonal(@Param("busqueda") String busqueda, @Param("rol") String rol,
            @Param("activo") Boolean activo, Pageable pageable);

    /** La cuenta del personal con ese codigo; vacio si no existe, esta eliminada o es de un Postulante. */
    @Query("select u from Usuario u where u.codigo = :codigo and u.rol.nombre <> 'POSTULANTE'")
    Optional<Usuario> findPersonalByCodigo(@Param("codigo") UUID codigo);

    /**
     * Los Administradores DUBSS activos, con las filas bloqueadas hasta el fin de
     * la transaccion: quien cuenta cuantos quedan antes de desactivar a uno no se
     * pisa con otra operacion simultanea (dos desactivaciones a la vez no pueden
     * dejar a cero). Se llama siempre dentro de una transaccion.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.rol.nombre = 'ADMINISTRADOR_DUBSS' and u.activo = true")
    List<Usuario> bloquearAdministradoresActivos();
}
