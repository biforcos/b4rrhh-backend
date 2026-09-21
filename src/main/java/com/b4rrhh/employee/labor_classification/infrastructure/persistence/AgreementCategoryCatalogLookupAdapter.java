package com.b4rrhh.employee.labor_classification.infrastructure.persistence;

import com.b4rrhh.employee.labor_classification.application.model.AgreementCategoryCatalogItem;
import com.b4rrhh.employee.labor_classification.application.port.AgreementCategoryCatalogLookupPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class AgreementCategoryCatalogLookupAdapter implements AgreementCategoryCatalogLookupPort {

    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);
    private static final String BASE_QUERY = """
            select distinct
                upper(trim(cat.code)) as category_code,
                cat.name as category_name,
                greatest(agr.start_date, cat.start_date, r.start_date) as category_start_date,
                nullif(least(coalesce(agr.end_date, DATE '9999-12-31'),
                             coalesce(cat.end_date, DATE '9999-12-31'),
                             coalesce(r.end_date,   DATE '9999-12-31')),
                       DATE '9999-12-31') as category_end_date
            from rulesystem.agreement_category_relation r
            join rulesystem.rule_system rs
              on rs.id = r.rule_system_id
            join rulesystem.rule_entity agr
              on agr.id = r.agreement_rule_entity_id
            join rulesystem.rule_entity cat
              on cat.id = r.category_rule_entity_id
            where upper(trim(rs.code)) = :ruleSystemCode
              and upper(trim(agr.code)) = :agreementCode
              and agr.rule_entity_type_code = 'AGREEMENT'
              and cat.rule_entity_type_code = 'AGREEMENT_CATEGORY'
              and agr.rule_system_code = rs.code
              and cat.rule_system_code = rs.code
              and agr.active = true
              and cat.active = true
              and r.is_active = true
            """;

    private static final String TEMPORAL_FILTERS = """
              and agr.start_date <= :referenceDate
              and :referenceDate <= coalesce(agr.end_date, :maxDate)
              and cat.start_date <= :referenceDate
              and :referenceDate <= coalesce(cat.end_date, :maxDate)
              and r.start_date <= :referenceDate
              and :referenceDate <= coalesce(r.end_date, :maxDate)
            """;

    private static final String ORDER_BY = """
            order by category_code
            """;

    private final EntityManager entityManager;

    public AgreementCategoryCatalogLookupAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<AgreementCategoryCatalogItem> listActiveCategoriesByAgreement(
            String ruleSystemCode,
            String agreementCode
    ) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(BASE_QUERY + ORDER_BY)
                .setParameter("ruleSystemCode", ruleSystemCode)
                .setParameter("agreementCode", agreementCode)
                .getResultList();

        return rows.stream()
                .map(this::toCatalogItem)
                .toList();
    }

    @Override
    public List<AgreementCategoryCatalogItem> listActiveCategoriesByAgreementOnDate(
            String ruleSystemCode,
            String agreementCode,
            LocalDate referenceDate
    ) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(BASE_QUERY + TEMPORAL_FILTERS + ORDER_BY)
                .setParameter("ruleSystemCode", ruleSystemCode)
                .setParameter("agreementCode", agreementCode)
                .setParameter("referenceDate", referenceDate)
                .setParameter("maxDate", MAX_DATE)
                .getResultList();

        return rows.stream()
                .map(this::toCatalogItem)
                .toList();
    }

    /**
     * La vigencia EFECTIVA de la opcion, que es la interseccion de las tres ({@code backend#115}).
     *
     * <p>Aqui se publicaban las fechas de {@code category} y llegaban nulas al cliente, por dos
     * defectos encadenados:
     *
     * <ul>
     *   <li>El conversor era {@code row[i] instanceof LocalDate ? ... : null}, y el driver
     *       devuelve {@code java.sql.Date} para una columna {@code date}. La comprobacion no
     *       fallaba: devolvia nulo, siempre, en silencio.
     *   <li>Y aunque no fallara, publicaba la fecha de la entidad y no la de la RELACION, que es
     *       la que de verdad decide -y la que un cliente no puede ver por ningun otro sitio-.
     * </ul>
     *
     * <p>Se publica la interseccion y no una de las tres porque es la unica que contesta la
     * pregunta que se hace: <b>desde cuando se puede usar esta opcion</b>. Es exactamente lo que
     * el filtro temporal de esta misma consulta exige, escrito como dato en vez de como
     * condicion.
     *
     * <p>El {@code nullif} devuelve el fin a nulo cuando ninguna de las tres caduca: un
     * {@code 9999-12-31} en la respuesta se leeria como una fecha de verdad.
     */
    private AgreementCategoryCatalogItem toCatalogItem(Object[] row) {
        return new AgreementCategoryCatalogItem(
                String.valueOf(row[0]),
                row[1] != null ? String.valueOf(row[1]) : null,
                toLocalDate(row[2]),
                toLocalDate(row[3])
        );
    }

    /**
     * Lo que el driver devuelve para una columna {@code date} es {@code java.sql.Date}, no
     * {@code LocalDate}. Darlo por hecho es lo que vaciaba estas dos fechas.
     */
    private static LocalDate toLocalDate(Object value) {
        return switch (value) {
            case null -> null;
            case LocalDate localDate -> localDate;
            case java.sql.Date sqlDate -> sqlDate.toLocalDate();
            default -> throw new IllegalStateException(
                    "Tipo inesperado para una fecha del catalogo: " + value.getClass());
        };
    }
}
