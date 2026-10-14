package com.b4rrhh.geo.territory.application.usecase;

import com.b4rrhh.geo.territory.domain.model.StreetType;

import java.util.List;

public interface ListTerritoryStreetTypesUseCase {

    /** Los del país; vacío si no tiene territorio (backend#169). */
    List<StreetType> list(String countryCode);
}
