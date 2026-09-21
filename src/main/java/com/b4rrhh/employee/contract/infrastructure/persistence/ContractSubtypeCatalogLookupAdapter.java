package com.b4rrhh.employee.contract.infrastructure.persistence;

import com.b4rrhh.employee.contract.application.model.ContractSubtypeCatalogItem;
import com.b4rrhh.employee.contract.application.port.ContractSubtypeCatalogLookupPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class ContractSubtypeCatalogLookupAdapter implements ContractSubtypeCatalogLookupPort {

    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);
    private static final String BASE_QUERY = """
            select distinct
                upper(trim(sub.code)) as subtype_code,
                sub.name as subtype_name,
                greatest(ctr.start_date, sub.start_date, r.start_date) as subtype_start_date,
                nullif(least(coalesce(ctr.end_date, DATE '9999-12-31'),
                             coalesce(sub.end_date, DATE '9999-12-31'),
                             coalesce(r.end_date,   DATE '9999-12-31')),
                       DATE '9999-12-31') as subtype_end_date
            from rulesystem.contract_subtype_relation r
            join rulesystem.rule_system rs
              on rs.id = r.rule_system_id
            join rulesystem.rule_entity ctr
              on ctr.id = r.contract_rule_entity_id
            join rulesystem.rule_entity sub
              on sub.id = r.subtype_rule_entity_id
            where upper(trim(rs.code)) = :ruleSystemCode
              and upper(trim(ctr.code)) = :contractTypeCode
              and ctr.rule_entity_type_code = 'CONTRACT'
              and sub.rule_entity_type_code = 'CONTRACT_SUBTYPE'
              and ctr.rule_system_code = rs.code
              and sub.rule_system_code = rs.code
              and ctr.active = true
              and sub.active = true
              and r.is_active = true
            """;

    private static final String TEMPORAL_FILTERS = """
              and ctr.start_date <= :referenceDate
              and :referenceDate <= coalesce(ctr.end_date, :maxDate)
              and sub.start_date <= :referenceDate
              and :referenceDate <= coalesce(sub.end_date, :maxDate)
              and r.start_date <= :referenceDate
              and :referenceDate <= coalesce(r.end_date, :maxDate)
            """;

    private static final String ORDER_BY = """
            order by subtype_code
            """;

    private final EntityManager entityManager;

    public ContractSubtypeCatalogLookupAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<ContractSubtypeCatalogItem> listActiveSubtypesByContractType(
            String ruleSystemCode,
            String contractTypeCode
    ) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(BASE_QUERY + ORDER_BY)
                .setParameter("ruleSystemCode", ruleSystemCode)
                .setParameter("contractTypeCode", contractTypeCode)
                .getResultList();

        return rows.stream()
                .map(this::toCatalogItem)
                .toList();
    }

    @Override
    public List<ContractSubtypeCatalogItem> listActiveSubtypesByContractTypeOnDate(
            String ruleSystemCode,
            String contractTypeCode,
            LocalDate referenceDate
    ) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(BASE_QUERY + TEMPORAL_FILTERS + ORDER_BY)
                .setParameter("ruleSystemCode", ruleSystemCode)
                .setParameter("contractTypeCode", contractTypeCode)
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
     * <p>Aqui se publicaban las fechas de {@code subtype} y llegaban nulas al cliente, por dos
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
    private ContractSubtypeCatalogItem toCatalogItem(Object[] row) {
        return new ContractSubtypeCatalogItem(
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
