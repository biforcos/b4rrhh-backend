package com.b4rrhh.payroll.domain.port;

import com.b4rrhh.payroll.domain.model.CalculationRun;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CalculationRunRepository {

    CalculationRun save(CalculationRun calculationRun);

    Optional<CalculationRun> findById(Long id);

    /**
     * Las ejecuciones que estan en alguno de estos estados. Lo pide el barrido de arranque
     * para encontrar las que se quedaron a medias (#75).
     */
    List<CalculationRun> findByStatusIn(Collection<String> statuses);
}