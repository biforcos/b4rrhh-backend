package com.b4rrhh.rulesystem.workcenter.infrastructure.persistence;

import com.b4rrhh.geo.address.infrastructure.persistence.PostalAddressColumns;
import com.b4rrhh.rulesystem.workcenter.domain.model.WorkCenterProfile;
import com.b4rrhh.rulesystem.workcenter.domain.port.WorkCenterProfileRepository;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class WorkCenterProfilePersistenceAdapter implements WorkCenterProfileRepository {

    private final SpringDataWorkCenterProfileRepository springDataWorkCenterProfileRepository;

    public WorkCenterProfilePersistenceAdapter(SpringDataWorkCenterProfileRepository springDataWorkCenterProfileRepository) {
        this.springDataWorkCenterProfileRepository = springDataWorkCenterProfileRepository;
    }

    @Override
    public Optional<WorkCenterProfile> findByWorkCenterRuleEntityId(Long workCenterRuleEntityId) {
        return springDataWorkCenterProfileRepository.findByWorkCenterRuleEntityId(workCenterRuleEntityId)
                .map(this::toDomain);
    }

    @Override
    public Map<Long, WorkCenterProfile> findByWorkCenterRuleEntityIds(Iterable<Long> workCenterRuleEntityIds) {
        java.util.List<Long> ids = new java.util.ArrayList<>();
        workCenterRuleEntityIds.forEach(ids::add);

        Map<Long, WorkCenterProfile> result = new HashMap<>();
        springDataWorkCenterProfileRepository.findByWorkCenterRuleEntityIdIn(ids)
                .forEach(entity -> result.put(entity.getWorkCenterRuleEntityId(), toDomain(entity)));
        return result;
    }

    @Override
    public WorkCenterProfile save(Long workCenterRuleEntityId, WorkCenterProfile workCenterProfile) {
        WorkCenterProfileEntity entity = springDataWorkCenterProfileRepository
                .findByWorkCenterRuleEntityId(workCenterRuleEntityId)
                .orElseGet(WorkCenterProfileEntity::new);

        entity.setWorkCenterRuleEntityId(workCenterRuleEntityId);
        entity.setCompanyCode(workCenterProfile.getCompanyCode());
        entity.setAddress(PostalAddressColumns.of(workCenterProfile.getAddress()));

        WorkCenterProfileEntity saved = springDataWorkCenterProfileRepository.save(entity);
        // La dirección tal y como se escribió: el nombre del municipio es una fórmula que Hibernate
        // no lee hasta volver a cargar la fila.
        return new WorkCenterProfile(saved.getCompanyCode(), workCenterProfile.getAddress());
    }

    private WorkCenterProfile toDomain(WorkCenterProfileEntity entity) {
        return new WorkCenterProfile(
                entity.getCompanyCode(),
                entity.getAddress() == null ? null : entity.getAddress().toDomainOrNull()
        );
    }
}