package com.b4rrhh.geo.territory.domain.port;

import com.b4rrhh.geo.territory.domain.model.Municipality;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MunicipalityRepository {

    /**
     * Los municipios de un país con una palabra del nombre que empieza por {@code name}, sin
     * mirar mayúsculas, tildes ni la barra de los nombres bilingües, vigentes en {@code date}.
     * Primero los que empiezan por {@code name}, luego el resto, cada grupo por nombre.
     */
    List<Municipality> searchByName(String countryCode, String name, LocalDate date, int limit);

    Optional<Municipality> findByCode(String countryCode, String code);
}
