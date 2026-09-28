package com.b4rrhh.payroll.domain.port;

import com.b4rrhh.payroll.domain.model.Payroll;

import java.util.List;

/**
 * Una página de la búsqueda de recibos, con el total de todos los que cumplen los filtros
 * ({@code b4rrhh/frontend#93}).
 *
 * <p>El total es lo que distingue «no hay más» de «no caben más en esta página». Antes la búsqueda
 * cortaba en 500 sin decirlo, y con la semilla de nueve meses la pantalla afirmaba «500 nóminas
 * encontradas» de 7.908.
 */
public record PayrollSearchPage(List<Payroll> items, int page, int size, long total) {

    public PayrollSearchPage {
        items = List.copyOf(items);
    }
}
