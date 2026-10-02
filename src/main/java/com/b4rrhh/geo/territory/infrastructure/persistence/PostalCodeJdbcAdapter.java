package com.b4rrhh.geo.territory.infrastructure.persistence;

import com.b4rrhh.geo.territory.domain.port.PostalCodeRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
public class PostalCodeJdbcAdapter implements PostalCodeRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostalCodeJdbcAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<String> findSuggestedMunicipalityCode(String countryCode, String postalCode) {
        return jdbcTemplate.queryForList("""
                        select suggested_municipality_code
                          from geo.postal_code
                         where country_code = ? and code = ?
                        """, String.class, countryCode, postalCode).stream()
                .filter(Objects::nonNull)
                .findFirst();
    }
}
