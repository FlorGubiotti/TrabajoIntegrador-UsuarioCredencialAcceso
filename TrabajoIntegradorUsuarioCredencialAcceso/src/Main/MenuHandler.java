package Main;

import Models.CredencialAcceso;
import Models.Usuario;
import Service.UsuarioServiceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

/**
 * Controlador de operaciones del menú para Usuarios y Credenciales.
 * Gestiona la interacción por consola y delega la lógica de negocio al Service.
 *
 * Responsabilidades:
 * - Capturar y validar entrada básica desde consola (Scanner)
 * - Invocar métodos del servicio (CRUD, búsquedas y operaciones coordinadas)
 * - Mostrar resultados o mensajes de error de forma clara
 *
 * Arquitectura: Main → Service → DAO → Models
 *
 * Nota: Esta clase no contiene reglas de negocio, solo orquesta I/O y llamadas a la capa Service.
 */
public class MenuHandler {

    /** Scanner compartido para lectura de entrada */
    private final Scanner scanner;

    /** Servicio de usuarios (expone también operaciones de credenciales mediante getCredencialService()) */
    private final UsuarioServiceImpl usuarioService;

    /**
     * Constructor con inyección de dependencias.
     * @param scanner Scanner a usar para leer desde consola
     * @param usuarioService Servicio de usuarios
     */
    public MenuHandler(Scanner scanner, UsuarioServiceImpl usuarioService) {
        if (scanner == null) {
            throw new IllegalArgumentException("Scanner no puede ser null");
        }
        if (usuarioService == null) {
            throw new IllegalArgumentException("UsuarioService no puede ser null");
        }
        this.scanner = scanner;
        this.usuarioService = usuarioService;
    }

    // =============================================================================
    // USUARIOS
    // =============================================================================

    /**
     * Opción: Crear nuevo usuario (con credencial opcional).
     * Flujo:
     * 1) Solicita username, email, activo (s/n)
     * 2) Pregunta si desea agregar credencial (s/n)
     * 3) Si sí, captura hash y salt, y marca requiereReset opcional
     * 4) Inserta usuario; si hay credencial, se inserta/actualiza primero desde Service
     */
    public void crearUsuario() {
        try {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();

            System.out.print("Email: ");
            String email = scanner.nextLine().trim();

            System.out.print("¿Activo? (s/n): ");
            boolean activo = scanner.nextLine().trim().equalsIgnoreCase("s");

            // fecha_registro (puede ser null); si querés setear automáticamente:
            LocalDateTime fechaRegistro = LocalDateTime.now();

            CredencialAcceso cred = null;
            System.out.print("¿Desea agregar credencial de acceso? (s/n): ");
            if (scanner.nextLine().trim().equalsIgnoreCase("s")) {
                cred = crearCredencial();
            }

            Usuario u = new Usuario(0, username, email, activo, fechaRegistro);
            u.setCredencial(cred);

            usuarioService.insertar(u);
            System.out.println("Usuario creado exitosamente con ID: " + u.getId());
        } catch (Exception e) {
            System.err.println("Error al crear usuario: " + e.getMessage());
        }
    }

    /**
     * Opción: Listar usuarios o buscar por username/email.
     * Submenú:
     * 1) Listar todos
     * 2) Buscar por username exacto
     * 3) Buscar por email exacto
     */
    public void listarUsuarios() {
        try {
            System.out.print("¿Desea (1) listar todos, (2) buscar por username, (3) buscar por email? Opción: ");
            String op = scanner.nextLine().trim();

            if (null == op) {
                System.out.println("Opción inválida.");
            } else switch (op) {
                case "1" -> {
                    List<Usuario> usuarios = usuarioService.getAll();
                    if (usuarios.isEmpty()) {
                        System.out.println("No se encontraron usuarios.");
                        return;
                    }   for (Usuario u : usuarios) {
                        imprimirUsuario(u);
                    }
                }
                case "2" ->                     {
                        System.out.print("Username exacto: ");
                        String username = scanner.nextLine().trim();
                        Usuario u = usuarioService.buscarPorUsername(username);
                        if (u == null) {
                            System.out.println("No existe usuario con ese username.");
                        } else {
                            imprimirUsuario(u);
                        }                          }
                case "3" ->                     {
                        System.out.print("Email exacto: ");
                        String email = scanner.nextLine().trim();
                        Usuario u = usuarioService.buscarPorEmail(email);
                        if (u == null) {
                            System.out.println("No existe usuario con ese email.");
                        } else {
                            imprimirUsuario(u);
                        }                          }
                default -> System.out.println("Opción inválida.");
            }
        } catch (Exception e) {
            System.err.println("Error al listar/buscar usuarios: " + e.getMessage());
        }
    }

