package com.b4rrhh.payroll.retro.infrastructure.web.dto;

/** @param discardReason por que no se paga. Obligatorio, y el servidor no se lo inventa. */
public record DiscardRetroMarkRequest(String discardReason) {
}
