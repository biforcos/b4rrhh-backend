package com.b4rrhh.rulesystem.workcenter.infrastructure.persistence;

import com.b4rrhh.rulesystem.workcenter.domain.model.WorkCenterContact;
import com.b4rrhh.rulesystem.workcenter.domain.port.WorkCenterContactRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class WorkCenterContactPersistenceAdapter implements WorkCenterContactRepository {

    private final SpringDataWorkCenterContactRepository springDataWorkCenterContactRepository;

    public WorkCenterContactPersistenceAdapter(SpringDataWorkCenterContactRepository springDataWorkCenterContactRepository) {
        this.springDataWorkCenterContactRepository = springDataWorkCenterContactRepository;
    }

    @Override
    public Integer nextContactNumberForWorkCenterRuleEntityId(Long workCenterRuleEntityId) {
        return springDataWorkCenterContactRepository
                .findTopByWorkCenterRuleEntityIdOrderByContactNumberDesc(workCenterRuleEntityId)
                .map(WorkCenterContactEntity::getContactNumber)
                .map(maxContactNumber -> maxContactNumber + 1)
                .orElse(1);
    }

    @Override
    public List<WorkCenterContact> findByWorkCenterRuleEntityIdOrderByContactNumberAsc(Long workCenterRuleEntityId) {
        return springDataWorkCenterContactRepository.findByWorkCenterRuleEntityIdOrderByContactNumberAsc(workCenterRuleEntityId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<WorkCenterContact> findByWorkCenterRuleEntityIdAndContactNumber(Long workCenterRuleEntityId, Integer contactNumber) {
        return springDataWorkCenterContactRepository
                .findByWorkCenterRuleEntityIdAndContactNumber(workCenterRuleEntityId, contactNumber)
                .map(this::toDomain);
    }

    @Override
    public WorkCenterContact save(Long workCenterRuleEntityId, WorkCenterContact contact) {
        WorkCenterContactEntity entity = springDataWorkCenterContactRepository
                .findByWorkCenterRuleEntityIdAndContactNumber(workCenterRuleEntityId, contact.getContactNumber())
                .orElseGet(WorkCenterContactEntity::new);

        entity.setWorkCenterRuleEntityId(workCenterRuleEntityId);
        entity.setContactNumber(contact.getContactNumber());
        entity.setContactTypeCode(contact.getContactTypeCode());
        entity.setContactValue(contact.getContactValue());

        return toDomain(springDataWorkCenterContactRepository.save(entity));
    }

    @Override
    public void deleteByWorkCenterRuleEntityIdAndContactNumber(Long workCenterRuleEntityId, Integer contactNumber) {
        springDataWorkCenterContactRepository.deleteByWorkCenterRuleEntityIdAndContactNumber(
                workCenterRuleEntityId,
                contactNumber
        );
    }

    private WorkCenterContact toDomain(WorkCenterContactEntity entity) {
        return new WorkCenterContact(
                entity.getContactNumber(),
                entity.getContactTypeCode(),
                entity.getContactValue()
        );
    }
}