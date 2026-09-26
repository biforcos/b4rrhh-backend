package com.b4rrhh.payroll.retro.application.usecase;

import com.b4rrhh.payroll.retro.domain.model.RetroMark;

public interface DiscardRetroMarkUseCase {
    RetroMark discard(DiscardRetroMarkCommand command);
}
