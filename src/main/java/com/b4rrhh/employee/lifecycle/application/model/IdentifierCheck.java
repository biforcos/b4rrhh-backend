package com.b4rrhh.employee.lifecycle.application.model;

/**
 * Lo que se sabe de un documento antes del alta (backend#149, backend#171): si está bien escrito y,
 * si lo está, de quién es ya. Un documento mal escrito no tiene dueño: no hay a quién comparárselo.
 *
 * @param reason por qué no vale, en palabras («la letra debería ser Z»); null si vale
 * @param owner  quién lo tiene ya; null si está libre o si no vale
 */
public record IdentifierCheck(boolean valid, String reason, IdentifierOwner owner) {

    public static IdentifierCheck invalid(String reason) {
        return new IdentifierCheck(false, reason, null);
    }

    public static IdentifierCheck valid(IdentifierOwner owner) {
        return new IdentifierCheck(true, null, owner);
    }
}
