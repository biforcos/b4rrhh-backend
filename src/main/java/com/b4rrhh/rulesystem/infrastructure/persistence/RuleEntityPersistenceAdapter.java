package com.b4rrhh.rulesystem.infrastructure.persistence;

import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * El adaptador del único puerto de entidades de catálogo (ADR-077 §4, backend#157).
 *
 * <p>Resolver es dos pasos y los dos están aquí: primero la capa —la que la reglamentación monta en
 * el nivel del tipo, de {@code rule_system_layer}—, después la entidad en esa capa. Comparar la capa
 * con la reglamentación funcionaba mientras todo era nacional y la capa nacional se llamaba como su
 * reglamentación; con un tipo en {@code INT} no encuentra nada.
 */
@Component
public class RuleEntityPersistenceAdapter implements RuleEntityRepository {

    /** La capa que una reglamentación monta en el nivel de un tipo: una o ninguna, por la PK (rule_system_code, level). */
    private static final String LAYER_OF_TYPE = """
            select rsl.layer_code, rsl.level
              from rulesystem.rule_system_layer rsl
              join rulesystem.rule_entity_type t on t.level = rsl.level
             where rsl.rule_system_code = :ruleSystemCode
               and t.code = :ruleEntityTypeCode
            """;

    private static final String LAYERS_OF_RULE_SYSTEM = """
            select rsl.layer_code, rsl.level
              from rulesystem.rule_system_layer rsl
             where rsl.rule_system_code = :ruleSystemCode
            """;

    private static final String RULE_SYSTEMS = """
            select distinct rsl.rule_system_code
              from rulesystem.rule_system_layer rsl
             order by 1
            """;

    private final SpringDataRuleEntityRepository springDataRuleEntityRepository;
    private final EntityManager entityManager;

    public RuleEntityPersistenceAdapter(
            SpringDataRuleEntityRepository springDataRuleEntityRepository,
            EntityManager entityManager
    ) {
        this.springDataRuleEntityRepository = springDataRuleEntityRepository;
        this.entityManager = entityManager;
    }

    @Override
    public List<RuleEntity> findAll() {
        return findByFilters(null, null, null, null, null);
    }

    @Override
    public List<RuleEntity> findByFilters(
            String ruleSystemCode,
            String ruleEntityTypeCode,
            String code,
            Boolean active,
            LocalDate referenceDate
    ) {
        List<String> ruleSystemCodes = ruleSystemCode != null ? List.of(ruleSystemCode) : ruleSystemCodes();

        List<RuleEntity> found = new ArrayList<>();
        for (String asked : ruleSystemCodes) {
            Map<String, Integer> layers = ruleEntityTypeCode != null
                    ? layerOf(asked, ruleEntityTypeCode).map(layer -> Map.of(layer.code(), layer.level())).orElse(Map.of())
                    : layersOf(asked);
            if (layers.isEmpty()) {
                continue;
            }

            Specification<RuleEntityEntity> specification = (root, query, criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<>();

                predicates.add(root.get("layerCode").in(layers.keySet()));

                if (ruleEntityTypeCode != null) {
                    predicates.add(criteriaBuilder.equal(root.get("ruleEntityTypeCode"), ruleEntityTypeCode));
                }

                if (code != null) {
                    predicates.add(criteriaBuilder.equal(root.get("code"), code));
                }

                if (active != null) {
                    predicates.add(criteriaBuilder.equal(root.get("active"), active));
                }

                if (referenceDate != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("startDate"), referenceDate));
                    predicates.add(criteriaBuilder.or(
                            criteriaBuilder.isNull(root.get("endDate")),
                            criteriaBuilder.greaterThanOrEqualTo(root.get("endDate"), referenceDate)
                    ));
                }

                return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
            };

            springDataRuleEntityRepository.findAll(specification, Sort.by("ruleEntityTypeCode", "code"))
                    .stream()
                    .map(entity -> toDomain(entity, asked, layers.get(entity.getLayerCode())))
                    .forEach(found::add);
        }
        return found;
    }

    @Override
    public Optional<RuleEntity> findApplicableByBusinessKey(
            String ruleSystemCode,
            String ruleEntityTypeCode,
            String code,
            LocalDate referenceDate
    ) {
        Optional<Layer> layer = layerOf(ruleSystemCode, ruleEntityTypeCode);
        if (layer.isEmpty()) {
            return Optional.empty();
        }

        List<RuleEntityEntity> matches = springDataRuleEntityRepository.findApplicableByBusinessKey(
                layer.get().code(),
                ruleEntityTypeCode,
                code,
                referenceDate,
                SpringDataRuleEntityRepository.MAX_DATE
        );

        if (matches.size() > 1) {
            throw new IllegalStateException(
                    "Multiple applicable rule entities found for business key: "
                            + ruleSystemCode
                            + "/"
                            + ruleEntityTypeCode
                            + "/"
                            + code
                            + " at "
                            + referenceDate
            );
        }

        return matches.stream().findFirst().map(entity -> toDomain(entity, ruleSystemCode, layer.get().level()));
    }

    @Override
    public Optional<RuleEntity> findByBusinessKey(String ruleSystemCode, String ruleEntityTypeCode, String code) {
        return layerOf(ruleSystemCode, ruleEntityTypeCode)
                .flatMap(layer -> springDataRuleEntityRepository
                        .findByLayerCodeAndRuleEntityTypeCodeAndCode(layer.code(), ruleEntityTypeCode, code)
                        .map(entity -> toDomain(entity, ruleSystemCode, layer.level())));
    }

    @Override
    public Optional<RuleEntity> findByBusinessKeyAndStartDate(
            String ruleSystemCode,
            String ruleEntityTypeCode,
            String code,
            LocalDate startDate
    ) {
        return layerOf(ruleSystemCode, ruleEntityTypeCode)
                .flatMap(layer -> springDataRuleEntityRepository
                        .findByLayerCodeAndRuleEntityTypeCodeAndCodeAndStartDate(
                                layer.code(),
                                ruleEntityTypeCode,
                                code,
                                startDate
                        )
                        .map(entity -> toDomain(entity, ruleSystemCode, layer.level())));
    }

    @Override
    public boolean existsOverlapExcludingStartDate(
            String ruleSystemCode,
            String ruleEntityTypeCode,
            String code,
            LocalDate projectedStartDate,
            LocalDate projectedEndDate,
            LocalDate excludedStartDate
    ) {
        LocalDate effectiveProjectedEndDate = projectedEndDate == null
                ? SpringDataRuleEntityRepository.MAX_DATE
                : projectedEndDate;

        return layerOf(ruleSystemCode, ruleEntityTypeCode)
                .map(layer -> springDataRuleEntityRepository.existsOverlapExcludingStartDate(
                        layer.code(),
                        ruleEntityTypeCode,
                        code,
                        projectedStartDate,
                        effectiveProjectedEndDate,
                        excludedStartDate,
                        SpringDataRuleEntityRepository.MAX_DATE
                ))
                .orElse(false);
    }

    @Override
    public List<RuleEntity> findActiveOptions(
            String ruleSystemCode,
            String ruleEntityTypeCode,
            String qLike,
            LocalDate referenceDate
    ) {
        return layerOf(ruleSystemCode, ruleEntityTypeCode)
                .map(layer -> (referenceDate == null
                        ? springDataRuleEntityRepository.findDirectCatalogOptions(layer.code(), ruleEntityTypeCode, qLike)
                        : springDataRuleEntityRepository.findDirectCatalogOptionsOnDate(
                                layer.code(), ruleEntityTypeCode, qLike, referenceDate,
                                SpringDataRuleEntityRepository.MAX_DATE))
                        .stream()
                        .map(entity -> toDomain(entity, ruleSystemCode, layer.level()))
                        .toList())
                .orElseGet(List::of);
    }

    @Override
    public void deleteByBusinessKeyAndStartDate(
            String ruleSystemCode,
            String ruleEntityTypeCode,
            String code,
            LocalDate startDate
    ) {
        layerOf(ruleSystemCode, ruleEntityTypeCode).ifPresent(layer ->
                springDataRuleEntityRepository.deleteByLayerCodeAndRuleEntityTypeCodeAndCodeAndStartDate(
                        layer.code(),
                        ruleEntityTypeCode,
                        code,
                        startDate
                ));
    }

    @Override
    public RuleEntity save(RuleEntity ruleEntity) {
        Layer layer = layerOf(ruleEntity.getRuleSystemCode(), ruleEntity.getRuleEntityTypeCode())
                .orElseThrow(() -> new IllegalStateException(
                        "Rule system " + ruleEntity.getRuleSystemCode()
                                + " mounts no layer for rule entity type " + ruleEntity.getRuleEntityTypeCode()));
        RuleEntityEntity entity = ruleEntity.getId() == null
                ? toNewEntity(ruleEntity, layer)
                : toExistingEntity(ruleEntity, layer);
        RuleEntityEntity saved = springDataRuleEntityRepository.save(entity);
        return toDomain(saved, ruleEntity.getRuleSystemCode(), layer.level());
    }

    private Optional<Layer> layerOf(String ruleSystemCode, String ruleEntityTypeCode) {
        if (ruleSystemCode == null || ruleEntityTypeCode == null) {
            return Optional.empty();
        }
        List<?> rows = entityManager.createNativeQuery(LAYER_OF_TYPE)
                .setParameter("ruleSystemCode", ruleSystemCode)
                .setParameter("ruleEntityTypeCode", ruleEntityTypeCode)
                .getResultList();
        return rows.stream().findFirst().map(row -> toLayer((Object[]) row));
    }

    private Map<String, Integer> layersOf(String ruleSystemCode) {
        List<?> rows = entityManager.createNativeQuery(LAYERS_OF_RULE_SYSTEM)
                .setParameter("ruleSystemCode", ruleSystemCode)
                .getResultList();
        Map<String, Integer> layers = new LinkedHashMap<>();
        for (Object row : rows) {
            Layer layer = toLayer((Object[]) row);
            layers.put(layer.code(), layer.level());
        }
        return layers;
    }

    private List<String> ruleSystemCodes() {
        return entityManager.createNativeQuery(RULE_SYSTEMS).getResultList().stream()
                .map(String::valueOf)
                .toList();
    }

    private static Layer toLayer(Object[] row) {
        return new Layer(String.valueOf(row[0]), ((Number) row[1]).intValue());
    }

    private RuleEntity toDomain(RuleEntityEntity entity, String ruleSystemCode, Integer level) {
        return new RuleEntity(
                entity.getId(),
                ruleSystemCode,
                entity.getLayerCode(),
                level,
                entity.getRuleEntityTypeCode(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.isActive(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private RuleEntityEntity toNewEntity(RuleEntity ruleEntity, Layer layer) {
        RuleEntityEntity entity = new RuleEntityEntity();
        entity.setLayerCode(layer.code());
        entity.setRuleEntityTypeCode(ruleEntity.getRuleEntityTypeCode());
        entity.setCode(ruleEntity.getCode());
        entity.setStartDate(ruleEntity.getStartDate());
        applyMutableFields(entity, ruleEntity);
        return entity;
    }

    private RuleEntityEntity toExistingEntity(RuleEntity ruleEntity, Layer layer) {
        RuleEntityEntity entity = springDataRuleEntityRepository
                .findByLayerCodeAndRuleEntityTypeCodeAndCodeAndStartDate(
                        layer.code(),
                        ruleEntity.getRuleEntityTypeCode(),
                        ruleEntity.getCode(),
                        ruleEntity.getStartDate()
                )
                .orElseThrow(() -> new IllegalStateException(
                        "Rule entity not found for update with business key: "
                                + ruleEntity.getRuleSystemCode()
                                + "/"
                                + ruleEntity.getRuleEntityTypeCode()
                                + "/"
                                + ruleEntity.getCode()
                                + "/"
                                + ruleEntity.getStartDate()
                ));

        applyMutableFields(entity, ruleEntity);
        return entity;
    }

    private void applyMutableFields(RuleEntityEntity entity, RuleEntity ruleEntity) {
        entity.setName(ruleEntity.getName());
        entity.setDescription(ruleEntity.getDescription());
        entity.setActive(ruleEntity.isActive());
        entity.setEndDate(ruleEntity.getEndDate());
    }

    private record Layer(String code, int level) {
    }
}
