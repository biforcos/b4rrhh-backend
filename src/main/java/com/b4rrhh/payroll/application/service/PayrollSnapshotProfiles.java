package com.b4rrhh.payroll.application.service;

import com.b4rrhh.payroll.domain.model.PayrollContextSnapshot;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Lo que las fotos del contexto dicen del recibo, leido una sola vez ({@code backend#112}).
 *
 * <p>Esto vivia entero dentro de {@code PayrollResponseAssembler}, y alli estaba bien mientras la
 * pantalla era la unica salida. El PDF es una segunda salida del <b>mismo</b> documento, y el
 * criterio 5 del issue pide que las dos digan lo mismo. Dos lectores del mismo JSON no lo
 * garantizan: lo garantiza que haya uno.
 *
 * <p>Las fotos son JSON guardado al calcular, no columnas: un campo que no este, o un JSON que no
 * se pueda leer, devuelve nulo y no rompe el recibo. Un recibo viejo al que le falta un dato
 * ensena un hueco, que es la verdad; inventarselo, no.
 */
@Component
public class PayrollSnapshotProfiles {

    private static final String COMPANY_DATA = "COMPANY_DATA";
    private static final String EMPLOYEE_DATA = "EMPLOYEE_DATA";
    private static final String AGREEMENT_DATA = "AGREEMENT_DATA";
    private static final String EMPLOYEE_PAYROLL_CONTEXT = "EMPLOYEE_PAYROLL_CONTEXT";
    private static final String WORK_CENTER_DATA = "WORK_CENTER_DATA";

    public record Company(
            String legalName,
            String taxIdentifier,
            String street,
            String city,
            String postalCode
    ) {}

    public record Employee(
            String fullName,
            String nif,
            String street,
            String city,
            String postalCode
    ) {}

    public record Agreement(
            String officialAgreementNumber,
            String displayName,
            String shortName,
            String annualHours,
            String agreementCategoryCode
    ) {}

    private final ObjectMapper objectMapper;

    public PayrollSnapshotProfiles(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Company company(List<PayrollContextSnapshot> snapshots) {
        Map<String, String> map = stringMap(snapshots, COMPANY_DATA);
        if (map == null) {
            return null;
        }
        return new Company(
                map.get("legalName"),
                map.get("taxIdentifier"),
                map.get("street"),
                map.get("city"),
                map.get("postalCode")
        );
    }

    public Employee employee(List<PayrollContextSnapshot> snapshots) {
        Map<String, String> map = stringMap(snapshots, EMPLOYEE_DATA);
        if (map == null) {
            return null;
        }
        return new Employee(
                map.get("fullName"),
                map.get("nif"),
                map.get("street"),
                map.get("city"),
                map.get("postalCode")
        );
    }

    public Agreement agreement(List<PayrollContextSnapshot> snapshots) {
        Map<String, String> map = stringMap(snapshots, AGREEMENT_DATA);
        if (map == null) {
            return null;
        }
        return new Agreement(
                map.get("officialAgreementNumber"),
                map.get("displayName"),
                map.get("shortName"),
                map.get("annualHours"),
                map.get("agreementCategoryCode")
        );
    }

    public String presenceStartDate(List<PayrollContextSnapshot> snapshots) {
        return field(snapshots, EMPLOYEE_PAYROLL_CONTEXT, "presenceStartDate");
    }

    public String presenceEndDate(List<PayrollContextSnapshot> snapshots) {
        return field(snapshots, EMPLOYEE_PAYROLL_CONTEXT, "presenceEndDate");
    }

    /**
     * La antiguedad que la foto guardo al calcular ({@code backend#91}). Nula en los recibos
     * anteriores al issue, y esa nulidad significa «no se sabe»: no se sustituye por
     * {@code presenceStartDate}, que es la fecha que esta al lado y da un numero distinto para
     * todo readmitido.
     */
    public String seniorityDate(List<PayrollContextSnapshot> snapshots) {
        return field(snapshots, EMPLOYEE_PAYROLL_CONTEXT, "seniorityDate");
    }

    public String workCenterCode(List<PayrollContextSnapshot> snapshots) {
        return field(snapshots, WORK_CENTER_DATA, "workCenterCode");
    }

    public String workCenterName(List<PayrollContextSnapshot> snapshots) {
        return field(snapshots, WORK_CENTER_DATA, "workCenterName");
    }

    private Map<String, String> stringMap(List<PayrollContextSnapshot> snapshots, String type) {
        String json = payloadOf(snapshots, type);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private String field(List<PayrollContextSnapshot> snapshots, String type, String field) {
        String json = payloadOf(snapshots, type);
        if (json == null) {
            return null;
        }
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
            Object value = map.get(field);
            return value == null ? null : value.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String payloadOf(List<PayrollContextSnapshot> snapshots, String type) {
        if (snapshots == null) {
            return null;
        }
        return snapshots.stream()
                .filter(s -> type.equals(s.getSnapshotTypeCode()))
                .findFirst()
                .map(PayrollContextSnapshot::getSnapshotPayloadJson)
                .orElse(null);
    }
}
