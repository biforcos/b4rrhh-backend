package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.application.port.TerminationParticipant;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeCatalogValueInvalidException;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeConflictException;
import com.b4rrhh.employee.presence.application.usecase.ClosePresenceCommand;
import com.b4rrhh.employee.presence.application.usecase.ClosePresenceUseCase;
import com.b4rrhh.employee.presence.application.usecase.ListEmployeePresencesUseCase;
import com.b4rrhh.employee.presence.domain.exception.PresenceCatalogValueInvalidException;
import com.b4rrhh.employee.presence.domain.model.Presence;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PresenceTerminationParticipant implements TerminationParticipant {

    private final ListEmployeePresencesUseCase listPresences;
    private final ClosePresenceUseCase closePresence;

    public PresenceTerminationParticipant(
            ListEmployeePresencesUseCase listPresences,
            ClosePresenceUseCase closePresence) {
        this.listPresences = listPresences;
        this.closePresence = closePresence;
    }

    /**
     * El primero, y no el ultimo.
     *
     * El cese cierra todo lo que deriva de la presencia del empleado en la
     * empresa. La presencia es la raiz: si se cierra al final, cada vertical
     * que se cierra antes valida sus periodos contra una presencia que todavia
     * llega hasta 9999-12-31, y la cobertura nunca cuadra. Cerrandola primero,
     * todos los demas se comprueban contra el periodo que de verdad va a
     * quedar.
     *
     * La regla para un vertical nuevo es entonces sencilla: si deriva de la
     * presencia, va detras de ella. El orden entre derivados da igual.
     *
     * Cerrar de dentro afuera, como un destructor, era lo intuitivo. Por eso
     * estaba asi. Pero el invariante de cobertura no lo admite.
     */
    @Override
    public int order() { return 5; }

    @Override
    public void participate(TerminationContext ctx) {
        List<Presence> all = listPresences.listByEmployeeBusinessKey(
                ctx.ruleSystemCode(), ctx.employeeTypeCode(), ctx.employeeNumber());

        List<Presence> deduplicated = deduplicate(all);

        Presence activePresence = deduplicated.stream()
                .filter(p -> p.getEndDate() == null)
                .findFirst()
                .orElseThrow(() -> new TerminateEmployeeConflictException(
                        "No active presence found for employee " + ctx.employeeNumber()));

        try {
            Presence closed = closePresence.close(new ClosePresenceCommand(
                    ctx.ruleSystemCode(), ctx.employeeTypeCode(), ctx.employeeNumber(),
                    activePresence.getPresenceNumber(), ctx.terminationDate(), ctx.exitReasonCode()));
            ctx.setClosedPresence(closed);
        } catch (PresenceCatalogValueInvalidException e) {
            throw new TerminateEmployeeCatalogValueInvalidException(e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new TerminateEmployeeConflictException(e.getMessage(), e);
        }
    }

    private List<Presence> deduplicate(List<Presence> presences) {
        Map<Integer, Presence> byNumber = presences.stream().collect(
                Collectors.toMap(
                        Presence::getPresenceNumber,
                        p -> p,
                        (existing, candidate) -> {
                            if (existing.getEndDate() == null && candidate.getEndDate() != null) return candidate;
                            if (existing.getEndDate() != null && candidate.getEndDate() == null) return existing;
                            if (existing.getEndDate() != null && candidate.getEndDate() != null) {
                                return existing.getEndDate().isAfter(candidate.getEndDate()) ? existing : candidate;
                            }
                            return existing;
                        }));
        return List.copyOf(byNumber.values());
    }
}