    /**
     * Opción: Actualizar usuario existente.
     * Usa patrón "Enter para mantener".
     * Permite además agregar/actualizar una credencial si el usuario la tiene o no.
     */
    public void actualizarUsuario() {
        try {
            System.out.print("ID del usuario a actualizar: ");
            int id = Integer.parseInt(scanner.nextLine().trim());
            Usuario u = usuarioService.getById(id);

            if (u == null) {
                System.out.println("Usuario no encontrado.");
                return;
            }

            System.out.print("Nuevo username (actual: " + u.getUsername() + ", Enter para mantener): ");
            String username = scanner.nextLine().trim();
            if (!username.isEmpty()) u.setUsername(username);

            System.out.print("Nuevo email (actual: " + u.getEmail() + ", Enter para mantener): ");
            String email = scanner.nextLine().trim();
            if (!email.isEmpty()) u.setEmail(email);

            System.out.print("¿Activo? (actual: " + (u.isActivo() ? "sí" : "no") + ") (s/n/Enter para mantener): ");
            String act = scanner.nextLine().trim();
            if (act.equalsIgnoreCase("s")) u.setActivo(true);
            else if (act.equalsIgnoreCase("n")) u.setActivo(false);

            // Manejo de credencial
            if (u.getCredencial() != null) {
                System.out.print("El usuario tiene credencial. ¿Desea actualizarla? (s/n): ");
                if (scanner.nextLine().trim().equalsIgnoreCase("s")) {
                    actualizarCredencialInPlace(u.getCredencial());
                }
            } else {
                System.out.print("El usuario no tiene credencial. ¿Desea agregar una? (s/n): ");
                if (scanner.nextLine().trim().equalsIgnoreCase("s")) {
                    CredencialAcceso nueva = crearCredencial();
                    u.setCredencial(nueva);
                }
            }

            usuarioService.actualizar(u);
            System.out.println("Usuario actualizado exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al actualizar usuario: " + e.getMessage());
        }
    }

    /**
     * Opción: Eliminar usuario (soft delete).
     */
    public void eliminarUsuario() {
        try {
            System.out.print("ID del usuario a eliminar: ");
            int id = Integer.parseInt(scanner.nextLine().trim());
            usuarioService.eliminar(id);
            System.out.println("Usuario eliminado exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al eliminar usuario: " + e.getMessage());
        }
    }

    /**
     * Opción: Eliminar la credencial de un usuario de forma segura (desasociar FK y luego eliminar).
     */
    public void eliminarCredencialDeUsuario() {
        try {
            System.out.print("ID del usuario: ");
            int usuarioId = Integer.parseInt(scanner.nextLine().trim());
            Usuario u = usuarioService.getById(usuarioId);
            if (u == null) {
                System.out.println("Usuario no encontrado.");
                return;
            }
            if (u.getCredencial() == null) {
                System.out.println("El usuario no tiene credencial asociada.");
                return;
            }
            int credId = u.getCredencial().getId();
            usuarioService.eliminarCredencialDeUsuario(usuarioId, credId);
            System.out.println("Credencial eliminada y desasociada correctamente.");
        } catch (Exception e) {
            System.err.println("Error al eliminar credencial del usuario: " + e.getMessage());
        }
    }

