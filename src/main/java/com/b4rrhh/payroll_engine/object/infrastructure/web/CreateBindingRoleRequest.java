package com.b4rrhh.payroll_engine.object.infrastructure.web;

import jakarta.validation.constraints.NotBlank;

public record CreateBindingRoleRequest(@NotBlank String bindingRoleCode) {
}
