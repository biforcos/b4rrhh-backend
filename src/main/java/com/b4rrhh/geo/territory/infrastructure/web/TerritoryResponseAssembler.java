package com.b4rrhh.geo.territory.infrastructure.web;

import com.b4rrhh.geo.territory.domain.model.Country;
import com.b4rrhh.geo.territory.domain.model.LocatedMunicipality;
import com.b4rrhh.geo.territory.domain.model.Municipality;
import com.b4rrhh.geo.territory.domain.model.PostalCodeCheck;
import com.b4rrhh.geo.territory.domain.model.Province;
import com.b4rrhh.geo.territory.domain.model.Region;
import com.b4rrhh.geo.territory.domain.model.StreetType;
import com.b4rrhh.geo.territory.infrastructure.web.dto.TerritoryCountryResponse;
import com.b4rrhh.geo.territory.infrastructure.web.dto.TerritoryMunicipalityRefResponse;
import com.b4rrhh.geo.territory.infrastructure.web.dto.TerritoryMunicipalityResponse;
import com.b4rrhh.geo.territory.infrastructure.web.dto.TerritoryPostalCodeResponse;
import com.b4rrhh.geo.territory.infrastructure.web.dto.TerritoryProvinceRefResponse;
import com.b4rrhh.geo.territory.infrastructure.web.dto.TerritoryProvinceResponse;
import com.b4rrhh.geo.territory.infrastructure.web.dto.TerritoryRegionRefResponse;
import com.b4rrhh.geo.territory.infrastructure.web.dto.TerritoryRegionResponse;
import com.b4rrhh.geo.territory.infrastructure.web.dto.TerritoryStreetTypeResponse;
import org.springframework.stereotype.Component;

@Component
public class TerritoryResponseAssembler {

    public TerritoryCountryResponse toResponse(Country country) {
        return new TerritoryCountryResponse(country.code(), country.name());
    }

    public TerritoryProvinceResponse toResponse(Province province) {
        Region region = province.region();
        return new TerritoryProvinceResponse(
                province.code(),
                province.name(),
                province.isoCode(),
                region == null ? null : new TerritoryRegionResponse(region.code(), region.name(), region.isoCode()));
    }

    public TerritoryStreetTypeResponse toResponse(StreetType streetType) {
        return new TerritoryStreetTypeResponse(streetType.code(), streetType.name());
    }

    public TerritoryMunicipalityResponse toResponse(LocatedMunicipality located) {
        Municipality municipality = located.municipality();
        Province province = located.province();
        Region region = province == null ? null : province.region();
        return new TerritoryMunicipalityResponse(
                municipality.code(),
                municipality.name(),
                municipality.startDate(),
                municipality.endDate(),
                province == null
                        ? new TerritoryProvinceRefResponse(municipality.provinceCode(), null)
                        : new TerritoryProvinceRefResponse(province.code(), province.name()),
                region == null ? null : new TerritoryRegionRefResponse(region.code(), region.name()));
    }

    public TerritoryPostalCodeResponse toResponse(PostalCodeCheck check) {
        Province province = check.province();
        Municipality suggested = check.suggestedMunicipality();
        return new TerritoryPostalCodeResponse(
                check.postalCode(),
                check.countryCode(),
                province == null ? null : new TerritoryProvinceRefResponse(province.code(), province.name()),
                suggested == null ? null : new TerritoryMunicipalityRefResponse(suggested.code(), suggested.name()),
                check.message());
    }
}
