package com.b4rrhh.architecture;

import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeOwnedRuleEntityUsageParticipant;
import com.b4rrhh.geo.address.infrastructure.persistence.PostalAddressRuleEntityUsageParticipant;
import com.b4rrhh.rulesystem.application.port.RuleEntityUsageParticipant;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Toda tabla con una dirección cuenta el territorio que usa (backend#168).
 *
 * <p>Las direcciones guardan tipo de vía, provincia y país por código y sin clave ajena (la marca
 * «sin normalizar» del backend#155 lo exige), así que lo único que impide borrar uno que se usa es
 * el recuento de referencias. Los guardarraíles que ya había no lo ven: {@code
 * EveryCatalogVerticalDeclaresItsRuleEntityUsageTest} mira ficheros de los verticales del empleado,
 * y empresa y centro no lo son; {@code EveryCatalogColumnIsDeclaredOrExemptedTest} exime el tipo de
 * vía y la provincia a propósito.</p>
 *
 * <p>Éste sale del esquema: una tabla con {@code street_type_code} tiene estructura de dirección, y
 * una tabla nueva con dirección aparece sola en el fallo.</p>
 */
@TestWebSobreEsquemaReal
class EveryAddressTableCountsTheTerritoryItUsesTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private List<RuleEntityUsageParticipant> participants;

    @Test
    void everyTableWithAnAddressHasAParticipantThatCountsItsStreetTypeAndProvince() {
        Set<String> conDireccion = tablasConDireccion();
        assertThat(conDireccion).as("las tres de backend#155, por lo menos").hasSizeGreaterThanOrEqualTo(3);

        Set<String> contadas = participantesDeDireccion().stream()
                .map(PostalAddressRuleEntityUsageParticipant::table)
                .collect(Collectors.toCollection(TreeSet::new));

        assertThat(contadas).as("tablas con dirección que cuentan tipo de vía y provincia")
                .isEqualTo(conDireccion);
    }

    @Test
    void everyTableWithAnAddressCountsItsCountrySomewhere() {
        Set<String> paisContado = new TreeSet<>();
        participantesDeDireccion().stream()
                .filter(PostalAddressRuleEntityUsageParticipant::countsCountry)
                .map(PostalAddressRuleEntityUsageParticipant::table)
                .forEach(paisContado::add);
        participants.stream()
                .filter(EmployeeOwnedRuleEntityUsageParticipant.class::isInstance)
                .map(EmployeeOwnedRuleEntityUsageParticipant.class::cast)
                .flatMap(participant -> participant.declaredUsages().stream())
                .filter(usage -> usage.ruleEntityTypeCode().equals("COUNTRY") && usage.column().equals("country_code"))
                .map(usage -> "employee." + usage.table())
                .forEach(paisContado::add);

        assertThat(paisContado).as("tablas con dirección que cuentan el país").containsAll(tablasConDireccion());
    }

    private List<PostalAddressRuleEntityUsageParticipant> participantesDeDireccion() {
        return participants.stream()
                .filter(PostalAddressRuleEntityUsageParticipant.class::isInstance)
                .map(PostalAddressRuleEntityUsageParticipant.class::cast)
                .toList();
    }

    private Set<String> tablasConDireccion() {
        return new TreeSet<>(jdbcTemplate.queryForList("""
                select table_schema || '.' || table_name
                  from information_schema.columns
                 where column_name = 'street_type_code'
                   and table_schema in ('employee', 'rulesystem')
                """, String.class));
    }
}
