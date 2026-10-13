package com.b4rrhh.rulesystem.infrastructure.persistence;

import com.b4rrhh.rulesystem.domain.model.Layer;
import com.b4rrhh.rulesystem.domain.port.LayerRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class LayerJdbcAdapter implements LayerRepository {

    /** Con las reglamentaciones que la montan: COM e INT, todas; una nacional, la suya. */
    private static final String LAYERS = """
            select l.code, l.name, l.level, l.active,
                   array_remove(array_agg(rsl.rule_system_code order by rsl.rule_system_code), null) as mounted_by
              from rulesystem.layer l
              left join rulesystem.rule_system_layer rsl on rsl.layer_code = l.code
            """;

    private static final String GROUPED = """
             group by l.code, l.name, l.level, l.active
            """;

    private final JdbcTemplate jdbcTemplate;

    public LayerJdbcAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Layer> findAll() {
        return jdbcTemplate.query(LAYERS + GROUPED + " order by l.level, l.code", LayerJdbcAdapter::toLayer);
    }

    @Override
    public Optional<Layer> findByCode(String code) {
        return jdbcTemplate.query(LAYERS + " where l.code = ?" + GROUPED, LayerJdbcAdapter::toLayer, code)
                .stream()
                .findFirst();
    }

    private static Layer toLayer(ResultSet rs, int rowNum) throws SQLException {
        Array mountedBy = rs.getArray("mounted_by");
        return new Layer(
                rs.getString("code"),
                rs.getString("name"),
                rs.getInt("level"),
                rs.getBoolean("active"),
                Arrays.asList((String[]) mountedBy.getArray())
        );
    }
}
