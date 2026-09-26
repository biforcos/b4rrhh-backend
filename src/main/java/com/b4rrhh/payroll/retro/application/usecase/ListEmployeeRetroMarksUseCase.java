package com.b4rrhh.payroll.retro.application.usecase;

import com.b4rrhh.payroll.retro.domain.model.RetroMark;

import java.util.List;

public interface ListEmployeeRetroMarksUseCase {
    List<RetroMark> list(ListEmployeeRetroMarksCommand command);
}
