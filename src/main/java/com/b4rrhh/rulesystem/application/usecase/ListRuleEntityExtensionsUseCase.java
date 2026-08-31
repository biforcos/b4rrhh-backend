package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.domain.model.RuleEntityExtension;

import java.util.List;

public interface ListRuleEntityExtensionsUseCase {

    /** Todas las extensiones declaradas (ADR-053 §2). El metamodelo describe; no ejecuta. */
    List<RuleEntityExtension> listAll();
}
