package com.b4rrhh.geo.territory.application.usecase;

import com.b4rrhh.geo.territory.domain.model.PostalCodeCheck;

public interface CheckPostalCodeUseCase {

    PostalCodeCheck check(String ruleSystemCode, String countryCode, String postalCode);
}
