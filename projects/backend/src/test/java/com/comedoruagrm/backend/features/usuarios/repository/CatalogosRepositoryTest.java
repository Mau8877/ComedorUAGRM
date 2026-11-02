package com.comedoruagrm.backend.features.usuarios.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.features.usuarios.RolNombres;
import com.comedoruagrm.backend.features.usuarios.model.Carrera;
import com.comedoruagrm.backend.features.usuarios.model.Rol;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CatalogosRepositoryTest {

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private CarreraRepository carreraRepository;

    @BeforeAll
    static void saltarSiNoHayPostgres() {
        PostgresTestDatabase.asegurarDisponible();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.registrar(registry);
    }

    @Test
    void findByNombre_conPostulante_devuelveElRolSembradoPorLaMigracion() {
        Rol rol = rolRepository.findByNombre(RolNombres.POSTULANTE).orElseThrow();

        assertThat(rol.getNombre()).isEqualTo("POSTULANTE");
        assertThat(rol.getCodigo()).isNotNull();
        assertThat(rol.getId()).isNotNull();
    }

    @Test
    void findByNombre_conLosCuatroRoles_losEncuentraATodos() {
        assertThat(rolRepository.findByNombre(RolNombres.ADMINISTRADOR_DUBSS)).isPresent();
        assertThat(rolRepository.findByNombre(RolNombres.DIRECTIVO)).isPresent();
        assertThat(rolRepository.findByNombre(RolNombres.PERSONAL_COMEDOR)).isPresent();
    }

    @Test
    void findAll_conPaginacion_devuelveLasCarrerasCargadasPorLaMigracion() {
        var pagina = carreraRepository.findAll(PageRequest.of(0, 200));

        assertThat(pagina.getTotalElements()).isEqualTo(71);
        assertThat(pagina.getContent()).allSatisfy(c -> {
            assertThat(c.getNombre()).isNotBlank();
            assertThat(c.getFacultad()).isNotBlank();
        });
    }

    @Test
    void findByCodigo_conElCodigoDeUnaCarrera_laEncuentra() {
        Carrera carrera = carreraRepository.findAll(PageRequest.of(0, 1)).getContent().get(0);

        assertThat(carreraRepository.findByCodigo(carrera.getCodigo())).isPresent();
    }
}
