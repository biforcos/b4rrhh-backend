package com.b4rrhh.employee.address.infrastructure.persistence;

import com.b4rrhh.employee.address.domain.model.Address;
import com.b4rrhh.employee.address.domain.port.AddressRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class AddressPersistenceAdapter implements AddressRepository {

    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final SpringDataAddressRepository springDataAddressRepository;

    public AddressPersistenceAdapter(SpringDataAddressRepository springDataAddressRepository) {
        this.springDataAddressRepository = springDataAddressRepository;
    }

    @Override
    public Optional<Address> findByEmployeeIdAndAddressNumber(Long employeeId, Integer addressNumber) {
        return springDataAddressRepository.findByEmployeeIdAndAddressNumber(employeeId, addressNumber)
                .map(this::toDomain);
    }

    @Override
    public List<Address> findByEmployeeIdOrderByStartDate(Long employeeId) {
        return springDataAddressRepository.findByEmployeeIdOrderByStartDateAsc(employeeId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Address> findByEmployeeIdAndAddressTypeCodeOrderByStartDate(Long employeeId, String addressTypeCode) {
        return springDataAddressRepository.findByEmployeeIdAndAddressTypeCodeOrderByStartDateAsc(employeeId, addressTypeCode)
                .stream()
                .map(this::toDomain)
                .toList();
    }

        @Override
        public boolean existsOverlappingPeriodByAddressType(
            Long employeeId,
            String addressTypeCode,
            LocalDate startDate,
            LocalDate endDate
        ) {
        LocalDate effectiveEndDate = endDate == null ? MAX_DATE : endDate;
        return springDataAddressRepository.existsOverlappingPeriodByAddressType(
            employeeId,
            addressTypeCode,
            startDate,
            effectiveEndDate,
            MAX_DATE
        );
        }

    @Override
    public Optional<Integer> findMaxAddressNumberByEmployeeId(Long employeeId) {
        return Optional.ofNullable(springDataAddressRepository.findMaxAddressNumberByEmployeeId(employeeId));
    }

    @Override
    public Address save(Address address) {
        AddressEntity entity = toEntity(address);
        AddressEntity saved = springDataAddressRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void delete(Address address) {
        springDataAddressRepository.deleteById(address.getId());
    }

    private Address toDomain(AddressEntity entity) {
        return new Address(
                entity.getId(),
                entity.getEmployeeId(),
                entity.getAddressNumber(),
                entity.getAddressTypeCode(),
                entity.getStreet(),
                entity.getCity(),
                entity.getCountryCode(),
                entity.getPostalCode(),
                entity.getRegionCode(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private AddressEntity toEntity(Address address) {
        AddressEntity entity = new AddressEntity();
        entity.setId(address.getId());
        entity.setEmployeeId(address.getEmployeeId());
        entity.setAddressNumber(address.getAddressNumber());
        entity.setAddressTypeCode(address.getAddressTypeCode());
        entity.setStreet(address.getStreet());
        entity.setCity(address.getCity());
        entity.setCountryCode(address.getCountryCode());
        entity.setPostalCode(address.getPostalCode());
        entity.setRegionCode(address.getRegionCode());
        entity.setStartDate(address.getStartDate());
        entity.setEndDate(address.getEndDate());
        entity.setCreatedAt(address.getCreatedAt());
        entity.setUpdatedAt(address.getUpdatedAt());
        return entity;
    }
}
