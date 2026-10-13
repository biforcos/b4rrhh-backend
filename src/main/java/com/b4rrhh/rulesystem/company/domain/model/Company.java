package com.b4rrhh.rulesystem.company.domain.model;

import com.b4rrhh.geo.address.domain.model.PostalAddress;
import java.time.LocalDate;

public record Company(
        String ruleSystemCode,
        String companyCode,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        String legalName,
        String taxIdentifier,
        PostalAddress address,
        /**
         * La actividad economica de la empresa, en CNAE ({@code backend#122}).
         *
         * <p>Llega hasta aqui porque se ve y se edita en la pantalla de la empresa, que es donde
         * un tecnico de nominas lo busca. De el sale el tipo de la cuota de accidentes de
         * trabajo.
         */
        String cnaeCode
) {
}
