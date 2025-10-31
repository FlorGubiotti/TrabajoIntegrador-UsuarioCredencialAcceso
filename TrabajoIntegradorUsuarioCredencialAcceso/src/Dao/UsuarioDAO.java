package Dao;

import Config.DataBaseConnection;
import Models.CredencialAcceso;
import Models.Usuario;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad Usuario.
 * Gestiona todas las operaciones de persistencia de usuarios en la base de datos.
 *
 * Características:
 * - Implementa GenericDAO<Usuario> para operaciones CRUD estándar
 * - Usa PreparedStatements en TODAS las consultas (protección contra SQL injection)
 * - Maneja LEFT JOIN con credenciales_acceso para cargar la relación de forma eager
 * - Implementa soft delete (eliminado=TRUE, no DELETE físico)
 * - Proporciona búsquedas especializadas (por username exacto, por email exacto)
 * - Soporta transacciones mediante insertTx() (recibe Connection externa)
 *
 * Patrón: DAO con try-with-resources para manejo automático de recursos JDBC
 */
public class UsuarioDAO implements GenericDAO<Usuario> {

    // =======================
    // SQL BASE
    // =======================

    /** Inserta username, email, activo, fecha_registro y FK credencial_id (nullable). */
    private static final String INSERT_SQL = """
        INSERT INTO usuarios (username, email, activo, fecha_registro, credencial_id)
        VALUES (?, ?, ?, ?, ?)
    """;

    /** Actualiza username, email, activo, fecha_registro y FK credencial_id. No toca 'eliminado'. */
    private static final String UPDATE_SQL = """
        UPDATE usuarios
        SET username = ?, email = ?, activo = ?, fecha_registro = ?, credencial_id = ?
        WHERE id = ?
    """;

    /** Soft delete: marca eliminado = TRUE (no borra físicamente). */
    private static final String DELETE_SQL = "UPDATE usuarios SET eliminado = TRUE WHERE id = ?";

    /**
     * SELECT por ID con LEFT JOIN a credenciales_acceso.
     * Solo retorna usuarios activos (eliminado=FALSE).
     */
    private static final String SELECT_BY_ID_SQL = """
        SELECT
            u.id, u.username, u.email, u.activo, u.fecha_registro, u.credencial_id, u.eliminado,
            c.id AS cred_id, c.hash_password, c.salt, c.ultimo_cambio, c.requiere_reset, c.eliminado AS cred_eliminado
        FROM usuarios u
        LEFT JOIN credenciales_acceso c ON u.credencial_id = c.id
        WHERE u.id = ? AND u.eliminado = FALSE
    """;

    /** SELECT all (activos), con LEFT JOIN a credenciales. */
    private static final String SELECT_ALL_SQL = """
        SELECT
            u.id, u.username, u.email, u.activo, u.fecha_registro, u.credencial_id, u.eliminado,
            c.id AS cred_id, c.hash_password, c.salt, c.ultimo_cambio, c.requiere_reset, c.eliminado AS cred_eliminado
        FROM usuarios u
        LEFT JOIN credenciales_acceso c ON u.credencial_id = c.id
        WHERE u.eliminado = FALSE
        ORDER BY u.id
    """;

    /** Búsqueda exacta por username (único). Solo activos. */
    private static final String SEARCH_BY_USERNAME_SQL = """
        SELECT
            u.id, u.username, u.email, u.activo, u.fecha_registro, u.credencial_id, u.eliminado,
            c.id AS cred_id, c.hash_password, c.salt, c.ultimo_cambio, c.requiere_reset, c.eliminado AS cred_eliminado
        FROM usuarios u
        LEFT JOIN credenciales_acceso c ON u.credencial_id = c.id
        WHERE u.eliminado = FALSE AND u.username = ?
    """;

    /** Búsqueda exacta por email (único). Solo activos. */
    private static final String SEARCH_BY_EMAIL_SQL = """
        SELECT
            u.id, u.username, u.email, u.activo, u.fecha_registro, u.credencial_id, u.eliminado,
            c.id AS cred_id, c.hash_password, c.salt, c.ultimo_cambio, c.requiere_reset, c.eliminado AS cred_eliminado
        FROM usuarios u
        LEFT JOIN credenciales_acceso c ON u.credencial_id = c.id
        WHERE u.eliminado = FALSE AND u.email = ?
    """;

    // =======================
    // CRUD (GenericDAO)
    // =======================

    /**
     * Inserta un usuario (versión sin transacción).
     * Asigna el ID autogenerado a la entidad usando RETURN_GENERATED_KEYS.
     */
    @Override
    public void insertar(Usuario usuario) throws Exception {
        try (Connection conn = DataBaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            setUsuarioParameters(stmt, usuario);
            stmt.executeUpdate();
            setGeneratedId(stmt, usuario);
        }
    }

