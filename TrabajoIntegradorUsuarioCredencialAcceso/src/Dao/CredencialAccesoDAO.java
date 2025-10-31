package Dao;

import Config.DataBaseConnection;
import Models.CredencialAcceso;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad CredencialAcceso.
 * Gestiona todas las operaciones de persistencia de credenciales en la base de datos.
 *
 * Características:
 * - Implementa GenericDAO<CredencialAcceso> para operaciones CRUD estándar
 * - Usa PreparedStatements en TODAS las consultas (protección contra SQL injection)
 * - Implementa soft delete (eliminado=TRUE, no DELETE físico)
 * - NO maneja relaciones inversas (la relación 1→1 se resuelve desde UsuarioDAO con LEFT JOIN)
 * - Soporta transacciones mediante insertTx() (recibe Connection externa)
 *
 * Patrón: DAO con try-with-resources para manejo automático de recursos JDBC
 */
public class CredencialAccesoDAO implements GenericDAO<CredencialAcceso> {

    // =======================
    // SQL BASE
    // =======================

    /**
     * Inserta una credencial.
     * Campos: hash_password, salt, ultimo_cambio, requiere_reset.
     * El id es AUTO_INCREMENT y se obtiene con RETURN_GENERATED_KEYS.
     * El campo eliminado tiene DEFAULT FALSE en la BD.
     */
    private static final String INSERT_SQL = """
        INSERT INTO credenciales_acceso (hash_password, salt, ultimo_cambio, requiere_reset)
        VALUES (?, ?, ?, ?)
    """;

    /**
     * Actualiza una credencial por id.
     * NO actualiza el flag eliminado (solo se modifica en soft delete).
     */
    private static final String UPDATE_SQL = """
        UPDATE credenciales_acceso
        SET hash_password = ?, salt = ?, ultimo_cambio = ?, requiere_reset = ?
        WHERE id = ?
    """;

    /**
     * Soft delete: marca eliminado=TRUE sin borrar físicamente la fila.
     */
    private static final String DELETE_SQL = "UPDATE credenciales_acceso SET eliminado = TRUE WHERE id = ?";

    /**
     * Obtiene credencial por ID (solo activas, eliminado=FALSE).
     */
    private static final String SELECT_BY_ID_SQL = """
        SELECT id, hash_password, salt, ultimo_cambio, requiere_reset
        FROM credenciales_acceso
        WHERE id = ? AND eliminado = FALSE
    """;

    /**
     * Obtiene todas las credenciales activas (eliminado=FALSE).
     */
    private static final String SELECT_ALL_SQL = """
        SELECT id, hash_password, salt, ultimo_cambio, requiere_reset
        FROM credenciales_acceso
        WHERE eliminado = FALSE
        ORDER BY id
    """;

    // =======================
    // CRUD (GenericDAO)
    // =======================

    /**
     * Inserta una credencial (versión sin transacción).
     * Asigna el ID autogenerado a la entidad usando RETURN_GENERATED_KEYS.
     *
     * Validaciones de dominio (en Service):
     * - hashPassword: requerido, no null/blank, longitud <= 255
     * - salt: opcional, longitud <= 64
     * - ultimoCambio: opcional
     * - requiereReset: requerido
     * @param cred
     */
    @Override
    public void insertar(CredencialAcceso cred) throws Exception {
        try (Connection conn = DataBaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            setCredencialParameters(stmt, cred);
            stmt.executeUpdate();
            setGeneratedId(stmt, cred);
        }
    }

