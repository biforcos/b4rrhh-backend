package com.b4rrhh.employee.shared.infrastructure.persistence;

import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Lo que una relación de catálogo ata a un código: las categorías de un convenio, los subtipos de
 * un contrato (backend#115, backend#157).
 *
 * <p>Las dos puntas las resuelve el puerto de entidades, por el nivel de su tipo; de la tabla de
 * relación sólo se leen los ids y la vigencia. Antes era un {@code join} a {@code rule_entity} que
 * comparaba la capa con la reglamentación, y con un tipo fuera del nivel 3 no encontraba nada.
 *
 * <p>El orden es el del puerto —por código, en la base—, y por eso se recorre la lista del puerto
 * y no la de la relación: la intercalación de la base no ordena como Java, y el loader elige por
 * posición.
 *
 * <p>Tabla y columnas son constantes del adaptador que lo usa; nada sale de la petición.
 */
public final class RuleEntityRelationReader {

    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    /** Una opción con su vigencia efectiva: la intersección de las dos puntas y la relación. */
    public record RelatedOption(String code, String name, LocalDate startDate, LocalDate endDate) {
    }

    private final EntityManager entityManager;
    private final RuleEntityRepository ruleEntityRepository;
    private final String relationTable;
    private final String fromColumn;
    private final String toColumn;
    private final String fromType;
    private final String toType;

    public RuleEntityRelationReader(
            EntityManager entityManager,
            RuleEntityRepository ruleEntityRepository,
            String relationTable,
            String fromColumn,
            String fromType,
            String toColumn,
            String toType
    ) {
        this.entityManager = entityManager;
        this.ruleEntityRepository = ruleEntityRepository;
        this.relationTable = relationTable;
        this.fromColumn = fromColumn;
        this.toColumn = toColumn;
        this.fromType = fromType;
        this.toType = toType;
    }

    /**
     * Lo que la relación ata al código {@code fromCode}, activo en las tres, y vigente en las tres
     * en {@code referenceDate} si llega. Sin repetidos, en el orden del puerto.
     */
    public List<RelatedOption> related(String ruleSystemCode, String fromCode, LocalDate referenceDate) {
        Map<Long, RuleEntity> from = ruleEntityRepository
                .findByFilters(ruleSystemCode, fromType, null, true, referenceDate).stream()
                .filter(entity -> normalized(entity.getCode()).equals(fromCode))
                .collect(Collectors.toMap(RuleEntity::getId, entity -> entity));
        if (from.isEmpty()) {
            return List.of();
        }

        List<RuleEntity> to = ruleEntityRepository.findByFilters(ruleSystemCode, toType, null, true, referenceDate);
        Map<Long, List<Link>> linksByTo = links(ruleSystemCode, referenceDate).stream()
                .filter(link -> from.containsKey(link.fromId()))
                .collect(Collectors.groupingBy(Link::toId));

        Set<RelatedOption> options = new LinkedHashSet<>();
        for (RuleEntity target : to) {
            for (Link link : linksByTo.getOrDefault(target.getId(), List.of())) {
                RuleEntity source = from.get(link.fromId());
                options.add(new RelatedOption(
                        normalized(target.getCode()),
                        target.getName(),
                        Stream.of(source.getStartDate(), target.getStartDate(), link.startDate())
                                .max(LocalDate::compareTo).orElseThrow(),
                        endOrNull(Stream.of(source.getEndDate(), target.getEndDate(), link.endDate())
                                .map(end -> end == null ? MAX_DATE : end)
                                .min(LocalDate::compareTo).orElseThrow())));
            }
        }
        return new ArrayList<>(options);
    }

    private List<Link> links(String ruleSystemCode, LocalDate referenceDate) {
        String sql = "select r." + fromColumn + ", r." + toColumn + ", r.start_date, r.end_date"
                + " from rulesystem." + relationTable + " r"
                + " join rulesystem.rule_system rs on rs.id = r.rule_system_id"
                + " where upper(trim(rs.code)) = :ruleSystemCode"
                + "   and r.is_active = true"
                + (referenceDate == null ? ""
                        : "   and r.start_date <= :referenceDate"
                        + "   and :referenceDate <= coalesce(r.end_date, :maxDate)");
        var query = entityManager.createNativeQuery(sql).setParameter("ruleSystemCode", ruleSystemCode);
        if (referenceDate != null) {
            query.setParameter("referenceDate", referenceDate);
            query.setParameter("maxDate", MAX_DATE);
        }

        List<Link> links = new ArrayList<>();
        for (Object row : query.getResultList()) {
            Object[] columns = (Object[]) row;
            links.add(new Link(
                    ((Number) columns[0]).longValue(),
                    ((Number) columns[1]).longValue(),
                    toLocalDate(columns[2]),
                    toLocalDate(columns[3])));
        }
        return links;
    }

    private static String normalized(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    /** Un {@code 9999-12-31} en la respuesta se leería como una fecha de verdad. */
    private static LocalDate endOrNull(LocalDate end) {
        return MAX_DATE.equals(end) ? null : end;
    }

    /**
     * Lo que el driver devuelve para una columna {@code date} es {@code java.sql.Date}, no
     * {@code LocalDate} (backend#115).
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

    private record Link(Long fromId, Long toId, LocalDate startDate, LocalDate endDate) {
    }
}
