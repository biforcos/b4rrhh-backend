package com.b4rrhh.rulesystem.catalogoption.application.usecase;

import com.b4rrhh.rulesystem.catalogoption.application.query.GetDirectCatalogOptionsQuery;
import com.b4rrhh.rulesystem.catalogoption.domain.model.DirectCatalogOption;
import com.b4rrhh.rulesystem.catalogoption.domain.port.DirectCatalogOptionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class GetDirectCatalogOptionsService implements GetDirectCatalogOptionsUseCase {

    private final DirectCatalogOptionRepository directCatalogOptionRepository;

    public GetDirectCatalogOptionsService(DirectCatalogOptionRepository directCatalogOptionRepository) {
        this.directCatalogOptionRepository = directCatalogOptionRepository;
    }

    @Override
    public DirectCatalogOptionsResult get(GetDirectCatalogOptionsQuery query) {
        String normalizedRuleSystemCode = normalizeRequired("ruleSystemCode", query.ruleSystemCode());
        String normalizedRuleEntityTypeCode = normalizeRequired("ruleEntityTypeCode", query.ruleEntityTypeCode());
        String normalizedQLike = normalizeLike(query.q());

        // Una sola fecha, y solo para fechar la marca (backend#32). Antes habia dos —la
        // pedida, que filtraba, y la efectiva, que marcaba— y esa distincion es justo la
        // que se va: el catalogo se devuelve entero y la fecha dice respecto a que dia se
        // mira la vigencia de cada opcion.
        LocalDate effectiveReferenceDate = query.referenceDate() != null
                ? query.referenceDate()
                : LocalDate.now();

        List<DirectCatalogOption> items = directCatalogOptionRepository
                .findDirectOptions(
                        normalizedRuleSystemCode,
                        normalizedRuleEntityTypeCode,
                        normalizedQLike
                )
                .stream()
                .map(option -> option.withActive(option.isEffectiveOn(effectiveReferenceDate)))
                .toList();

        return new DirectCatalogOptionsResult(
                normalizedRuleSystemCode,
                normalizedRuleEntityTypeCode,
                effectiveReferenceDate,
                items
        );
    }

    private String normalizeRequired(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        return value.trim();
    }

    private String normalizeLike(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return "%" + value.trim().toLowerCase() + "%";
    }
}
