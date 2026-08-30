package com.b4rrhh.rulesystem.translation;

import com.b4rrhh.employee.address.infrastructure.persistence.AddressCatalogReadAdapter;
import com.b4rrhh.employee.contact.infrastructure.persistence.ContactCatalogReadAdapter;
import com.b4rrhh.employee.contract.infrastructure.persistence.ContractCatalogReadAdapter;
import com.b4rrhh.employee.cost_center.infrastructure.persistence.CostCenterCatalogReadAdapter;
import com.b4rrhh.employee.identifier.infrastructure.persistence.IdentifierCatalogReadAdapter;
import com.b4rrhh.employee.labor_classification.infrastructure.persistence.LaborClassificationCatalogReadAdapter;
import com.b4rrhh.employee.workcenter.infrastructure.persistence.WorkCenterCatalogReadAdapter;
import com.b4rrhh.rulesystem.catalogoption.domain.model.DirectCatalogOption;
import com.b4rrhh.rulesystem.catalogoption.infrastructure.persistence.RuleEntityDirectCatalogOptionReadAdapter;
import com.b4rrhh.rulesystem.infrastructure.persistence.SpringDataRuleEntityRepository;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.rulesystem.translation.infrastructure.persistence.SpringDataRuleEntityTranslationRepository;
import com.b4rrhh.rulesystem.workcenter.infrastructure.persistence.WorkCenterContactCatalogReadAdapter;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * backend#24 (ADR-052 §3): los adaptadores de catálogo ya no llevan su copia de
 * {@code findCatalogName}; delegan en el resolutor, y por eso una traducción sembrada les
 * llega a todos. Sin idioma —la sobrecarga de siempre— siguen dando el literal base.
 *
 * Los adaptadores se construyen a mano con el resolutor del contexto: no hace falta meterlos
 * en el {@code @Import} compartido para probar dos líneas de delegación.
 */
