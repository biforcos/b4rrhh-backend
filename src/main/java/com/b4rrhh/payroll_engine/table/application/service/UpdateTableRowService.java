package com.b4rrhh.payroll_engine.table.application.service;

import com.b4rrhh.payroll_engine.table.application.usecase.UpdateTableRowCommand;
import com.b4rrhh.payroll_engine.table.application.usecase.UpdateTableRowUseCase;
import com.b4rrhh.payroll_engine.table.domain.exception.TableRowNotFoundException;
import com.b4rrhh.payroll_engine.table.domain.model.PayrollTableRow;
import com.b4rrhh.payroll_engine.metamodel.domain.model.MetamodelValidityWindow;
import com.b4rrhh.payroll_engine.table.domain.port.PayrollTableRowManagementPort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class UpdateTableRowService implements UpdateTableRowUseCase {

    private final PayrollTableRowManagementPort port;

    public UpdateTableRowService(PayrollTableRowManagementPort port) {
        this.port = port;
    }

    @Override
    public PayrollTableRow update(UpdateTableRowCommand cmd) {
        PayrollTableRow existing = port.findById(cmd.id())
                .orElseThrow(() -> new TableRowNotFoundException(cmd.id()));

        // Es una actualizacion parcial: lo que hay que comprobar es la ventana que queda,
        // no la que viene. Mandar solo un endDate a mitad de mes tambien rompe la regla.
        LocalDate startDate = cmd.startDate() != null ? cmd.startDate() : existing.getStartDate();
        LocalDate endDate = cmd.endDate() != null ? cmd.endDate() : existing.getEndDate();
        MetamodelValidityWindow.requireWholePeriods(
                startDate,
                endDate,
                "la fila " + cmd.id() + " de la tabla " + existing.getTableCode(),
                "startDate",
                "endDate"
        );

        return port.save(new PayrollTableRow(
                existing.getId(),
                existing.getRuleSystemCode(),
                existing.getTableCode(),
                cmd.searchCode()     != null ? cmd.searchCode()     : existing.getSearchCode(),
                startDate,
                endDate,
                cmd.monthlyValue()   != null ? cmd.monthlyValue()   : existing.getMonthlyValue(),
                cmd.annualValue()    != null ? cmd.annualValue()    : existing.getAnnualValue(),
                cmd.dailyValue()     != null ? cmd.dailyValue()     : existing.getDailyValue(),
                cmd.hourlyValue()    != null ? cmd.hourlyValue()    : existing.getHourlyValue(),
                cmd.active()         != null ? cmd.active()         : existing.isActive()
        ));
    }
}
