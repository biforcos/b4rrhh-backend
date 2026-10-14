package com.b4rrhh.geo.territory.infrastructure.web;

import com.b4rrhh.geo.territory.application.usecase.CheckPostalCodeUseCase;
import com.b4rrhh.geo.territory.application.usecase.DescribeTerritoryOfCountryUseCase;
import com.b4rrhh.geo.territory.application.usecase.ListTerritoryCountriesUseCase;
import com.b4rrhh.geo.territory.application.usecase.ListTerritoryProvincesUseCase;
import com.b4rrhh.geo.territory.application.usecase.ListTerritoryStreetTypesUseCase;
import com.b4rrhh.geo.territory.application.usecase.SearchMunicipalitiesQuery;
import com.b4rrhh.geo.territory.application.usecase.SearchMunicipalitiesUseCase;
import com.b4rrhh.geo.territory.infrastructure.web.dto.TerritoryCountryResponse;
import com.b4rrhh.geo.territory.infrastructure.web.dto.TerritoryMunicipalityResponse;
import com.b4rrhh.geo.territory.infrastructure.web.dto.TerritoryOfCountryResponse;
import com.b4rrhh.geo.territory.infrastructure.web.dto.TerritoryPostalCodeResponse;
import com.b4rrhh.geo.territory.infrastructure.web.dto.TerritoryProvinceResponse;
import com.b4rrhh.geo.territory.infrastructure.web.dto.TerritoryStreetTypeResponse;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * La API de consulta del territorio (backend#154, ADR-078). Sólo lectura: lo que es catálogo
 * se mantiene por migración y lo que es {@code geo} se regenera de su fuente.
 */
@RestController
@RequestMapping("/territory")
public class TerritoryBusinessKeyController {

    private final ListTerritoryCountriesUseCase listCountries;
    private final DescribeTerritoryOfCountryUseCase describeTerritory;
    private final ListTerritoryProvincesUseCase listProvinces;
    private final ListTerritoryStreetTypesUseCase listStreetTypes;
    private final SearchMunicipalitiesUseCase searchMunicipalities;
    private final CheckPostalCodeUseCase checkPostalCode;
    private final TerritoryResponseAssembler assembler;

    public TerritoryBusinessKeyController(
            ListTerritoryCountriesUseCase listCountries,
            DescribeTerritoryOfCountryUseCase describeTerritory,
            ListTerritoryProvincesUseCase listProvinces,
            ListTerritoryStreetTypesUseCase listStreetTypes,
            SearchMunicipalitiesUseCase searchMunicipalities,
            CheckPostalCodeUseCase checkPostalCode,
            TerritoryResponseAssembler assembler
    ) {
        this.listCountries = listCountries;
        this.describeTerritory = describeTerritory;
        this.listProvinces = listProvinces;
        this.listStreetTypes = listStreetTypes;
        this.searchMunicipalities = searchMunicipalities;
        this.checkPostalCode = checkPostalCode;
        this.assembler = assembler;
    }

    /** Los países los ve una reglamentación, desde su capa de nivel 2. */
    @GetMapping("/{ruleSystemCode}/countries")
    public List<TerritoryCountryResponse> countries(@PathVariable String ruleSystemCode, ResponseLanguage language) {
        return listCountries.list(ruleSystemCode, language.code()).stream().map(assembler::toResponse).toList();
    }

    /**
     * Lo demás es de un país, sea quien sea quien pregunte (backend#169): un empleado de FRA que vive
     * en Valencia encuentra las provincias de España.
     */
    @GetMapping("/{countryCode}")
    public TerritoryOfCountryResponse territory(@PathVariable String countryCode) {
        return assembler.toResponse(describeTerritory.describe(countryCode));
    }

    @GetMapping("/{countryCode}/provinces")
    public List<TerritoryProvinceResponse> provinces(@PathVariable String countryCode) {
        return listProvinces.list(countryCode).stream().map(assembler::toResponse).toList();
    }

    @GetMapping("/{countryCode}/street-types")
    public List<TerritoryStreetTypeResponse> streetTypes(@PathVariable String countryCode) {
        return listStreetTypes.list(countryCode).stream().map(assembler::toResponse).toList();
    }

    @GetMapping("/{countryCode}/municipalities")
    public List<TerritoryMunicipalityResponse> municipalities(
            @PathVariable String countryCode,
            @RequestParam String name,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) Integer limit
    ) {
        return searchMunicipalities
                .search(new SearchMunicipalitiesQuery(countryCode, name, date, limit))
                .stream().map(assembler::toResponse).toList();
    }

    @GetMapping("/{countryCode}/postal-codes/{postalCode}")
    public TerritoryPostalCodeResponse postalCode(@PathVariable String countryCode, @PathVariable String postalCode) {
        return assembler.toResponse(checkPostalCode.check(countryCode, postalCode));
    }
}
