package Models;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad A (tabla: usuarios)
 *
 * Campos (BD):
 *  - id               INT AUTO_INCREMENT PRIMARY KEY
 *  - username         VARCHAR(30)  NOT NULL UNIQUE
 *  - email            VARCHAR(120) NOT NULL UNIQUE
 *  - activo           BOOLEAN NOT NULL
 *  - fecha_registro   DATETIME NULL
 *  - credencial_id    INT (FK a credenciales_acceso.id)  // mapeado aquí como objeto
 *  - eliminado        BOOLEAN DEFAULT FALSE
 *
 * Relación 1→1 unidireccional:
 *  - Usuario conoce a CredencialAcceso
 *  - CredencialAcceso NO conoce a Usuario
 *
 * Persistencia:
 *  - JDBC + DAO (sin JPA). El mapeo de credencial_id se hace en UsuarioDAO mediante LEFT JOIN.
 *  - La FK puede ser NULL (usuario sin credencial asociada).
 */
public class Usuario extends Base {

    /**
     * Username del usuario.
     * Reglas: requerido, no null/blank, único (menor o igual a 30).
     * La verificación la realiza Service y la constraint UNIQUE en BD.
     */
    private String username;

    /**
     * Email del usuario.
     * Reglas: requerido, no null/blank, único (menor o igual 120), formato válido.
     * Validado en Service y por UNIQUE en BD.
     */
    private String email;

    /**
     * Estado del usuario (BOOLEAN NOT NULL).
     * Reglas de coherencia a cargo de Service.
     */
    private boolean activo;

    /**
     * Fecha/hora de registro (puede ser NULL).
     * Sugerencia: si no se provee, DAO/Service pueden setear NOW() al insertar.
     */
    private LocalDateTime fechaRegistro;

    /**
     * Lado B de la relación 1→1 unidireccional.
     * Puede ser null (usuario sin credencial).
     * Se mapea desde usuarios.credencial_id mediante LEFT JOIN en el DAO.
     */
    private CredencialAcceso credencial;

    /**
     * Constructor completo para reconstruir un Usuario desde la BD.
     * Usado por UsuarioDAO al mapear ResultSet.
     * La credencial se asigna posteriormente con setCredencial().
     */
    public Usuario(int id, String username, String email, boolean activo, LocalDateTime fechaRegistro) {
        // Por defecto, al reconstruir desde SELECT se asume eliminado=false,
        // porque los listados deberían filtrar por eliminado=false en el DAO.
        super(id, false);
        this.username = username;
        this.email = email;
        this.activo = activo;
        this.fechaRegistro = fechaRegistro;
    }

    /**
     * Constructor por defecto para crear un Usuario nuevo sin ID. 
     */
    public Usuario() {
        super();
    }

    // --------------------
    // Getters / Setters
    // --------------------
    public String getUsername() {
        return username;
    }

    /**
     * Validaciones esperadas en Service:
     * - no null/blank
     * - longitud menor o igual a 30
     * - unicidad (consultando DAO)
     */
    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    /**
     * Validaciones esperadas en Service:
     * - no null/blank
     * - longitud menor o igual a 120
     * - formato de email
     * - unicidad (consultando DAO)
     */
    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActivo() {
        return activo;
    }

    /**
     * Establece el estado del usuario (activo/inactivo). Observación: al ser
     * boolean, no aplica "vacío"; solo coherencia de negocio en Service.
     */
    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    /**
     * Puede ser null. Si es null al insertar, DAO/Service podrían setear NOW().
     */
    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public CredencialAcceso getCredencial() {
        return credencial;
    }

    /**
     * Asocia o desasocia una credencial al usuario.
     * - Si es null -> la FK credencial_id será NULL en la BD.
     * - Si no es null -> el DAO persistirá usuarios.credencial_id = credencial.getId().
     */
    public void setCredencial(CredencialAcceso credencial) {
        this.credencial = credencial;
    }

    // --------------
    // toString / equals / hashCode
    // --------------
    @Override
    public String toString() {
        // Evitamos imprimir datos sensibles de la credencial; mostramos solo su id.
        String credStr = (credencial == null) ? "null" : ("CredencialAcceso{id=" + credencial.getId() + "}");
        return "Usuario{"
                + "id=" + getId()
                + ", eliminado=" + isEliminado()
                + ", username='" + username + '\''
                + ", email='" + email + '\''
                + ", activo=" + activo
                + ", fechaRegistro=" + fechaRegistro
                + ", credencial=" + credStr
                + '}';
    }
    
    /**
     * Determina si dos instancias de Usuario son iguales.
     *
     * Criterio:
     * - Si ambos objetos tienen ID asignado (id != 0), se comparan por ID.
     * - Si alguno no tiene ID (id == 0), se comparan por email (atributo único).
     *
     * Permite comparar correctamente tanto objetos persistidos como transitorios.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)return true;
        if (!(o instanceof Usuario)) return false;
        Usuario that = (Usuario) o;

        // Si ambos tienen id asignado (distinto de 0), comparar por id
        if (getId() != 0 && that.getId() != 0) {
            return getId() == that.getId();
        }

        // Si no hay id (objetos transitorios), se compara por email
        return Objects.equals(email, that.email);
    }

    /**
     * Código hash consistente con equals():
     * - Si id != 0 -> hash por id
     * - Si id == 0 -> hash por email
     */
    @Override
    public int hashCode() {
        return (getId() != 0) ? Integer.hashCode(getId()) : Objects.hash(email);
    }
}
