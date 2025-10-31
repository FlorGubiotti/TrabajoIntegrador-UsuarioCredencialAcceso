package Main;

import Dao.CredencialAccesoDAO;
import Dao.UsuarioDAO;
import Service.CredencialAccesoServiceImpl;
import Service.UsuarioServiceImpl;

import java.util.Scanner;

/**
 * Orquestador principal del menú de la aplicación.
 * Gestiona el ciclo de vida del menú y coordina todas las dependencias.
 *
 * Responsabilidades:
 * - Crear y gestionar el Scanner único (evita múltiples instancias de System.in)
 * - Inicializar la cadena de dependencias (DAOs → Services → Handler)
 * - Ejecutar el loop principal del menú
 * - Delegar la opción seleccionada al MenuHandler
 * - Cerrar recursos al salir
 *
 * Patrón: Application Controller + Inyección de dependencias manual
 */
public class AppMenu {

    /** Scanner único compartido por toda la aplicación (¡no crear más instancias de System.in!). */
    private final Scanner scanner;

    /** Handler que ejecuta las operaciones del menú (capa de presentación). */
    private final MenuHandler menuHandler;

    /** Flag que controla el loop principal (se apaga con la opción 0). */
    private boolean running;

    /**
     * Constructor: ensambla dependencias y deja listo el ciclo del menú.
     *
     * Flujo:
     * 1) Crea Scanner único
     * 2) Crea cadena DAOs → Services
     * 3) Crea MenuHandler con Scanner y UsuarioService
     * 4) Setea running=true
     */
    public AppMenu() {
        this.scanner = new Scanner(System.in);
        UsuarioServiceImpl usuarioService = createUsuarioService();
        this.menuHandler = new MenuHandler(scanner, usuarioService);
        this.running = true;
    }

    /**
     * Punto de entrada.
     * @param args
     */
    public static void main(String[] args) {
        AppMenu app = new AppMenu();
        app.run();
    }

    /**
     * Loop principal del menú:
     * - Muestra menú
     * - Lee opción (maneja NumberFormatException)
     * - Procesa la opción
     * - Cierra scanner al salir
     */
    public void run() {
        while (running) {
            try {
                MenuDisplay.mostrarMenuPrincipal();
                int opcion = Integer.parseInt(scanner.nextLine().trim());
                processOption(opcion);
            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida. Por favor, ingrese un número.");
            }
        }
        scanner.close();
    }

    /**
     * Mapea la opción seleccionada a los métodos del MenuHandler.
     * La numeración debe coincidir con MenuDisplay.mostrarMenuPrincipal().
     *
     * 1  → Crear usuario
     * 2  → Listar/buscar usuarios
     * 3  → Actualizar usuario
     * 4  → Eliminar usuario (soft delete)
     * 5  → Crear credencial independiente
     * 6  → Listar credenciales
     * 7  → Actualizar credencial por ID
     * 8  → Eliminar credencial por ID (soft delete directo)
     * 9  → Actualizar credencial por ID de usuario
     * 10 → Eliminar credencial por ID de usuario (secuencias seguras de desasociación + delete)
     * 0  → Salir
     */
    private void processOption(int opcion) {
        switch (opcion) {
            case 1 -> menuHandler.crearUsuario();
            case 2 -> menuHandler.listarUsuarios();
            case 3 -> menuHandler.actualizarUsuario();
            case 4 -> menuHandler.eliminarUsuario();

            case 5 -> menuHandler.crearCredencialIndependiente();
            case 6 -> menuHandler.listarCredenciales();
            case 7 -> menuHandler.actualizarCredencialPorId();
            case 8 -> menuHandler.eliminarCredencialPorId();
            case 9 -> menuHandler.actualizarCredencialPorUsuario();
            case 10 -> menuHandler.eliminarCredencialPorUsuario();

            case 0 -> {
                System.out.println("Saliendo...");
                running = false;
            }
            default -> System.out.println("Opción no válida.");
        }
    }

    /**
     * Factory Method que crea la cadena de dependencias (bottom-up).
     *
     * Orden:
     * 1) DAOs: UsuarioDAO, CredencialAccesoDAO
     * 2) Services: CredencialAccesoServiceImpl (→ DAO credenciales),
     *              UsuarioServiceImpl (→ DAO usuarios + service credenciales)
     *
     * De esta forma, UsuarioServiceImpl puede coordinar operaciones que
     * involucren persistencia de usuario y su credencial asociada.
     */
    private UsuarioServiceImpl createUsuarioService() {
        CredencialAccesoDAO credencialDAO = new CredencialAccesoDAO();
        UsuarioDAO usuarioDAO = new UsuarioDAO();

        CredencialAccesoServiceImpl credencialService = new CredencialAccesoServiceImpl(credencialDAO);
        return new UsuarioServiceImpl(usuarioDAO, credencialService);
    }
}