    // =============================================================================
    // CREDENCIALES (operaciones directas)
    // =============================================================================

    /**
     * Opción: Crear credencial independiente (no asociada todavía).
     * Útil para luego asignarla a un usuario en actualizarUsuario().
     */
    public void crearCredencialIndependiente() {
        try {
            CredencialAcceso c = crearCredencial();
            usuarioService.getCredencialService().insertar(c);
            System.out.println("Credencial creada exitosamente con ID: " + c.getId());
        } catch (Exception e) {
            System.err.println("Error al crear credencial: " + e.getMessage());
        }
    }

    /**
     * Opción: Listar credenciales activas.
     */
    public void listarCredenciales() {
        try {
            List<CredencialAcceso> list = usuarioService.getCredencialService().getAll();
            if (list.isEmpty()) {
                System.out.println("No se encontraron credenciales.");
                return;
            }
            for (CredencialAcceso c : list) {
                imprimirCredencial(c);
            }
        } catch (Exception e) {
            System.err.println("Error al listar credenciales: " + e.getMessage());
        }
    }

    /**
     * Opción: Actualizar credencial por ID.
     * Permite cambiar hash, salt, requiereReset y último cambio.
     */
    public void actualizarCredencialPorId() {
        try {
            System.out.print("ID de la credencial a actualizar: ");
            int id = Integer.parseInt(scanner.nextLine().trim());
            CredencialAcceso c = usuarioService.getCredencialService().getById(id);
            if (c == null) {
                System.out.println("Credencial no encontrada.");
                return;
            }
            actualizarCredencialInPlace(c);
            usuarioService.getCredencialService().actualizar(c);
            System.out.println("Credencial actualizada exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al actualizar credencial: " + e.getMessage());
        }
    }

    /**
     * Opción: Eliminar credencial por ID (soft delete directo).
     * Advertencia: si alguna FK la referencia, es preferible usar eliminarCredencialDeUsuario().
     */
    public void eliminarCredencialPorId() {
        try {
            System.out.print("ID de la credencial a eliminar: ");
            int id = Integer.parseInt(scanner.nextLine().trim());
            usuarioService.getCredencialService().eliminar(id);
            System.out.println("Credencial eliminada exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al eliminar credencial: " + e.getMessage());
        }
    }

    // =============================================================================
    // Helpers de captura e impresión
    // =============================================================================

    /** Crea un objeto CredencialAcceso capturando datos por consola (no persiste). */
    private CredencialAcceso crearCredencial() {
        System.out.print("Hash de contraseña (string ya hasheado): ");
        String hash = scanner.nextLine().trim();

        System.out.print("Salt (opcional, Enter para omitir): ");
        String salt = scanner.nextLine().trim();
        if (salt.isEmpty()) salt = null;

        System.out.print("¿Requiere reset en próximo login? (s/n): ");
        boolean requiereReset = scanner.nextLine().trim().equalsIgnoreCase("s");

        CredencialAcceso c = new CredencialAcceso();
        c.setHashPassword(hash);
        c.setSalt(salt);
        c.setRequiereReset(requiereReset);
        c.setUltimoCambio(LocalDateTime.now());
        return c;
    }

    /** Actualiza in-place una credencial ya cargada, con patrón "Enter para mantener". */
    private void actualizarCredencialInPlace(CredencialAcceso c) {
        System.out.print("Nuevo hash (Enter para mantener): ");
        String nh = scanner.nextLine().trim();
        if (!nh.isEmpty()) c.setHashPassword(nh);

        System.out.print("Nuevo salt (Enter para mantener, o '-' para limpiar): ");
        String ns = scanner.nextLine().trim();
        if (!ns.isEmpty()) {
            if (ns.equals("-")) c.setSalt(null);
            else c.setSalt(ns);
        }

        System.out.print("¿Requiere reset? (actual: " + (c.isRequiereReset() ? "sí" : "no") + ") (s/n/Enter): ");
        String rr = scanner.nextLine().trim();
        if (rr.equalsIgnoreCase("s")) c.setRequiereReset(true);
        else if (rr.equalsIgnoreCase("n")) c.setRequiereReset(false);

        // Se actualiza timestamp de último cambio por conveniencia
        c.setUltimoCambio(LocalDateTime.now());
    }
    
