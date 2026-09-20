package com.b4rrhh.payroll.document.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Donde van los documentos de los recibos.
 *
 * <p>Es un cubo aparte del de las fotos, y no por orden: las fotos se sirven publicas —el cubo de
 * empleados lleva {@code anonymous download}— y un recibo de nomina no se sirve a nadie que no
 * pase por el API autenticado. Compartir cubo seria compartir esa politica.
 */
@ConfigurationProperties(prefix = "payslip-document")
public record PayslipDocumentStorageProperties(String bucketName) {
}
