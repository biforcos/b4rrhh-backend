package com.b4rrhh.rulesystem.infrastructure.persistence;

import com.b4rrhh.rulesystem.application.port.RuleSystemLayerPort;
import com.b4rrhh.rulesystem.domain.model.DefaultRuleSystemLayers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RuleSystemLayerJdbcAdapter implements RuleSystemLayerPort {

    private final JdbcTemplate jdbcTemplate;

    public RuleSystemLayerJdbcAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean layerExists(String layerCode) {
        Boolean exists = jdbcTemplate.queryForObject(
                "select exists (select 1 from rulesystem.layer where code = ?)", Boolean.class, layerCode);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public void assembleDefaultLayers(String ruleSystemCode, String ruleSystemName) {
        // Los mismos nombres que les puso la V166 a las de ESP, FRA y PRT.
        insertLayer(DefaultRuleSystemLayers.national(ruleSystemCode), ruleSystemName, 3);
        insertLayer(DefaultRuleSystemLayers.payroll(ruleSystemCode), "Nómina nacional · " + ruleSystemName, 4);
        insertLayer(DefaultRuleSystemLayers.companyPayroll(ruleSystemCode), "Nómina de empresa · " + ruleSystemName, 5);

        mount(ruleSystemCode, 1, DefaultRuleSystemLayers.COMMON);
        mount(ruleSystemCode, 2, DefaultRuleSystemLayers.INTERNATIONAL);
        mount(ruleSystemCode, 3, DefaultRuleSystemLayers.national(ruleSystemCode));
        mount(ruleSystemCode, 4, DefaultRuleSystemLayers.payroll(ruleSystemCode));
        mount(ruleSystemCode, 5, DefaultRuleSystemLayers.companyPayroll(ruleSystemCode));
    }

    private void insertLayer(String code, String name, int level) {
        jdbcTemplate.update("insert into rulesystem.layer (code, name, level) values (?, ?, ?)", code, name, level);
    }

    private void mount(String ruleSystemCode, int level, String layerCode) {
        jdbcTemplate.update(
                "insert into rulesystem.rule_system_layer (rule_system_code, level, layer_code) values (?, ?, ?)",
                ruleSystemCode, level, layerCode);
    }
}
