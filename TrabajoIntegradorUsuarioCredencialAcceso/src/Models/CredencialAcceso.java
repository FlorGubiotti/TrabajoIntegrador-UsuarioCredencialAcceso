package Models;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad que representa las credenciales de acceso de un usuario.
 * Hereda de Base para obtener id y eliminado.
 *
 * Entidad B (tabla: credenciales_acceso):
 *   id               INT AUTO_INCREMENT PK
 *   hash_password    VARCHAR(255) NOT NULL
 *   salt             VARCHAR(64) NULL
 *   ultimo_cambio    DATETIME NULL
 *   requiere_reset   BOOLEAN NOT NULL
 *   eliminado        BOOLEAN DEFAULT FALSE
 *
 * Relación con Usuario:
 *  - Un Usuario puede tener 0 o 1 CredencialAcceso (1→1 unidireccional desde Usuario)
 *  - CredencialAcceso NO conoce a Usuario (no hay referencia inversa)
 */

public class CredencialAcceso extends Base{
    
/**
     * Hash de la contraseña (NO la contraseña en texto).
     * Requerido, no puede ser null ni vacío. Longitud máxima: 255.
     * La validación se realiza en Service.
     */
    private String hashPassword;

    /**
     * Salt utilizado para el hash. Opcional. Longitud máxima: 64.
     */
    private String salt;

    /**
     * Fecha/hora del último cambio de contraseña. Puede ser null.
     */
    private LocalDateTime ultimoCambio;

    /**
     * Indica si el usuario debe resetear su contraseña en el próximo inicio de sesión.
     * Requerido (BOOLEAN NOT NULL).
     */
    private boolean requiereReset;

    /**
     * Constructor completo para reconstruir una credencial desde la base de datos.
     * Usado por los DAOs al mapear ResultSet.
     *
     * @param id            ID en la BD
     * @param hashPassword  hash de la contraseña (requerido)
     * @param salt          salt (opcional)
     * @param ultimoCambio  fecha del último cambio (opcional)
     * @param requiereReset flag de requerimiento de reset (requerido)
     */
    public CredencialAcceso(int id, String hashPassword, String salt, LocalDateTime ultimoCambio, boolean requiereReset) {
        super(id, false); // eliminado=false en lecturas regulares (los listados filtran en DAO)
        this.hashPassword = hashPassword;
        this.salt = salt;
        this.ultimoCambio = ultimoCambio;
        this.requiereReset = requiereReset;
    }

    /**
     * Constructor por defecto para crear una credencial nueva (sin ID).
     * El ID será asignado por la BD (AUTO_INCREMENT) al insertar.
     */
    public CredencialAcceso() {
        super();
    }

    // --------------------
    // Getters / Setters
    // --------------------

    public String getHashPassword() {
        return hashPassword;
    }

    /**
     * Establece el hash de contraseña.
     * Validaciones esperadas en Service:
     *  - no null/blank
     *  - longitud <= 255
     */
    public void setHashPassword(String hashPassword) {
        this.hashPassword = hashPassword;
    }

    public String getSalt() {
        return salt;
    }

    /**
     * Establece el salt. Puede ser null.
     * Validación esperada en Service: longitud <= 64.
     */
    public void setSalt(String salt) {
        this.salt = salt;
    }

    public LocalDateTime getUltimoCambio() {
        return ultimoCambio;
    }

    /**
     * Establece la fecha del último cambio. Puede ser null.
     * Sugerencia: setear NOW() en Service al modificar hash.
     */
    public void setUltimoCambio(LocalDateTime ultimoCambio) {
        this.ultimoCambio = ultimoCambio;
    }

    public boolean isRequiereReset() {
        return requiereReset;
    }

    /**
     * Establece el flag de requerimiento de reset (requerido).
     */
    public void setRequiereReset(boolean requiereReset) {
        this.requiereReset = requiereReset;
    }

    // --------------
    // toString / equals / hashCode
    // --------------

    /**
     * Representación en texto de la credencial.
     * Por seguridad, se evita imprimir el hash completo.
     */
    @Override
    public String toString() {
        String hashPreview = (hashPassword == null)
                ? "null"
                : (hashPassword.length() <= 8 ? hashPassword : (hashPassword.substring(0, 8) + "…"));
        return "CredencialAcceso{" +
                "id=" + getId() +
                ", eliminado=" + isEliminado() +
                ", hashPassword=" + hashPreview +
                ", salt=" + (salt == null ? "null" : salt) +
                ", ultimoCambio=" + ultimoCambio +
                ", requiereReset=" + requiereReset +
                '}';
    }

    /**
     * Compara dos credenciales por igualdad SEMÁNTICA.
     * Criterio: mismo hash y mismo salt.
     * Nota: no se compara por ID para permitir detectar duplicados lógicos.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CredencialAcceso that = (CredencialAcceso) o;
        return Objects.equals(hashPassword, that.hashPassword)
                && Objects.equals(salt, that.salt);
    }

    /**
     * Hash code consistente con equals(): basado en hashPassword y salt.
     */
    @Override
    public int hashCode() {
        return Objects.hash(hashPassword, salt);
    }
}
