package com.b4rrhh.payroll.retro.application.usecase;

import com.b4rrhh.payroll.retro.domain.model.RetroMark;

import java.util.List;

public interface ListEmployeeRetroMarksUseCase {
    List<RetroMark> list(ListEmployeeRetroMarksCommand command);

    /** Las mismas, cada una con si le queda algun recibo que la pague ({@code backend#139}). */
    List<ListedRetroMark> listWithReceiptStatus(ListEmployeeRetroMarksCommand command);
}
