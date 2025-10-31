package Service;

import java.util.List;

/**
 * Servicio genérico de negocio para operaciones estándar sobre entidades.
 *
 * Convenciones del modelo:
 * - Las validaciones de dominio (no nulos, longitudes, unicidad, coherencia) se realizan aquí,
 *   antes de invocar al DAO.
 * - eliminar(int id) aplica baja lógica (soft delete), nunca DELETE físico.
 * - getAll() devuelve únicamente entidades activas (eliminado = false).
 * - Las transacciones se orquestan desde la capa Service cuando una operación
 *   involucra múltiples DAOs; los DAO exponen variantes con Connection cuando corresponda.
 *
 * @param <T> tipo de entidad de dominio
 */
public interface GenericService<T> {

    /** Inserta una nueva entidad (valida reglas de negocio antes de persistir).
     * @param entidad
     * @throws java.lang.Exception */
    void insertar(T entidad) throws Exception;

    /** Actualiza una entidad existente (sin modificar el flag de eliminado).
     * @param entidad
     * @throws java.lang.Exception */
    void actualizar(T entidad) throws Exception;

    /** Realiza baja lógica de la entidad (eliminado = true).
     * @param id
     * @throws java.lang.Exception */
    void eliminar(int id) throws Exception;

    /** Obtiene una entidad por su ID (solo si no está eliminada).
     * @param id
     * @return
     * @throws java.lang.Exception  */
    T getById(int id) throws Exception;

    /** Lista todas las entidades activas (eliminado = false).
     * @return 
     * @throws java.lang.Exception */
    List<T> getAll() throws Exception;
}
