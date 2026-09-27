package com.b4rrhh.payroll.retro.infrastructure.web;

import com.b4rrhh.payroll.retro.application.usecase.ListedRetroMark;
import com.b4rrhh.payroll.retro.domain.model.RetroMark;
import com.b4rrhh.payroll.retro.infrastructure.web.dto.RetroMarkResponse;
import org.springframework.stereotype.Component;

@Component
public class RetroMarkWebMapper {

    /** Una marca suelta, como la devuelve el descarte: ya no espera a nadie. */
    public RetroMarkResponse toResponse(RetroMark m) {
        return toResponse(m, false);
    }

    public RetroMarkResponse toResponse(ListedRetroMark listed) {
        return toResponse(listed.mark(), listed.withoutAReceiptToPayIt());
    }

    private RetroMarkResponse toResponse(RetroMark m, boolean withoutAReceiptToPayIt) {
        return new RetroMarkResponse(
                m.getId(),
                m.getPresenceNumber(),
                m.getFromPeriodCode(),
                m.getStatus().name(),
                m.getCreatedAt(),
                m.getSource().verticalCode(),
                m.getSource().table(),
                m.getSource().rowId(),
                m.getSource().rowKey(),
                m.getDiscardedAt(),
                m.getDiscardedBy(),
                m.getDiscardReason(),
                m.getConsumedAt(),
                m.getConsumedPeriodCode(),
                m.getConsumedRunId(),
                withoutAReceiptToPayIt);
    }
}
