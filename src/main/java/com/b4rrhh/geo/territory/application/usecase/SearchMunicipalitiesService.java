package com.b4rrhh.geo.territory.application.usecase;

import com.b4rrhh.geo.territory.application.port.TerritoryCatalogPort;
import com.b4rrhh.geo.territory.domain.exception.InvalidTerritoryQueryException;
import com.b4rrhh.geo.territory.domain.model.LocatedMunicipality;
import com.b4rrhh.geo.territory.domain.model.Province;
import com.b4rrhh.geo.territory.domain.port.MunicipalityRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * La búsqueda del autocompletado de municipio (backend#154): por nombre, dentro de un país y
 * vigentes en una fecha, con su provincia y su comunidad.
 */
@Service
public class SearchMunicipalitiesService implements SearchMunicipalitiesUseCase {

    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 50;
    static final int MIN_NAME_LENGTH = 2;

    private final TerritoryCatalogPort catalog;
    private final MunicipalityRepository municipalities;

    public SearchMunicipalitiesService(TerritoryCatalogPort catalog, MunicipalityRepository municipalities) {
        this.catalog = catalog;
        this.municipalities = municipalities;
    }

    @Override
    public List<LocatedMunicipality> search(SearchMunicipalitiesQuery query) {
        String ruleSystemCode = TerritoryRuleSystem.require(catalog, query.ruleSystemCode());
        String name = query.name() == null ? "" : query.name().strip();
        if (name.length() < MIN_NAME_LENGTH) {
            throw new InvalidTerritoryQueryException(
                    "Para buscar un municipio hacen falta al menos " + MIN_NAME_LENGTH + " letras.");
        }
        int limit = query.limit() == null ? DEFAULT_LIMIT : query.limit();
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new InvalidTerritoryQueryException("El límite va de 1 a " + MAX_LIMIT + ".");
        }
        String countryCode = query.countryCode() == null ? "" : query.countryCode().strip().toUpperCase();
        LocalDate date = query.date() == null ? LocalDate.now() : query.date();

        Map<String, Province> provinces = catalog.provinces(ruleSystemCode).stream()
                .collect(Collectors.toMap(Province::code, Function.identity()));
        return municipalities.searchByName(countryCode, name, date, limit).stream()
                .map(municipality -> new LocatedMunicipality(municipality, provinces.get(municipality.provinceCode())))
                .toList();
    }
}
