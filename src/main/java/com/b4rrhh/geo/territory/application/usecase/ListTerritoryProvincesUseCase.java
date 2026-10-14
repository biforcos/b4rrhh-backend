package com.b4rrhh.geo.territory.application.usecase;

import com.b4rrhh.geo.territory.domain.model.Province;

import java.util.List;

public interface ListTerritoryProvincesUseCase {

    /** Las del país; vacío si no tiene territorio (backend#169). */
    List<Province> list(String countryCode);
}
