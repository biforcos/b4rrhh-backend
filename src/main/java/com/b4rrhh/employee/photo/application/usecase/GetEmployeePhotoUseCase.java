package com.b4rrhh.employee.photo.application.usecase;

import java.util.Optional;

public interface GetEmployeePhotoUseCase {

    Optional<EmployeePhotoContent> get(GetEmployeePhotoQuery query);
}
