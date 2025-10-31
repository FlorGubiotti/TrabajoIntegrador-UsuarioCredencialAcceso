package Service;


import Dao.UsuarioDAO;
import Models.CredencialAcceso;
import Models.Usuario;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Servicio de negocio para la entidad Usuario.
 * Aplica reglas de validación, gestiona la lógica de unicidad y coordina
 * operaciones con CredencialAcceso cuando corresponde.
 *
 * Responsabilidades:
 * - Validar los datos de los usuarios antes de persistir.
 * - Garantizar la unicidad de username y email.
 * - Coordinar inserciones y actualizaciones entre Usuario y CredencialAcceso.
 * - Implementar baja lógica (soft delete).
 */
public class UsuarioServiceImpl implements GenericService<Usuario> {

    private final UsuarioDAO usuarioDAO;
    private final CredencialAccesoServiceImpl credencialService;

    private static final int USERNAME_MAX = 30;
    private static final int EMAIL_MAX = 120;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public UsuarioServiceImpl(UsuarioDAO usuarioDAO, CredencialAccesoServiceImpl credencialService) {
        if (usuarioDAO == null) {
            throw new IllegalArgumentException("UsuarioDAO no puede ser null");
        }
        if (credencialService == null) {
            throw new IllegalArgumentException("CredencialAccesoServiceImpl no puede ser null");
        }
        this.usuarioDAO = usuarioDAO;
        this.credencialService = credencialService;
    }

    // ============================================================
    // CRUD (GenericService)
    // ============================================================

    /**
     * Inserta un nuevo usuario.
     * Valida la información, garantiza la unicidad de username y email,
     * y coordina la inserción de la credencial si está asociada.
     * @param usuario
     */
    @Override
    public void insertar(Usuario usuario) throws Exception {
        validateUsuario(usuario);
        validateUsernameUnique(usuario.getUsername(), null);
        validateEmailUnique(usuario.getEmail(), null);

        if (usuario.getCredencial() != null) {
            CredencialAcceso cred = usuario.getCredencial();
            if (cred.getId() == 0) {
                credencialService.insertar(cred);
            } else {
                credencialService.actualizar(cred);
            }
        }

        usuarioDAO.insertar(usuario);
    }

    /**
     * Actualiza un usuario existente.
     * Verifica que los datos sean válidos, mantiene la unicidad de los campos
     * y actualiza o inserta la credencial asociada si corresponde.
     * @param usuario
     */
    @Override
    public void actualizar(Usuario usuario) throws Exception {
        if (usuario == null || usuario.getId() <= 0) {
            throw new IllegalArgumentException("El ID del usuario debe ser mayor a 0 para actualizar");
        }

        validateUsuario(usuario);
        validateUsernameUnique(usuario.getUsername(), usuario.getId());
        validateEmailUnique(usuario.getEmail(), usuario.getId());

        if (usuario.getCredencial() != null) {
            CredencialAcceso cred = usuario.getCredencial();
            if (cred.getId() == 0) {
                credencialService.insertar(cred);
            } else {
                credencialService.actualizar(cred);
            }
        }

        usuarioDAO.actualizar(usuario);
    }

    /**
     * Realiza la baja lógica del usuario.
     * No elimina la credencial asociada.
     */
    @Override
    public void eliminar(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        usuarioDAO.eliminar(id);
    }

    /**
     * Obtiene un usuario por su ID.
     * Retorna null si no existe o está eliminado.
     */
    @Override
    public Usuario getById(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        return usuarioDAO.getById(id);
    }

    /**
     * Obtiene la lista de todos los usuarios activos.
     */
    @Override
    public List<Usuario> getAll() throws Exception {
        return usuarioDAO.getAll();
    }

    // ============================================================
    // VALIDACIONES Y BÚSQUEDAS
    // ============================================================

    private void validateUsuario(Usuario u) {
        if (u == null) throw new IllegalArgumentException("El usuario no puede ser null");

        // Username
        if (u.getUsername() == null || u.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("El username no puede estar vacío");
        }
        if (u.getUsername().length() > USERNAME_MAX) {
            throw new IllegalArgumentException("El username excede la longitud máxima permitida (" + USERNAME_MAX + ")");
        }

        // Email
        if (u.getEmail() == null || u.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("El email no puede estar vacío");
        }
        if (u.getEmail().length() > EMAIL_MAX) {
            throw new IllegalArgumentException("El email excede la longitud máxima permitida (" + EMAIL_MAX + ")");
        }
        if (!EMAIL_PATTERN.matcher(u.getEmail()).matches()) {
            throw new IllegalArgumentException("El formato del email no es válido");
        }
    }

    private void validateUsernameUnique(String username, Integer usuarioId) throws Exception {
        Usuario existente = usuarioDAO.buscarPorUsername(username);
        if (existente != null && (usuarioId == null || existente.getId() != usuarioId)) {
            throw new IllegalArgumentException("Ya existe un usuario con el username: " + username);
        }
    }

    private void validateEmailUnique(String email, Integer usuarioId) throws Exception {
        Usuario existente = usuarioDAO.buscarPorEmail(email);
        if (existente != null && (usuarioId == null || existente.getId() != usuarioId)) {
            throw new IllegalArgumentException("Ya existe un usuario con el email: " + email);
        }
    }

    // ============================================================
    // CONSULTAS ADICIONALES
    // ============================================================

    public Usuario buscarPorUsername(String username) throws Exception {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("El username no puede estar vacío");
        }
        return usuarioDAO.buscarPorUsername(username);
    }

    public Usuario buscarPorEmail(String email) throws Exception {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El email no puede estar vacío");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("El formato del email no es válido");
        }
        return usuarioDAO.buscarPorEmail(email);
    }

    public CredencialAccesoServiceImpl getCredencialService() {
        return credencialService;
    }
    
    /**
     * Elimina una credencial asociada a un usuario de forma segura.
     *
     * Flujo:
     * 1) Verifica parámetros válidos.
     * 2) Obtiene el usuario y valida que exista.
     * 3) Verifica que el usuario tenga credencial y que coincida con el ID indicado.
     * 4) Desasocia la credencial del usuario (FK NULL).
     * 5) Actualiza el usuario en BD.
     * 6) Elimina (baja lógica) la credencial.
     *
     * @param usuarioId    ID del usuario dueño de la credencial
     * @param credencialId ID de la credencial a eliminar
     * @throws Exception si los IDs no son válidos, el usuario no existe,
     *                   la credencial no pertenece al usuario, o falla BD
     */
    public void eliminarCredencialDeUsuario(int usuarioId, int credencialId) throws Exception {
        if (usuarioId <= 0 || credencialId <= 0) {
            throw new IllegalArgumentException("Los IDs deben ser mayores a 0");
        }

        Usuario usuario = usuarioDAO.getById(usuarioId);
        if (usuario == null) {
            throw new IllegalArgumentException("No existe un usuario con ID: " + usuarioId);
        }

        if (usuario.getCredencial() == null || usuario.getCredencial().getId() != credencialId) {
            throw new IllegalArgumentException("La credencial no pertenece a este usuario");
        }

        usuario.setCredencial(null);
        usuarioDAO.actualizar(usuario);

        credencialService.eliminar(credencialId);
    }

}
