package com.b4rrhh.employee.absence.application.usecase;

import com.b4rrhh.employee.absence.domain.exception.AbsenceCatalogValueInvalidException;
import com.b4rrhh.employee.absence.domain.exception.AbsenceEmployeeNotFoundException;
import com.b4rrhh.employee.absence.domain.exception.AbsenceOutsidePresencePeriodException;
import com.b4rrhh.employee.absence.domain.exception.AbsenceOverlapException;
import com.b4rrhh.employee.absence.domain.exception.InvalidAbsenceDateRangeException;
import com.b4rrhh.employee.absence.domain.model.Absence;
import com.b4rrhh.employee.absence.domain.port.AbsenceRepository;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.presence.application.usecase.ListEmployeePresencesUseCase;
import com.b4rrhh.employee.presence.domain.model.Presence;
import com.b4rrhh.employee.shared.application.port.DatedWrite;
import com.b4rrhh.employee.shared.application.port.DatedWriteNoticePort;
import com.b4rrhh.employee.shared.application.port.DatedWriteSources;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class UpsertAbsenceService implements UpsertAbsenceUseCase {

    private static final String TABLA = "employee.employee_absence";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RuleEntityRepository ruleEntityRepository;
    private final GetEmployeeByBusinessKeyUseCase getEmployee;
    private final ListEmployeePresencesUseCase listPresences;
    private final AbsenceRepository absenceRepository;
    private final DatedWriteNoticePort datedWrites;

    public UpsertAbsenceService(
            RuleEntityRepository ruleEntityRepository,
            GetEmployeeByBusinessKeyUseCase getEmployee,
            ListEmployeePresencesUseCase listPresences,
            AbsenceRepository absenceRepository,
            DatedWriteNoticePort datedWrites
    ) {
        this.ruleEntityRepository = ruleEntityRepository;
        this.getEmployee = getEmployee;
        this.listPresences = listPresences;
        this.absenceRepository = absenceRepository;
        this.datedWrites = datedWrites;
    }

    @Override
    @Transactional
    public Absence upsert(UpsertAbsenceCommand command) {
        String ruleSystemCode   = command.ruleSystemCode();
        String employeeTypeCode = command.employeeTypeCode();
        String employeeNumber   = command.employeeNumber();
        String absenceTypeCode  = command.absenceTypeCode();
        LocalDate startDate     = command.startDate();
        int startTime           = command.startTime();
        LocalDate endDate       = command.endDate();
        Integer endTime         = command.endTime();

        // 1. Validate absence type catalog
        ruleEntityRepository.findByBusinessKey(ruleSystemCode, AbsenceRuleEntityTypeCodes.EMPLOYEE_ABSENCE_TYPE, absenceTypeCode)
                .orElseThrow(() -> new AbsenceCatalogValueInvalidException("absenceTypeCode", absenceTypeCode));

        // 2. Validate employee exists
        // No existir es un 404 como en toda la ficha (b4rrhh/backend#144). Estar de baja no se mira
        // aqui: no hay estado guardado (b4rrhh/backend#148), y lo que de verdad importa —que la
        // ausencia caiga en una presencia— lo comprueba el paso 3 con las fechas de la ausencia.
        Employee employee = getEmployee.getByBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber)
                .orElseThrow(() -> new AbsenceEmployeeNotFoundException(
                        "No existe el empleado " + ruleSystemCode + "/" + employeeTypeCode + "/" + employeeNumber + "."));

        Long employeeId = employee.getId();

        // 3. La ausencia cae entera dentro de UNA presencia (b4rrhh/backend#147). Comprobar el inicio
        // y el fin cada uno contra cualquiera dejaba pasar la que salta el hueco entre un cese y una
        // readmision: empieza en la primera y acaba en la segunda. Eso son dos ausencias o un error
        // de fechas, y en los dos casos se dice.
        List<Presence> presences = listPresences.listByEmployeeBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber);
        requireWithinOnePresence(presences, employeeNumber, startDate, endDate);

        // 4. Validate date range
        if (endDate != null) {
            if (endDate.isBefore(startDate)) {
                throw new InvalidAbsenceDateRangeException(
                        "endDate must not be before startDate");
            }
            if (endDate.isEqual(startDate) && endTime != null && endTime <= startTime) {
                throw new InvalidAbsenceDateRangeException(
                        "endTime must be after startTime when absence starts and ends on the same day");
            }
        }

        // 5. Look up existing absence by business key
        Optional<Absence> existing = absenceRepository.findByKey(employeeId, absenceTypeCode, startDate, startTime);

        Absence guardada;
        if (existing.isPresent()) {
            Absence current = existing.get();
            if (absenceRepository.existsOverlappingAbsenceExcluding(employeeId, startDate, endDate, current.getId())) {
                throw new AbsenceOverlapException(
                        "Absence overlaps with an existing absence for employee " + employeeNumber);
            }
            Absence updated = current.update(endDate, endTime, command.benefitEntitledOrDefault());
            guardada = absenceRepository.save(updated);
        } else {
            if (absenceRepository.existsOverlappingAbsence(employeeId, startDate, endDate)) {
                throw new AbsenceOverlapException(
                        "Absence overlaps with an existing absence for employee " + employeeNumber);
            }
            Absence newAbsence = Absence.create(employeeId, absenceTypeCode, startDate, startTime,
                    endDate, endTime, command.benefitEntitledOrDefault());
            guardada = absenceRepository.save(newAbsence);
        }

        // Por la fecha de INICIO y no por la de fin, aunque una baja del 28 de agosto al 4 de
        // septiembre mueva los dos meses: quien recalcula lo hace hacia delante desde el mas antiguo
        // (el #131), asi que decir agosto ya dice septiembre. Al contrario no (backend#130).
        //
        // La fecha es la del mandato y no la de lo guardado: la sabe quien llama, asi que no hay razon
        // para ir a buscarla. El id si sale de lo guardado, porque en un alta solo lo sabe el
        // repositorio.
        datedWrites.notice(DatedWrite.on(startDate,
                ruleSystemCode, employeeTypeCode, employeeNumber,
                DatedWriteSources.ABSENCE, TABLA, guardada.getId()));

        return guardada;
    }

    private static void requireWithinOnePresence(
            List<Presence> presences, String employeeNumber, LocalDate startDate, LocalDate endDate) {
        Presence presence = presences.stream()
                .filter(p -> !p.getStartDate().isAfter(startDate)
                        && (p.getEndDate() == null || !p.getEndDate().isBefore(startDate)))
                .findFirst()
                .orElseThrow(() -> new AbsenceOutsidePresencePeriodException(
                        "La ausencia empieza el " + DAY.format(startDate) + " y " + employeeNumber
                                + " no tiene presencia ese día."));
        LocalDate presenceEnd = presence.getEndDate();
        if (presenceEnd == null) return;
        if (endDate == null || endDate.isAfter(presenceEnd)) {
            String until = endDate == null ? "no tiene fin" : "acaba el " + DAY.format(endDate);
            throw new AbsenceOutsidePresencePeriodException(
                    "La ausencia empieza en la presencia " + presence.getPresenceNumber()
                            + " (del " + DAY.format(presence.getStartDate()) + " al " + DAY.format(presenceEnd)
                            + ") y " + until + ": tiene que acabar como tarde el " + DAY.format(presenceEnd)
                            + ". Una ausencia cae entera dentro de una presencia; si sigue en la siguiente, son dos.");
        }
    }
}
