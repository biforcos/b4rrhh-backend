package com.b4rrhh.geo.territory.application.usecase;

import com.b4rrhh.geo.territory.domain.model.CountryTerritory;

public interface DescribeTerritoryOfCountryUseCase {

    CountryTerritory describe(String countryCode);
}
