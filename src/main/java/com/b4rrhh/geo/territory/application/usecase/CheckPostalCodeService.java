package com.b4rrhh.geo.territory.application.usecase;

import com.b4rrhh.geo.territory.application.port.TerritoryCatalogPort;
import com.b4rrhh.geo.territory.domain.exception.InvalidTerritoryQueryException;
import com.b4rrhh.geo.territory.domain.model.Municipality;
import com.b4rrhh.geo.territory.domain.model.PostalCodeCheck;
import com.b4rrhh.geo.territory.domain.model.Province;
import com.b4rrhh.geo.territory.domain.port.MunicipalityRepository;
import com.b4rrhh.geo.territory.domain.port.PostalCodeRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * La comprobación de un código postal (backend#154, ADR-078 §4). En España la provincia son
 * las dos primeras cifras, siempre: eso es validación. El municipio es una sugerencia de la
 * fuente, y una sugerencia no valida nada.
 */
@Service
public class CheckPostalCodeService implements CheckPostalCodeUseCase {

    private static final Pattern SPANISH_POSTAL_CODE = Pattern.compile("\\d{5}");

    private final TerritoryCatalogPort catalog;
    private final PostalCodeRepository postalCodes;
    private final MunicipalityRepository municipalities;

    public CheckPostalCodeService(
            TerritoryCatalogPort catalog,
            PostalCodeRepository postalCodes,
            MunicipalityRepository municipalities
    ) {
        this.catalog = catalog;
        this.postalCodes = postalCodes;
        this.municipalities = municipalities;
    }

    @Override
    public PostalCodeCheck check(String ruleSystemCode, String countryCode, String postalCode) {
        String ruleSystem = TerritoryRuleSystem.require(catalog, ruleSystemCode);
        String country = countryCode == null ? "" : countryCode.strip().toUpperCase();
        String code = postalCode == null ? "" : postalCode.strip();

        if (!"ESP".equals(country)) {
            return new PostalCodeCheck(code, country, null, null,
                    "No hay datos de códigos postales de " + country + ".");
        }
        if (!SPANISH_POSTAL_CODE.matcher(code).matches()) {
            throw new InvalidTerritoryQueryException("Un código postal español son cinco cifras: " + code + ".");
        }

        Optional<Province> province = catalog.provinces(ruleSystem).stream()
                .filter(candidate -> candidate.code().equals(code.substring(0, 2)))
                .findFirst();
        if (province.isEmpty()) {
            return new PostalCodeCheck(code, country, null, null,
                    "Las dos primeras cifras de " + code + " no son una provincia.");
        }
        Municipality suggested = postalCodes.findSuggestedMunicipalityCode(country, code)
                .flatMap(municipalityCode -> municipalities.findByCode(country, municipalityCode))
                .orElse(null);
        return new PostalCodeCheck(code, country, province.get(), suggested, null);
    }
}
