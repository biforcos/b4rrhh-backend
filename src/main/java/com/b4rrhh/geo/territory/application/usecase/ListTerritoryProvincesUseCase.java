package com.b4rrhh.geo.territory.application.usecase;

import com.b4rrhh.geo.territory.domain.model.Province;

import java.util.List;

public interface ListTerritoryProvincesUseCase {

    List<Province> list(String ruleSystemCode);
}
