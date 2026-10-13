package com.b4rrhh.geo.address.application.service;

import com.b4rrhh.geo.address.domain.exception.PostalAddressRejectedException;
import com.b4rrhh.geo.address.domain.model.PostalAddress;
import com.b4rrhh.geo.address.domain.model.PostalAddressInput;
import com.b4rrhh.geo.territory.domain.model.Municipality;
import com.b4rrhh.geo.territory.domain.port.MunicipalityRepository;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * La validación de una dirección, una sola para el empleado, la empresa y el centro (backend#155,
 * ADR-078 §5). Devuelve la dirección con la marca puesta; sólo rechaza lo que no se puede guardar.
 *
 * <h2>Qué es estar normalizada</h2>
 *
 * <p>En España, las cuatro cosas: el municipio existe y no ha terminado en la fecha; la provincia
 * es la del municipio; el CP tiene cinco cifras y es de esa provincia; el tipo de vía está en el
 * catálogo. Fuera de España, el país y nada más: ni municipio INE, ni provincia, ni tipo de vía,
 * que son españoles. Es la misma regla que la V177 aplicó a lo que había, y
 * {@code TheAddressMigrationSaysWhatItLeftOnTheSeedTest} comprueba que dicen lo mismo.</p>
 *
 * <h2>Qué se rechaza</h2>
 *
 * <p>Sin país, un país que no está en el catálogo de la reglamentación, un CP que contradice a la
 * provincia —la elegida o la del municipio— y un campo que no cabe. Un pueblo que no se encuentra
 * no se rechaza: se guarda sin normalizar, porque bloquear el alta es lo que hace que la gente meta
 * «Valencia» a todo.</p>
 *
 * <h2>Dónde se mira el territorio</h2>
 *
 * <p>Por el país de la dirección, no por la reglamentación de quien la tiene: un empleado de FRA que
 * vive en Valencia tiene una calle española. Los tipos de vía y las provincias se leen en la capa
 * {@code ESP} (V174); el país, en el catálogo de la reglamentación, que lo ve desde INT.</p>
 *
 * <p>La vigencia de un municipio es «desde al menos» (V175): {@code startDate} es la primera
 * relación del INE cargada, no su creación. Por eso sólo cuenta que no haya terminado.</p>
 */
@Component
public class PostalAddressValidator {

    public static final String SPAIN = "ESP";
    /** La capa donde viven las provincias y los tipos de vía de España (V174). */
    static final String SPANISH_TERRITORY_LAYER = "ESP";
    static final String COUNTRY = "COUNTRY";
    static final String PROVINCE = "PROVINCE";
    static final String STREET_TYPE = "STREET_TYPE";

    private static final Pattern SPANISH_POSTAL_CODE = Pattern.compile("[0-9]{5}");

    private final RuleEntityRepository ruleEntityRepository;
    private final MunicipalityRepository municipalityRepository;

    public PostalAddressValidator(RuleEntityRepository ruleEntityRepository, MunicipalityRepository municipalityRepository) {
        this.ruleEntityRepository = ruleEntityRepository;
        this.municipalityRepository = municipalityRepository;
    }

