package com.b4rrhh.rulesystem.domain.port;

import com.b4rrhh.rulesystem.domain.model.RuleEntityExtension;

import java.util.List;

public interface RuleEntityExtensionRepository {

    /** Todas las extensiones declaradas en el metamodelo, para agrupar por tipo. */
    List<RuleEntityExtension> findAll();
}
