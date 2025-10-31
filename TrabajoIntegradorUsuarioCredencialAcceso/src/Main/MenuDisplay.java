package Main;

/**
 * Clase utilitaria para mostrar el menú de la aplicación.
 * Solo contiene métodos estáticos de visualización (no tiene estado).
 *
 * Responsabilidades:
 * - Mostrar el menú principal con todas las opciones disponibles
 * - Formatear la salida de forma consistente
 *
 * Patrón: Utility class (solo métodos estáticos, no instanciable)
 *
 * IMPORTANTE: Esta clase NO lee entrada del usuario.
 * Solo muestra el menú. AppMenu es responsable de leer la opción.
 */
public final class MenuDisplay {

    // Evita instanciación accidental
    private MenuDisplay() {}

    /**
     * Muestra el menú principal con todas las opciones CRUD.
     *
     * Opciones de Usuarios (1-4):
     * 1. Crear usuario: Permite crear usuario con credencial opcional
     * 2. Listar usuarios: Lista todos o busca por username/email
     * 3. Actualizar usuario: Actualiza datos del usuario y opcionalmente su credencial
     * 4. Eliminar usuario: Soft delete (NO elimina credencial asociada)
     *
     * Opciones de Credenciales (5-10):
     * 5. Crear credencial: Crea credencial independiente (sin asociar a usuario)
     * 6. Listar credenciales: Lista todas las credenciales activas
     * 7. Actualizar credencial por ID: Actualiza credencial directamente (hash/salt/reset)
     * 8. Eliminar credencial por ID: PELIGROSO - si está referenciada puede dejar FK huérfana
     * 9. Actualizar credencial por ID de usuario: Busca usuario primero, luego actualiza su credencial
     * 10. Eliminar credencial por ID de usuario: SEGURO - desasocia FK primero y luego elimina
     *
     * Opción de salida:
     * 0. Salir: Termina la aplicación
     *
     * Formato:
     * - Separador visual "========= MENU ========="
     * - Lista numerada clara
     * - Prompt "Ingrese una opcion: " sin salto de línea (espera input)
     *
     * Nota: Los números de opción deben corresponder al switch en AppMenu.processOption().
     */
    public static void mostrarMenuPrincipal() {
        System.out.println("\n========= MENU =========");
        System.out.println("1. Crear usuario");
        System.out.println("2. Listar usuarios");
        System.out.println("3. Actualizar usuario");
        System.out.println("4. Eliminar usuario");
        System.out.println("5. Crear credencial");
        System.out.println("6. Listar credenciales");
        System.out.println("7. Actualizar credencial por ID");
        System.out.println("8. Eliminar credencial por ID");
        System.out.println("9. Actualizar credencial por ID de usuario");
        System.out.println("10. Eliminar credencial por ID de usuario");
        System.out.println("0. Salir");
        System.out.print("Ingrese una opcion: ");
    }
}
