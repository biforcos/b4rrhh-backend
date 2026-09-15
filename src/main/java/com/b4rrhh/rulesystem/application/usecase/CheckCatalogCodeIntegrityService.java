package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.application.port.CatalogCodeIntegrityReadPort;
import org.springframework.stereotype.Service;

@Service
public class CheckCatalogCodeIntegrityService implements CheckCatalogCodeIntegrityUseCase {

    private final CatalogCodeIntegrityReadPort catalogCodeIntegrityReadPort;

    public CheckCatalogCodeIntegrityService(CatalogCodeIntegrityReadPort catalogCodeIntegrityReadPort) {
        this.catalogCodeIntegrityReadPort = catalogCodeIntegrityReadPort;
    }

    @Override
    public CatalogCodeIntegrityReport check() {
        return new CatalogCodeIntegrityReport(catalogCodeIntegrityReadPort.readAll());
    }
}
