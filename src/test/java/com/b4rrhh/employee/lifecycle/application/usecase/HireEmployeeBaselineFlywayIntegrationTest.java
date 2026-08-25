package com.b4rrhh.employee.lifecycle.application.usecase;

import com.b4rrhh.B4rrhhBackendApplication;
import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.HireEmployeeResult;
import com.b4rrhh.support.EsquemaRealInitializer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = B4rrhhBackendApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.flyway.enabled=false"
        }
)
// El esquema no lo declara el test: es el de produccion, aplicado por Flyway
// una vez y clonado para este contexto (ver EsquemaReal). Antes esto corria
// contra H2 con un DDL de fundacion escrito a mano mas un subconjunto de
// semillas; el esquema real trae las semillas baseline (V49-V52), los
// perfiles de convenio de AGR_OFFICE y AGR_TECH (V60) y la numeracion de
// empleados de ESP (V99).
@ContextConfiguration(initializers = EsquemaRealInitializer.class)
class HireEmployeeBaselineFlywayIntegrationTest {

    @Autowired
    private HireEmployeeUseCase hireEmployeeUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

        @ParameterizedTest(name = "{0}")
        @MethodSource("baselineHireScenarios")
        void hiresEmployeeSuccessfullyForAllCoherentBaselineScenarios(HireScenario scenario) {
        HireEmployeeResult result = hireEmployeeUseCase.hire(new HireEmployeeCommand(
            "ESP",
            "INTERNAL",
            scenario.firstName(),
            scenario.lastName1(),
            null,
            scenario.preferredName(),
            scenario.hireDate(),
            "HIRING",
            scenario.companyCode(),
            scenario.workCenterCode(),
            new HireEmployeeCommand.HireEmployeeContractCommand(scenario.contractCode(), scenario.contractSubtypeCode()),
            new HireEmployeeCommand.HireEmployeeLaborClassificationCommand(
                scenario.agreementCode(),
                scenario.agreementCategoryCode()
            ),
            null,
            new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(scenario.workingTimePercentage())
        ));

        String generatedNumber = result.employee().employeeNumber();

        assertThat(result.employee().ruleSystemCode()).isEqualTo("ESP");
        assertThat(result.employee().employeeTypeCode()).isEqualTo("INTERNAL");
        assertThat(generatedNumber).isNotNull().isNotBlank();
        assertThat(result.presence().companyCode()).isEqualTo(scenario.companyCode());
        assertThat(result.workCenter().workCenterCode()).isEqualTo(scenario.workCenterCode());
        assertThat(result.contract().contractTypeCode()).isEqualTo(scenario.contractCode());
        assertThat(result.contract().contractSubtypeCode()).isEqualTo(scenario.contractSubtypeCode());
        assertThat(result.laborClassification().agreementCode()).isEqualTo(scenario.agreementCode());
        assertThat(result.laborClassification().agreementCategoryCode()).isEqualTo(scenario.agreementCategoryCode());

        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from employee.employee where rule_system_code = ? and employee_type_code = ? and employee_number = ?",
            Integer.class,
            "ESP",
            "INTERNAL",
            generatedNumber
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from employee.presence where company_code = ? and employee_id = (select id from employee.employee where rule_system_code = ? and employee_type_code = ? and employee_number = ?)",
            Integer.class,
            scenario.companyCode(),
            "ESP",
            "INTERNAL",
            generatedNumber
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from employee.labor_classification where agreement_code = ? and agreement_category_code = ? and employee_id = (select id from employee.employee where rule_system_code = ? and employee_type_code = ? and employee_number = ?)",
            Integer.class,
            scenario.agreementCode(),
            scenario.agreementCategoryCode(),
            "ESP",
            "INTERNAL",
            generatedNumber
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from employee.contract where contract_code = ? and contract_subtype_code = ? and employee_id = (select id from employee.employee where rule_system_code = ? and employee_type_code = ? and employee_number = ?)",
            Integer.class,
            scenario.contractCode(),
            scenario.contractSubtypeCode(),
            "ESP",
            "INTERNAL",
            generatedNumber
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from employee.work_center where work_center_code = ? and employee_id = (select id from employee.employee where rule_system_code = ? and employee_type_code = ? and employee_number = ?)",
            Integer.class,
            scenario.workCenterCode(),
            "ESP",
            "INTERNAL",
            generatedNumber
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from employee.working_time where employee_id = (select id from employee.employee where rule_system_code = ? and employee_type_code = ? and employee_number = ?)",
            Integer.class,
            "ESP",
            "INTERNAL",
            generatedNumber
        )).isEqualTo(1);
    }

        private static Stream<HireScenario> baselineHireScenarios() {
        return Stream.of(
            new HireScenario(
                "Scenario Base - Office Indefinite",
                "Ana",
                "Lopez",
                "Ani",
                LocalDate.of(2026, 4, 1),
                "ES01",
                "MAIN_OFFICE",
                "AGR_OFFICE",
                "CAT_ADMIN",
                "IND",
                "FT1",
                new BigDecimal("100")
            ),
            new HireScenario(
                "Scenario A - Technical Employee",
                "Bruno",
                "Martin",
                "Bru",
                LocalDate.of(2026, 4, 2),
                "ES01",
                "BRANCH_NORTH",
                "AGR_TECH",
                "CAT_TECH_1",
                "IND",
                "FT1",
                new BigDecimal("100")
            ),
            new HireScenario(
                "Scenario B - Temporary Part Time",
                "Carla",
                "Diaz",
                "Car",
                LocalDate.of(2026, 4, 3),
                "ES01",
                "MAIN_OFFICE",
                "AGR_OFFICE",
                "CAT_ADMIN",
                "TMP",
                "PT1",
                new BigDecimal("60")
            ),
            new HireScenario(
                "Scenario C - Second Company",
                "Dario",
                "Santos",
                "Dari",
                LocalDate.of(2026, 4, 4),
                "ES02",
                "BRANCH_SOUTH",
                "AGR_TECH",
                "CAT_TECH_2",
                "IND",
                "FT1",
                new BigDecimal("100")
            )
        );
        }

        private record HireScenario(
            String name,
            String firstName,
            String lastName1,
            String preferredName,
            LocalDate hireDate,
            String companyCode,
            String workCenterCode,
            String agreementCode,
            String agreementCategoryCode,
            String contractCode,
            String contractSubtypeCode,
            BigDecimal workingTimePercentage
        ) {
        @Override
        public String toString() {
            return name;
        }
        }
}
