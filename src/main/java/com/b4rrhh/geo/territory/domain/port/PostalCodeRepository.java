package com.b4rrhh.geo.territory.domain.port;

import java.util.Optional;

public interface PostalCodeRepository {

    /** El código del municipio que sugiere la fuente para ese código postal, si sugiere uno. */
    Optional<String> findSuggestedMunicipalityCode(String countryCode, String postalCode);

    /** Si el código postal está en el maestro, sugiera municipio o no (backend#155). */
    boolean exists(String countryCode, String postalCode);
}
