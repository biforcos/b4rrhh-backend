package com.b4rrhh.payroll.retro.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Los tres numeros de una linea de atraso, y de donde sale cada uno ({@code backend#134}).
 *
 * <p>La documentacion de cada campo vive en el contrato OpenAPI, que es la fuente. Aqui no hay
 * comentarios <b>dentro</b> de la lista de componentes a proposito: el candado
 * {@code TheTwoContractsNeverDivergeInSilenceTest} la lee con una expresion regular, y un bloque de
 * javadoc entre componentes le hace ver campos que no existen.
 */
public record ArrearExplanationResponse(
        String originPeriodCode,
        String conceptCode,
        String conceptLabel,
        BigDecimal lineAmount,
        BigDecimal currentValue,
        Instant currentValueCalculatedAt,
        BigDecimal alreadyPaid,
        List<ArrearPaidInResponse> paidIn,
        BigDecimal difference,
        boolean addsUp
) {
}