    /**
     * @param ruleSystemCode la reglamentación de quien tiene la dirección, para el catálogo de países
     * @param date           la fecha en la que tiene que valer: el inicio del domicilio del empleado, o hoy
     */
    public PostalAddress validate(String ruleSystemCode, PostalAddressInput input, LocalDate date) {
        if (input == null) {
            throw new PostalAddressRejectedException("address is required: at least its countryCode");
        }
        String countryCode = code("countryCode", input.countryCode(), 3);
        String streetTypeCode = code("streetTypeCode", input.streetTypeCode(), 30);
        String municipalityCode = code("municipalityCode", input.municipalityCode(), 10);
        String subdivisionCode = code("subdivisionCode", input.subdivisionCode(), 10);
        String postalCode = text("postalCode", input.postalCode(), 20);
        String streetName = text("streetName", input.streetName(), 200);
        String streetNumber = text("streetNumber", input.streetNumber(), 10);
        String numberQualifier = text("numberQualifier", input.numberQualifier(), 10);
        String block = text("block", input.block(), 10);
        String stair = text("stair", input.stair(), 10);
        String floor = text("floor", input.floor(), 10);
        String door = text("door", input.door(), 10);
        String locality = text("locality", input.locality(), 120);

        if (countryCode == null) {
            throw new PostalAddressRejectedException("countryCode is required: an address without a country is not stored");
        }
        requireCountry(ruleSystemCode, countryCode, date);

        boolean normalized;
        String municipalityName = null;
        if (!SPAIN.equals(countryCode)) {
            normalized = municipalityCode == null && subdivisionCode == null && streetTypeCode == null;
        } else {
            Optional<Municipality> municipality = municipalityCode == null
                    ? Optional.empty()
                    : municipalityRepository.findByCode(SPAIN, municipalityCode);
            if (subdivisionCode == null) {
                subdivisionCode = municipality.map(Municipality::provinceCode)
                        .orElseGet(() -> provinceOfPostalCode(postalCode));
            }
            rejectAPostalCodeOfAnotherProvince(postalCode, subdivisionCode);
            municipalityName = municipality.map(Municipality::name).orElse(null);

            String province = subdivisionCode;
            normalized = municipality
                    .filter(m -> m.endDate() == null || !m.endDate().isBefore(date))
                    .filter(m -> m.provinceCode().equals(province))
                    .isPresent()
                    && isSpanishPostalCode(postalCode)
                    && postalCode.startsWith(province)
                    && inSpanishCatalog(PROVINCE, province)
                    && streetTypeCode != null
                    && inSpanishCatalog(STREET_TYPE, streetTypeCode);
        }

        return new PostalAddress(streetTypeCode, streetName, streetNumber, numberQualifier, block, stair,
                floor, door, locality, municipalityCode, subdivisionCode, postalCode, countryCode, normalized,
                municipalityName);
    }

    private void requireCountry(String ruleSystemCode, String countryCode, LocalDate date) {
        boolean known = ruleEntityRepository.findByBusinessKey(ruleSystemCode, COUNTRY, countryCode)
                .filter(RuleEntity::isActive)
                .filter(country -> !date.isBefore(country.getStartDate())
                        && (country.getEndDate() == null || !date.isAfter(country.getEndDate())))
                .isPresent();
        if (!known) {
            throw new PostalAddressRejectedException(
                    "countryCode " + countryCode + " is not an active country of the catalog of " + ruleSystemCode + " on " + date);
        }
    }

    private static void rejectAPostalCodeOfAnotherProvince(String postalCode, String province) {
        if (province != null && isSpanishPostalCode(postalCode) && !postalCode.startsWith(province)) {
            throw new PostalAddressRejectedException(
                    "postalCode " + postalCode + " is of province " + postalCode.substring(0, 2)
                            + ", not of province " + province + ": the first two digits of a Spanish postal code are its province");
        }
    }

    private String provinceOfPostalCode(String postalCode) {
        if (!isSpanishPostalCode(postalCode)) {
            return null;
        }
        String province = postalCode.substring(0, 2);
        return inSpanishCatalog(PROVINCE, province) ? province : null;
    }

    private boolean inSpanishCatalog(String ruleEntityTypeCode, String code) {
        return code != null
                && ruleEntityRepository.findInLayerByBusinessKey(SPANISH_TERRITORY_LAYER, ruleEntityTypeCode, code).isPresent();
    }

    private static boolean isSpanishPostalCode(String postalCode) {
        return postalCode != null && SPANISH_POSTAL_CODE.matcher(postalCode).matches();
    }

    private static String code(String field, String value, int maxLength) {
        String text = text(field, value, maxLength);
        return text == null ? null : text.toUpperCase();
    }

    private static String text(String field, String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.length() > maxLength) {
            throw new PostalAddressRejectedException(field + " exceeds max length " + maxLength);
        }
        return trimmed;
    }
}
