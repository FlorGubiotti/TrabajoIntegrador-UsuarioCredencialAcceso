package Service;

import Dao.GenericDAO;
import Models.CredencialAcceso;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de negocio para la entidad CredencialAcceso.
 * Aplica reglas de validación y delega la persistencia al DAO.
 *
 * Responsabilidades:
 * - Validar que los datos de la credencial sean correctos antes de persistir.
 * - Coordinar inserciones/actualizaciones asegurando consistencia de datos.
 * - Implementar baja lógica (soft delete).
 */
public class CredencialAccesoServiceImpl implements GenericService<CredencialAcceso> {

    /**
     * DAO genérico para credenciales.
     * Se inyecta para facilitar pruebas (mocks/stubs) y desacoplar de la implementación concreta.
     */
    private final GenericDAO<CredencialAcceso> credencialDAO;

    // Límites de longitud (acordes al esquema de BD sugerido)
    private static final int HASH_MAX = 255;
    private static final int SALT_MAX = 64;

    public CredencialAccesoServiceImpl(GenericDAO<CredencialAcceso> credencialDAO) {
        if (credencialDAO == null) {
            throw new IllegalArgumentException("CredencialDAO no puede ser null");
        }
        this.credencialDAO = credencialDAO;
    }

    /**
     * Inserta una nueva credencial.
     * Flujo:
     * 1) Validar campos (hash requerido, salt opcional, longitudes).
     * 2) Insertar con DAO (el DAO asigna el ID generado al objeto).
     * @param credencial
     */
    @Override
    public void insertar(CredencialAcceso credencial) throws Exception {
        validateCredencial(credencial);
        credencialDAO.insertar(credencial);
    }

    /**
     * Actualiza una credencial existente.
     * Reglas:
     * - El ID debe ser > 0.
     * - Validar campos antes de persistir.
     * @param credencial
     */
    @Override
    public void actualizar(CredencialAcceso credencial) throws Exception {
        validateCredencial(credencial);
        if (credencial.getId() <= 0) {
            throw new IllegalArgumentException("El ID de la credencial debe ser mayor a 0 para actualizar");
        }
        credencialDAO.actualizar(credencial);
    }

    /**
     * Baja lógica de la credencial (eliminado = true).
     */
    @Override
    public void eliminar(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        credencialDAO.eliminar(id);
    }

    /**
     * Obtiene una credencial por su ID.
     */
    @Override
    public CredencialAcceso getById(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        return credencialDAO.getById(id);
    }

    /**
     * Lista todas las credenciales activas (eliminado = false).
     */
    @Override
    public List<CredencialAcceso> getAll() throws Exception {
        return credencialDAO.getAll();
    }

    // ============================================================
    // Reglas de validación
    // ============================================================

    /**
     * Valida que una credencial tenga datos correctos.
     * Reglas:
     * - hashPassword: requerido, trim != "", longitud <= 255
     * - salt: opcional, si no es null/blank longitud <= 64
     * - ultimoCambio: puede ser null (si se envía, debe ser una fecha válida)
     * - requiereReset: boolean (no necesita validación adicional)
     *
     * Nota: Nunca se trabaja con contraseñas en texto plano aquí;
     *       se espera que hashPassword ya venga calculado en capas superiores.
     */
    private void validateCredencial(CredencialAcceso c) {
        if (c == null) {
            throw new IllegalArgumentException("La credencial no puede ser null");
        }

        // hashPassword (requerido)
        String hash = c.getHashPassword();
        if (hash == null || hash.trim().isEmpty()) {
            throw new IllegalArgumentException("El hash de contraseña no puede estar vacío");
        }
        if (hash.length() > HASH_MAX) {
            throw new IllegalArgumentException("El hash de contraseña excede la longitud máxima permitida (" + HASH_MAX + ")");
        }

        // salt (opcional)
        String salt = c.getSalt();
        if (salt != null && !salt.trim().isEmpty() && salt.length() > SALT_MAX) {
            throw new IllegalArgumentException("El salt excede la longitud máxima permitida (" + SALT_MAX + ")");
        }

        // ultimoCambio (opcional): si se pasa, sólo verificamos que no sea futuro absurdo
        LocalDateTime uc = c.getUltimoCambio();
        if (uc != null && uc.isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw new IllegalArgumentException("La fecha de último cambio no puede ser futura");
        }
    }
}