@TestSobreEsquemaReal
class CatalogReadAdaptersRespectTranslationsIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RuleEntityLabelResolver resolver;

    @Autowired
    private SpringDataRuleEntityRepository springDataRuleEntityRepository;

    @Autowired
    private SpringDataRuleEntityTranslationRepository springDataRuleEntityTranslationRepository;

    /** Una función por método de adaptador: (adaptadores, idioma) → literal. */
    record Lookup(String name, String typeCode, String code,
                  Function<CatalogReadAdaptersRespectTranslationsIntegrationTest, Function<String, Optional<String>>> call) {
        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Arguments> lookups() {
        return Stream.of(
                new Lookup("address type", "EMPLOYEE_ADDRESS_TYPE", "FISCAL",
                        t -> lang -> new AddressCatalogReadAdapter(t.resolver).findAddressTypeName("ESP", "FISCAL", lang)),
                new Lookup("contact type", "CONTACT_TYPE", "COMPANY_MOBILE",
                        t -> lang -> new ContactCatalogReadAdapter(t.resolver).findContactTypeName("ESP", "COMPANY_MOBILE", lang)),
                new Lookup("contract", "CONTRACT", "100",
                        t -> lang -> new ContractCatalogReadAdapter(t.resolver).findContractTypeName("ESP", "100", lang)),
                new Lookup("contract subtype", "CONTRACT_SUBTYPE", "01",
                        t -> lang -> new ContractCatalogReadAdapter(t.resolver).findContractSubtypeName("ESP", "01", lang)),
                new Lookup("cost center", "COST_CENTER", "CC_ADMIN",
                        t -> lang -> new CostCenterCatalogReadAdapter(t.resolver).findCostCenterName("ESP", "CC_ADMIN", lang)),
                new Lookup("identifier type", "EMPLOYEE_IDENTIFIER_TYPE", "NATIONAL_ID",
                        t -> lang -> new IdentifierCatalogReadAdapter(t.resolver).findIdentifierTypeName("ESP", "NATIONAL_ID", lang)),
                new Lookup("agreement", "AGREEMENT", "99002405011982",
                        t -> lang -> new LaborClassificationCatalogReadAdapter(t.resolver, null).findAgreementName("ESP", "99002405011982", lang)),
                new Lookup("agreement category", "AGREEMENT_CATEGORY", "99002405-G1",
                        t -> lang -> new LaborClassificationCatalogReadAdapter(t.resolver, null).findAgreementCategoryName("ESP", "99002405-G1", lang)),
                new Lookup("work center", "WORK_CENTER", "BRANCH_EAST",
                        t -> lang -> new WorkCenterCatalogReadAdapter(t.resolver, null).findWorkCenterName("ESP", "BRANCH_EAST", lang)),
                new Lookup("company (work center)", "COMPANY", "ES01",
                        t -> lang -> new WorkCenterCatalogReadAdapter(t.resolver, null).findCompanyName("ESP", "ES01", lang)),
                new Lookup("contact type (work center contact)", "CONTACT_TYPE", "COMPANY_MOBILE",
                        t -> lang -> new WorkCenterContactCatalogReadAdapter(t.resolver).findContactTypeName("ESP", "COMPANY_MOBILE", lang))
        ).map(Arguments::of);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("lookups")
    void eachAdapterServesTheTranslationForItsLanguageAndTheBaseLiteralOtherwise(Lookup lookup) {
        Function<String, Optional<String>> find = lookup.call().apply(this);
        String baseLiteral = find.apply(null).orElseThrow();
        insertTranslation(lookup.typeCode(), lookup.code(), "es-ES", "Traducido " + lookup.code());

        assertThat(find.apply("es-ES")).contains("Traducido " + lookup.code());
        assertThat(find.apply("fr-FR")).as("otro idioma").contains(baseLiteral);
        assertThat(find.apply(null)).as("sin idioma").contains(baseLiteral);
    }

    @Test
    void theCatalogOptionsThatFeedTheDropdownsRespectTheTranslationToo() {
        RuleEntityDirectCatalogOptionReadAdapter adapter = new RuleEntityDirectCatalogOptionReadAdapter(
                springDataRuleEntityRepository, springDataRuleEntityTranslationRepository);
        insertTranslation("EMPLOYEE_PRESENCE_ENTRY_REASON", "HIRING", "es-ES", "Contratación");

        // referenceDate va a null a proposito: con una fecha informada, el JPQL preexistente
        // findDirectCatalogOptions falla en Postgres ("could not determine data type of
        // parameter $3"). Es un bug anterior a este cambio, anotado en backend#24; aqui no se toca.
        List<DirectCatalogOption> translated = adapter.findDirectOptions(
                "ESP", "EMPLOYEE_PRESENCE_ENTRY_REASON", null, null, "es-ES");
        List<DirectCatalogOption> base = adapter.findDirectOptions(
                "ESP", "EMPLOYEE_PRESENCE_ENTRY_REASON", null, null);

        assertThat(translated).filteredOn(option -> option.code().equals("HIRING"))
                .extracting(DirectCatalogOption::name).containsExactly("Contratación");
        assertThat(base).filteredOn(option -> option.code().equals("HIRING"))
                .extracting(DirectCatalogOption::name).containsExactly("Hiring");
        assertThat(translated).extracting(DirectCatalogOption::code)
                .as("mismas opciones, mismo orden").containsExactlyElementsOf(base.stream().map(DirectCatalogOption::code).toList());
    }

    private void insertTranslation(String typeCode, String code, String language, String name) {
        int inserted = jdbcTemplate.update("""
                insert into rulesystem.rule_entity_translation (rule_entity_id, language_code, name)
                select id, ?, ?
                  from rulesystem.rule_entity
                 where rule_system_code = 'ESP'
                   and rule_entity_type_code = ?
                   and code = ?
                """, language, name, typeCode, code);
        assertThat(inserted).as("ESP/%s/%s viene en la semilla", typeCode, code).isEqualTo(1);
    }
}
