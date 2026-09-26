package com.b4rrhh.payroll.retro.domain.exception;

import com.b4rrhh.payroll.retro.domain.model.RetroMarkStatus;

/**
 * Se intento descartar una marca que ya no esta activa ({@code backend#130}).
 *
 * <p>Descartar una consumida seria reescribir la historia de un recibo entregado; descartar una ya
 * descartada dejaria dos motivos y ningun modo de saber cual valia.
 */
public class RetroMarkNotActiveException extends RuntimeException {
    public RetroMarkNotActiveException(Long id, RetroMarkStatus status) {
        super("La marca de retroactividad " + id + " esta en " + status + " y solo se descarta una activa");
    }
}
