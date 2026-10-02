package com.b4rrhh.geo.territory.application.usecase;

import com.b4rrhh.geo.territory.domain.model.StreetType;

import java.util.List;

public interface ListTerritoryStreetTypesUseCase {

    List<StreetType> list(String ruleSystemCode);
}
