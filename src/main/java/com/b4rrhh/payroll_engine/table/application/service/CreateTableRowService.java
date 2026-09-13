package com.b4rrhh.payroll_engine.table.application.service;

import com.b4rrhh.payroll_engine.table.application.usecase.CreateTableRowCommand;
import com.b4rrhh.payroll_engine.table.application.usecase.CreateTableRowUseCase;
import com.b4rrhh.payroll_engine.table.domain.exception.TableRowAlreadyExistsException;
import com.b4rrhh.payroll_engine.table.domain.model.PayrollTableRow;
import com.b4rrhh.payroll_engine.metamodel.domain.model.MetamodelValidityWindow;
import com.b4rrhh.payroll_engine.table.domain.port.PayrollTableRowManagementPort;
import org.springframework.stereotype.Service;

@Service
public class CreateTableRowService implements CreateTableRowUseCase {

    private final PayrollTableRowManagementPort port;

    public CreateTableRowService(PayrollTableRowManagementPort port) {
        this.port = port;
    }

    @Override
    public PayrollTableRow create(CreateTableRowCommand cmd) {
        MetamodelValidityWindow.requireWholePeriods(
                cmd.startDate(),
                cmd.endDate(),
                "la fila " + cmd.searchCode() + " de la tabla " + cmd.tableCode(),
                "startDate",
                "endDate"
        );

        if (port.existsByBusinessKey(cmd.ruleSystemCode(), cmd.tableCode(), cmd.searchCode(), cmd.startDate())) {
            throw new TableRowAlreadyExistsException(cmd.tableCode(), cmd.searchCode(), cmd.startDate());
        }
        return port.save(new PayrollTableRow(
                null, cmd.ruleSystemCode(), cmd.tableCode(), cmd.searchCode(),
                cmd.startDate(), cmd.endDate(),
                cmd.monthlyValue(), cmd.annualValue(), cmd.dailyValue(), cmd.hourlyValue(),
                true
        ));
    }
}
