package com.b4rrhh.geo.territory.infrastructure.persistence;

import com.b4rrhh.geo.territory.domain.model.Municipality;
import com.b4rrhh.geo.territory.domain.port.MunicipalityRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Los municipios de {@code geo}. La forma de búsqueda es {@code geo.search_form}, la misma
 * función que calcula la columna {@code search_name} (V175): lo que se busca y lo que se
 * guarda se normalizan en un solo sitio.
 */
@Component
public class MunicipalityJdbcAdapter implements MunicipalityRepository {

    private static final RowMapper<Municipality> ROW = (rs, n) -> new Municipality(
            rs.getString("country_code"),
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("province_code"),
            rs.getObject("start_date", LocalDate.class),
            rs.getObject("end_date", LocalDate.class));

    private final JdbcTemplate jdbcTemplate;

    public MunicipalityJdbcAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Municipality> searchByName(String countryCode, String name, LocalDate date, int limit) {
        // Los comodines de LIKE no son parte de ningún nombre: se quitan, no se escapan.
        String words = name.replaceAll("[%_\\\\]", "").strip();
        Date day = Date.valueOf(date);
        return jdbcTemplate.query("""
                select country_code, code, name, province_code, start_date, end_date
                  from geo.municipality
                 where country_code = ?
                   and start_date <= ?
                   and (end_date is null or end_date >= ?)
                   and (' ' || search_name) like '% ' || geo.search_form(?) || '%'
                 order by (search_name like geo.search_form(?) || '%') desc, name, code
                 limit ?
                """, ROW, countryCode, day, day, words, words, limit);
    }

    @Override
    public Optional<Municipality> findByCode(String countryCode, String code) {
        return jdbcTemplate.query("""
                select country_code, code, name, province_code, start_date, end_date
                  from geo.municipality
                 where country_code = ? and code = ?
                """, ROW, countryCode, code).stream().findFirst();
    }
}
