package cl.duoc.gestorcitas.usuarios.entity;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/** Roles del sistema. El valor coincide con el App Role de Entra ID. */
public enum RolUsuario {
    PACIENTE("Paciente"),
    MEDICO("Medico"),
    ADMIN("Admin");

    private final String appRole;

    RolUsuario(String appRole) {
        this.appRole = appRole;
    }

    public String getAppRole() {
        return appRole;
    }

    public static RolUsuario desdeAppRole(String appRole) {
        for (RolUsuario r : values()) {
            if (r.appRole.equalsIgnoreCase(appRole) || r.name().equalsIgnoreCase(appRole)) {
                return r;
            }
        }
        return null;
    }

    /** Convierte los roles del token ignorando valores desconocidos. */
    public static Set<RolUsuario> desdeAppRoles(Collection<String> appRoles) {
        Set<RolUsuario> set = EnumSet.noneOf(RolUsuario.class);
        if (appRoles != null) {
            appRoles.stream().map(RolUsuario::desdeAppRole).filter(r -> r != null).forEach(set::add);
        }
        return set;
    }
}
