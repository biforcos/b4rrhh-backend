package com.b4rrhh.geo.territory.application.usecase;

import com.b4rrhh.geo.territory.domain.model.Country;

import java.util.List;

public interface ListTerritoryCountriesUseCase {

    List<Country> list(String ruleSystemCode, String languageCode);
}
