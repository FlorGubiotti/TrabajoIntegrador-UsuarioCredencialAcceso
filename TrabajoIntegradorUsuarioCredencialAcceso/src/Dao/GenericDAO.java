package Dao;

import java.sql.Connection;
import java.util.List;

public interface GenericDAO<T>{
    
    /** Inserta una nueva entidad. Debe asignar el ID autogenerado al objeto.
     * @param entidad
     * @throws java.lang.Exception */
    void insertar(T entidad) throws Exception;

    /** Inserta dentro de una transacción existente (usando la misma Connection).
     * @param entidad
     * @param conn
     * @throws java.lang.Exception */
    void insertTx(T entidad, Connection conn) throws Exception;

    /** Actualiza los campos de la entidad existente (sin modificar el eliminado).
     * @param entidad
     * @throws java.lang.Exception */
    void actualizar(T entidad) throws Exception;

    /** Marca la entidad como eliminada (baja lógica).
     * @param id
     * @throws java.lang.Exception */
    void eliminar(int id) throws Exception;

    /** Recupera una entidad por su ID (solo si eliminado = false).
     * @param id
     * @return 
     * @throws java.lang.Exception */
    T getById(int id) throws Exception;

    /** Devuelve todas las entidades activas (eliminado = false).
     * @return 
     * @throws java.lang.Exception */
    List<T> getAll() throws Exception;
}