    /**
     * Actualizar credencial de acceso de un usuario específico.
     **/
    public void actualizarCredencialPorUsuario() {
        try {
            System.out.print("ID del usuario cuya credencial desea actualizar: ");
            int usuarioId = Integer.parseInt(scanner.nextLine());
            Usuario usuario = usuarioService.getById(usuarioId);

            if (usuario == null) {
                System.out.println("Usuario no encontrado.");
                return;
            }

            if (usuario.getCredencial()== null) {
                System.out.println("El usuario no tiene credencial asociada.");
                return;
            }

            CredencialAcceso cred = usuario.getCredencial();

            System.out.print("¿Desea cambiar la contraseña? (s/n): ");
            if (scanner.nextLine().equalsIgnoreCase("s")) {
                System.out.print("Nueva contraseña: ");
                String nuevaPass = scanner.nextLine().trim();
                if (!nuevaPass.isEmpty()) {
                    cred.setHashPassword(nuevaPass); 
                }
            }

            System.out.print("¿Desea marcar para reinicio de contraseña? (s/n): ");
            if (scanner.nextLine().equalsIgnoreCase("s")) {
                cred.setRequiereReset(true);
            }

            usuarioService.getCredencialService().actualizar(cred);
            System.out.println("Credencial actualizada exitosamente.");

        } catch (Exception e) {
            System.err.println("Error al actualizar credencial: " + e.getMessage());
        }

    
    }
    
    /**
     * Eliminar credencial de acceso de un usuario (MÉTODO SEGURO)
     **/
    public void eliminarCredencialPorUsuario() {
        try {
            System.out.print("ID del usuario cuya credencial desea eliminar: ");
            int usuarioId = Integer.parseInt(scanner.nextLine());
            Usuario usuario = usuarioService.getById(usuarioId);

            if (usuario == null) {
                System.out.println("Usuario no encontrado.");
                return;
            }

            if (usuario.getCredencial() == null) {
                System.out.println("El usuario no tiene credencial asociada.");
                return;
            }

            int credencialId = usuario.getCredencial().getId();
            usuarioService.eliminarCredencialDeUsuario(usuarioId, credencialId);
            System.out.println("Credencial eliminada y desasociada correctamente.");

        } catch (Exception e) {
            System.err.println("Error al eliminar credencial del usuario: " + e.getMessage());
        }
    }


    /** Imprime datos relevantes de un usuario sin exponer datos sensibles de la credencial. */
    private void imprimirUsuario(Usuario u) {
        System.out.println(
                "ID: " + u.getId()
                        + ", username: " + u.getUsername()
                        + ", email: " + u.getEmail()
                        + ", activo: " + (u.isActivo() ? "sí" : "no")
                        + ", fechaRegistro: " + u.getFechaRegistro()
                        + (u.getCredencial() != null ? ", credencialId: " + u.getCredencial().getId() : ", credencialId: -")
        );
    }

    /** Imprime datos de una credencial (sin mostrar información sensible en detalle). */
    private void imprimirCredencial(CredencialAcceso c) {
        System.out.println(
                "ID: " + c.getId()
                        + ", hash(len): " + (c.getHashPassword() == null ? 0 : c.getHashPassword().length())
                        + ", salt(len): " + (c.getSalt() == null ? 0 : c.getSalt().length())
                        + ", requiereReset: " + (c.isRequiereReset() ? "sí" : "no")
                        + ", ultimoCambio: " + c.getUltimoCambio()
        );
    }
}