    /**
     * Inserta una credencial dentro de una transacción existente.
     * NO cierra la conexión (responsabilidad del caller).
     * @param cred
     */
    @Override
    public void insertTx(CredencialAcceso cred, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            setCredencialParameters(stmt, cred);
            stmt.executeUpdate();
            setGeneratedId(stmt, cred);
        }
    }

    /**
     * Actualiza hash_password, salt, ultimo_cambio y requiere_reset.
     * Lanza excepción si no se afecta ninguna fila.
     * @param cred
     */
    @Override
    public void actualizar(CredencialAcceso cred) throws Exception {
        try (Connection conn = DataBaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            stmt.setString(1, cred.getHashPassword());
            stmt.setString(2, cred.getSalt());
            setUltimoCambio(stmt, 3, cred.getUltimoCambio());
            stmt.setBoolean(4, cred.isRequiereReset());
            stmt.setInt(5, cred.getId());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("No se pudo actualizar la credencial con ID: " + cred.getId());
            }
        }
    }

    /**
     * Soft delete: marca eliminado=TRUE.
     * Lanza excepción si no se afecta ninguna fila.
     */
    @Override
    public void eliminar(int id) throws Exception {
        try (Connection conn = DataBaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("No se encontró credencial con ID: " + id);
            }
        }
    }

    /**
     * Obtiene una credencial por ID (solo activas).
     */
    @Override
    public CredencialAcceso getById(int id) throws Exception {
        try (Connection conn = DataBaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCredencial(rs);
                }
            }
        } catch (SQLException e) {
            throw new Exception("Error al obtener credencial por ID: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Obtiene todas las credenciales activas (eliminado=FALSE).
     */
    @Override
    public List<CredencialAcceso> getAll() throws Exception {
        List<CredencialAcceso> list = new ArrayList<>();

        try (Connection conn = DataBaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_SQL)) {

            while (rs.next()) {
                list.add(mapResultSetToCredencial(rs));
            }
        } catch (SQLException e) {
            throw new Exception("Error al obtener todas las credenciales: " + e.getMessage(), e);
        }
        return list;
    }

    // =======================
    // HELPERS DE SETEO / ID
    // =======================

    /**
     * Setea parámetros para INSERT/UPDATE de credencial.
     * 1: hash_password (String, requerido)
     * 2: salt          (String, opcional)
     * 3: ultimo_cambio (Timestamp, opcional)
     * 4: requiere_reset (boolean, requerido)
     */
    private void setCredencialParameters(PreparedStatement stmt, CredencialAcceso cred) throws SQLException {
        stmt.setString(1, cred.getHashPassword());
        stmt.setString(2, cred.getSalt());
        setUltimoCambio(stmt, 3, cred.getUltimoCambio());
        stmt.setBoolean(4, cred.isRequiereReset());
    }

    /** Setea ultimo_cambio como Timestamp o NULL. */
    private void setUltimoCambio(PreparedStatement stmt, int index, LocalDateTime fecha) throws SQLException {
        if (fecha != null) {
            stmt.setTimestamp(index, Timestamp.valueOf(fecha));
        } else {
            stmt.setNull(index, Types.TIMESTAMP);
        }
    }

    /** Asigna el ID autogenerado a la entidad CredencialAcceso. */
    private void setGeneratedId(PreparedStatement stmt, CredencialAcceso cred) throws SQLException {
        try (ResultSet keys = stmt.getGeneratedKeys()) {
            if (keys.next()) {
                cred.setId(keys.getInt(1));
            } else {
                throw new SQLException("La inserción de credencial falló: no se obtuvo ID generado");
            }
        }
    }

    // =======================
    // MAPEOS
    // =======================

    /**
     * Mapea una fila de ResultSet a CredencialAcceso.
     * Columnas esperadas:
     *  - id, hash_password, salt, ultimo_cambio, requiere_reset
     */
    private CredencialAcceso mapResultSetToCredencial(ResultSet rs) throws SQLException {
        CredencialAcceso c = new CredencialAcceso();
        c.setId(rs.getInt("id"));
        c.setHashPassword(rs.getString("hash_password"));
        c.setSalt(rs.getString("salt"));

        Timestamp ts = rs.getTimestamp("ultimo_cambio");
        c.setUltimoCambio(ts == null ? null : ts.toLocalDateTime());

        c.setRequiereReset(rs.getBoolean("requiere_reset"));
        return c;
    }
}
