package com.b4rrhh.shared.infrastructure.demo.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuracion del acceso a la demo publica.
 *
 * A diferencia de dev-auth, aqui hay contrasena. No es un secreto: se publica
 * en la propia pantalla de acceso, porque la demo es para que entre cualquiera.
 * Sirve para que nadie entre por accidente y para dejar claro que hay una
 * puerta, no para impedir el paso.
 */
@ConfigurationProperties(prefix = "app.demo-auth")
public class DemoAuthProperties {

    private boolean enabled = false;
    private String password = "";
    private int expiresInMinutes = 120;

    /**
     * Sujetos que la demo permite, con los roles que lleva su token.
     *
     * Los sujetos existen ya en authz.subject_role_assignment, asi que el
     * permiso real lo decide la base de datos. El claim 'roles' del token debe
     * coincidir para que Spring Security resuelva bien los ROLE_*.
     *
     * 'bifor' (ADMIN) NO esta aqui a proposito: nadie entra de administrador
     * en una aplicacion abierta a internet.
     */
    private Map<String, List<String>> subjects = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getExpiresInMinutes() {
        return expiresInMinutes;
    }

    public void setExpiresInMinutes(int expiresInMinutes) {
        this.expiresInMinutes = expiresInMinutes;
    }

    public Map<String, List<String>> getSubjects() {
        return subjects;
    }

    public void setSubjects(Map<String, List<String>> subjects) {
        this.subjects = subjects;
    }
}