    /**
     * Inserta un usuario dentro de una transacción existente.
     * NO cierra la conexión (responsabilidad del caller).
     */
    @Override
    public void insertTx(Usuario usuario, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            setUsuarioParameters(stmt, usuario);
            stmt.executeUpdate();
            setGeneratedId(stmt, usuario);
        }
    }

    /**
     * Actualiza username, email, activo, fecha_registro y credencial_id.
     * Lanza excepción si no se afecta ninguna fila.
     */
    @Override
    public void actualizar(Usuario usuario) throws Exception {
        try (Connection conn = DataBaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            stmt.setString(1, usuario.getUsername());
            stmt.setString(2, usuario.getEmail());
            stmt.setBoolean(3, usuario.isActivo());
            setFechaRegistro(stmt, 4, usuario.getFechaRegistro());
            setCredencialId(stmt, 5, usuario.getCredencial());
            stmt.setInt(6, usuario.getId());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("No se pudo actualizar el usuario con ID: " + usuario.getId());
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
                throw new SQLException("No se encontró usuario con ID: " + id);
            }
        }
    }

    /**
     * Obtiene un usuario por ID (solo activos).
     * Incluye su credencial mediante LEFT JOIN (puede ser null).
     */
    @Override
    public Usuario getById(int id) throws Exception {
        try (Connection conn = DataBaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUsuario(rs);
                }
            }
        } catch (SQLException e) {
            throw new Exception("Error al obtener usuario por ID: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Obtiene todos los usuarios activos (eliminado=FALSE).
     * Incluye credenciales mediante LEFT JOIN.
     */
    @Override
    public List<Usuario> getAll() throws Exception {
        List<Usuario> usuarios = new ArrayList<>();

        try (Connection conn = DataBaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_SQL)) {

            while (rs.next()) {
                usuarios.add(mapResultSetToUsuario(rs));
            }
        } catch (SQLException e) {
            throw new Exception("Error al obtener todos los usuarios: " + e.getMessage(), e);
        }
        return usuarios;
    }

    // =======================
    // BÚSQUEDAS ESPECÍFICAS
    // =======================

    /**
     * Busca un usuario por username exacto (único en el sistema).
     * Retorna null si no existe o está eliminado.
     */
    public Usuario buscarPorUsername(String username) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("El username no puede estar vacío");
        }
        try (Connection conn = DataBaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SEARCH_BY_USERNAME_SQL)) {

            stmt.setString(1, username.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUsuario(rs);
                }
            }
        }
        return null;
    }

    /**
     * Busca un usuario por email exacto (único en el sistema).
     * Retorna null si no existe o está eliminado.
     */
    public Usuario buscarPorEmail(String email) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El email no puede estar vacío");
        }
        try (Connection conn = DataBaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SEARCH_BY_EMAIL_SQL)) {

            stmt.setString(1, email.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUsuario(rs);
                }
            }
        }
        return null;
    }

    // =======================
    // HELPERS DE SETEO/DICT
    // =======================

    /**
     * Setea parámetros para INSERT de usuario.
     * 1: username (String)
     * 2: email    (String)
     * 3: activo   (boolean)
     * 4: fecha_registro (Timestamp o NULL)
     * 5: credencial_id  (Integer o NULL)
     */
    private void setUsuarioParameters(PreparedStatement stmt, Usuario usuario) throws SQLException {
        stmt.setString(1, usuario.getUsername());
        stmt.setString(2, usuario.getEmail());
        stmt.setBoolean(3, usuario.isActivo());
        setFechaRegistro(stmt, 4, usuario.getFechaRegistro());
        setCredencialId(stmt, 5, usuario.getCredencial());
    }

    /** Setea fecha_registro como Timestamp o NULL. */
    private void setFechaRegistro(PreparedStatement stmt, int index, LocalDateTime fechaRegistro) throws SQLException {
        if (fechaRegistro != null) {
            stmt.setTimestamp(index, Timestamp.valueOf(fechaRegistro));
        } else {
            stmt.setNull(index, Types.TIMESTAMP);
        }
    }

    /** Setea credencial_id (nullable). */
    private void setCredencialId(PreparedStatement stmt, int index, CredencialAcceso credencial) throws SQLException {
        if (credencial != null && credencial.getId() > 0) {
            stmt.setInt(index, credencial.getId());
        } else {
            stmt.setNull(index, Types.INTEGER);
        }
    }

    /** Asigna el ID autogenerado a la entidad Usuario. */
    private void setGeneratedId(PreparedStatement stmt, Usuario usuario) throws SQLException {
        try (ResultSet keys = stmt.getGeneratedKeys()) {
            if (keys.next()) {
                usuario.setId(keys.getInt(1));
            } else {
                throw new SQLException("La inserción de usuario falló: no se obtuvo ID generado");
            }
        }
    }

    // =======================
    // MAPEOS
    // =======================

    /**
     * Mapea una fila de ResultSet a Usuario (y su CredencialAcceso por LEFT JOIN).
     */
    private Usuario mapResultSetToUsuario(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setActivo(rs.getBoolean("activo"));

        Timestamp ts = rs.getTimestamp("fecha_registro");
        u.setFechaRegistro(ts == null ? null : ts.toLocalDateTime());

        // Manejo correcto de LEFT JOIN: credencial puede ser NULL
        int credId = rs.getInt("cred_id");
        if (credId > 0 && !rs.wasNull()) {
            CredencialAcceso c = new CredencialAcceso();
            c.setId(credId);
            c.setHashPassword(rs.getString("hash_password"));
            c.setSalt(rs.getString("salt"));

            Timestamp tc = rs.getTimestamp("ultimo_cambio");
            c.setUltimoCambio(tc == null ? null : tc.toLocalDateTime());

            c.setRequiereReset(rs.getBoolean("requiere_reset"));
            u.setCredencial(c);
        }

        return u;
    }
}