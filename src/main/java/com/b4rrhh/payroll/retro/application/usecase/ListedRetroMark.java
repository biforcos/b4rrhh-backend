package com.b4rrhh.payroll.retro.application.usecase;

import com.b4rrhh.payroll.retro.domain.model.RetroMark;

/**
 * Una marca tal como se ensena en la ficha ({@code backend#139}).
 *
 * @param withoutAReceiptToPayIt si esta activa y su presencia ya no tiene ningun recibo que la pueda
 *        pagar. Falso en una marca pagada o descartada: esas ya no esperan a nadie
 */
public record ListedRetroMark(RetroMark mark, boolean withoutAReceiptToPayIt) {
}
