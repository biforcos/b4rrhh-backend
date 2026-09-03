package com.b4rrhh.employee.employee.application;

import com.b4rrhh.employee.employee.application.port.DisplayNameFormatLookupPort;
import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model.DisplayNameFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * El nombre para mostrar de un empleado, en este orden (backend#42):
 *
 * <ol>
 *   <li>El sustituto ({@code preferredName}), si lo hay. No es un hipocorístico: quien lo
 *       rellena dice «llamadme así», y gana sobre cualquier formato. Se muestra solo, sin
 *       componer con los apellidos.</li>
 *   <li>El formato normativo de la reglamentación ({@code employee_display_name_format}).
 *       ESP lo trae sembrado desde la V115.</li>
 *   <li>Sin formato configurado, lo que hay escrito, tal cual y separado por espacios.</li>
 * </ol>
 *
 * El tercer caso existe porque el formato puede faltar: las reglamentaciones se crean por
 * API y nacen sin él, y FRA y PRT vienen sembradas sin él, igual que sin numeración (V99).
 * Es configuración de quien usa la reglamentación, no una invariante del esquema, así que
 * no es un {@code RequiredExtensionMissingException} (backend#34) y no tumba la lectura
 * de la ficha ni del directorio. Pero tampoco puede pasar desapercibido, que es lo que
 * pasó durante meses con un formato puesto a mano: el relleno no aplica ningún formato
 * —ni mayúsculas ni orden— y la primera vez que se usa para cada reglamentación se avisa
 * en el log, con la reglamentación y el arreglo, para que se vea sin llenar el log con
 * una línea por fila del directorio.
 */
@Service
public class DisplayNameComputationService {

    private static final Logger log = LoggerFactory.getLogger(DisplayNameComputationService.class);

    private final DisplayNameFormatLookupPort formatLookupPort;
    private final Set<String> ruleSystemsWarnedWithoutFormat = ConcurrentHashMap.newKeySet();

    public DisplayNameComputationService(DisplayNameFormatLookupPort formatLookupPort) {
        this.formatLookupPort = formatLookupPort;
    }

    public String compute(
            String ruleSystemCode,
            String firstName,
            String lastName1,
            String lastName2,
            String preferredName) {

        if (preferredName != null && !preferredName.isBlank()) {
            return preferredName.trim();
        }

        return formatLookupPort.findFormatCodeForRuleSystem(ruleSystemCode)
                .map(code -> DisplayNameFormatter.format(firstName, lastName1, lastName2, code))
                .orElseGet(() -> asEntered(ruleSystemCode, firstName, lastName1, lastName2));
    }

    private String asEntered(String ruleSystemCode, String firstName, String lastName1, String lastName2) {
        if (ruleSystemsWarnedWithoutFormat.add(ruleSystemCode)) {
            log.warn("Rule system {} has no employee display name format configured: "
                            + "showing names as entered, without any format. "
                            + "Configure one at PUT /rule-systems/{}/employee-display-name-format",
                    ruleSystemCode, ruleSystemCode);
        }
        return Stream.of(firstName, lastName1, lastName2)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
    }
}
