package com.b4rrhh.rulesystem.workcenter.application.port;

import java.util.Optional;

public interface WorkCenterContactCatalogReadPort {

    Optional<String> findContactTypeName(String ruleSystemCode, String contactTypeCode);
}