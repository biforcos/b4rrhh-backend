package com.b4rrhh.geo.territory.application.usecase;

import com.b4rrhh.geo.territory.domain.model.LocatedMunicipality;

import java.util.List;

public interface SearchMunicipalitiesUseCase {

    List<LocatedMunicipality> search(SearchMunicipalitiesQuery query);
}
