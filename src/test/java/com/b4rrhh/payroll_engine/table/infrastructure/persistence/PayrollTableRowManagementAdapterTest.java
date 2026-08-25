package com.b4rrhh.payroll_engine.table.infrastructure.persistence;

import com.b4rrhh.support.EsquemaRealInitializer;
import com.b4rrhh.payroll_engine.table.domain.model.PayrollTableRow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false"
})
// El esquema no lo declara el test: es el de produccion, aplicado por Flyway
// una vez y clonado para este contexto (ver EsquemaReal). El subconjunto de
// migraciones que se copiaba aqui estaba congelado: una migracion futura que
// tocara estas tablas no se veia. El codigo de tabla SB_TEST no es ninguno
// de los que siembran las migraciones (SB, PC).
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = EsquemaRealInitializer.class)
@Import(PayrollTableRowManagementAdapter.class)
class PayrollTableRowManagementAdapterTest {

    @Autowired
    private PayrollTableRowManagementAdapter adapter;

    @Test
    void savesAndFindsRowsByTableCode() {
        PayrollTableRow row = new PayrollTableRow(
                null, "ESP", "SB_TEST", "SB-G1",
                LocalDate.of(2024, 1, 1), null,
                new BigDecimal("1800.00"), new BigDecimal("21600.00"),
                new BigDecimal("60.00"), new BigDecimal("7.50"),
                true
        );
        adapter.save(row);

        List<PayrollTableRow> rows = adapter.findAllByTableCode("ESP", "SB_TEST");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getSearchCode()).isEqualTo("SB-G1");
        assertThat(rows.get(0).getMonthlyValue()).isEqualByComparingTo("1800.00");
        assertThat(rows.get(0).getId()).isNotNull();
    }

    @Test
    void findsRowById() {
        PayrollTableRow saved = adapter.save(new PayrollTableRow(
                null, "ESP", "SB_TEST", "SB-G2",
                LocalDate.of(2024, 1, 1), null,
                new BigDecimal("1400.00"), new BigDecimal("16800.00"),
                new BigDecimal("46.67"), new BigDecimal("5.83"),
                true
        ));

        Optional<PayrollTableRow> found = adapter.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getSearchCode()).isEqualTo("SB-G2");
    }

    @Test
    void deletesRowById() {
        PayrollTableRow saved = adapter.save(new PayrollTableRow(
                null, "ESP", "SB_TEST", "SB-G3",
                LocalDate.of(2024, 1, 1), null,
                new BigDecimal("1200.00"), new BigDecimal("14400.00"),
                new BigDecimal("40.00"), new BigDecimal("5.00"),
                true
        ));

        adapter.deleteById(saved.getId());

        assertThat(adapter.findById(saved.getId())).isEmpty();
    }

    @Test
    void detectsDuplicateByBusinessKey() {
        adapter.save(new PayrollTableRow(
                null, "ESP", "SB_TEST", "SB-G4",
                LocalDate.of(2024, 1, 1), null,
                new BigDecimal("1000.00"), new BigDecimal("12000.00"),
                new BigDecimal("33.33"), new BigDecimal("4.17"),
                true
        ));

        boolean exists = adapter.existsByBusinessKey("ESP", "SB_TEST", "SB-G4", LocalDate.of(2024, 1, 1));
        assertThat(exists).isTrue();

        boolean notExists = adapter.existsByBusinessKey("ESP", "SB_TEST", "SB-G4", LocalDate.of(2025, 1, 1));
        assertThat(notExists).isFalse();
    }
}
