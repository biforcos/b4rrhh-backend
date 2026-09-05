package com.b4rrhh.employee.contract.infrastructure.persistence;

import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.contract.domain.port.ContractRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class ContractPersistenceAdapter implements ContractRepository {

    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final SpringDataContractRepository springDataContractRepository;

    public ContractPersistenceAdapter(
            SpringDataContractRepository springDataContractRepository
    ) {
        this.springDataContractRepository = springDataContractRepository;
    }

    @Override
    public Optional<Contract> findByEmployeeIdAndStartDate(Long employeeId, LocalDate startDate) {
        return springDataContractRepository
                .findByEmployeeIdAndStartDate(employeeId, startDate)
                .map(this::toDomain);
    }

    @Override
    public List<Contract> findByEmployeeIdOrderByStartDate(Long employeeId) {
        return springDataContractRepository
                .findByEmployeeIdOrderByStartDateAsc(employeeId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsOverlappingPeriod(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate excludeStartDateOrNull
    ) {
        LocalDate effectiveEndDate = endDate == null ? MAX_DATE : endDate;
        return springDataContractRepository.existsOverlappingPeriod(
                employeeId,
                startDate,
                effectiveEndDate,
                MAX_DATE,
                excludeStartDateOrNull
        );
    }

    @Override
    public void save(Contract contract) {
        springDataContractRepository.save(toEntity(contract));
    }

    @Override
    public void update(Contract contract, LocalDate originalStartDate) {
        ContractEntity entity = springDataContractRepository
                .findByEmployeeIdAndStartDate(contract.getEmployeeId(), originalStartDate)
                .orElseThrow(() -> new IllegalStateException(
                "Contract not found for update by functional identity"
                ));

        entity.setContractCode(contract.getContractCode());
        entity.setContractSubtypeCode(contract.getContractSubtypeCode());
        entity.setStartDate(contract.getStartDate());
        entity.setEndDate(contract.getEndDate());
        springDataContractRepository.save(entity);
    }

    @Override
    public void delete(Contract contract) {
        ContractEntity entity = springDataContractRepository
                .findByEmployeeIdAndStartDate(contract.getEmployeeId(), contract.getStartDate())
                .orElseThrow(() -> new IllegalStateException(
                        "Contract not found for delete by functional identity"
                ));

        springDataContractRepository.delete(entity);
    }

    private Contract toDomain(ContractEntity entity) {
        return new Contract(
                entity.getEmployeeId(),
                entity.getContractCode(),
                entity.getContractSubtypeCode(),
                entity.getStartDate(),
                entity.getEndDate()
        );
    }

    private ContractEntity toEntity(Contract contract) {
        ContractEntity entity = new ContractEntity();
        entity.setEmployeeId(contract.getEmployeeId());
        entity.setContractCode(contract.getContractCode());
        entity.setContractSubtypeCode(contract.getContractSubtypeCode());
        entity.setStartDate(contract.getStartDate());
        entity.setEndDate(contract.getEndDate());
        return entity;
    }
}
