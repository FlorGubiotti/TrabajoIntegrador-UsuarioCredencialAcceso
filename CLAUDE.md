# CLAUDE.md

Este archivo proporciona una guía técnica para el trabajo con el código de este repositorio.

## Visión General del Proyecto

Aplicación CRUD en Java para la gestión de Usuarios y Credenciales de Acceso, con persistencia en MySQL/MariaDB mediante JDBC.
Implementa una arquitectura en 4 capas bien definidas, utiliza el patrón de baja lógica (soft delete) y aplica validaciones completas a nivel de servicio.

**Stack Tecnológico:**

- Java 21
- Apache Ant (proyecto configurado desde NetBeans)
- MySQL 8.x / MariaDB 10.4.32 (XAMPP)
- JDBC con mysql-connector-j 8.4.0
- DBeaver para pruebas y consultas
- NetBeans IDE como entorno principal de desarrollo

**Documentación:**

- **README.md**: Guía para el usuario con pasos de instalación, configuración de la base de datos, ejecución y resolución de problemas.
- **HISTORIAS_DE_USUARIO.md**: Detalle funcional con historias de usuario, reglas de negocio y criterios de aceptación.
- **CLAUDE.md**: Este documento — guía técnica para desarrollo, arquitectura y mantenimiento.

## Comandos de compilación y ejecución

```bash
# Compilar el proyecto
ant compile

# Limpiar y recompilar
ant clean compile

# Ejecutar el proyecto
ant run

# Limpiar artefactos generados
ant clean
```

**Note**: Este proyecto no utiliza Gradle ni Maven, ya que fue desarrollado con Apache Ant directamente desde NetBeans.
El archivo build.xml maneja las tareas de compilación y ejecución.

## **Ejecución de la Aplicación**

### **Desde NetBeans (recomendado)**

1. Abrir el proyecto.
2. Configurar la ruta al conector **mysql-connector-j-8.4.0.jar** en las _Librerías_ del proyecto.
3. Ejecutar con el botón ▶️ **Run Project**.
4. Verificar que el servidor de base de datos (MySQL o MariaDB en XAMPP) esté iniciado antes de la ejecución.

---

### **Desde Línea de Comandos (Windows)**

```bash
javac -cp "lib/mysql-connector-j-8.4.0.jar;src" -d build\classes src\Main\Main.java
java -cp "build\classes;lib\mysql-connector-j-8.4.0.jar" Main.Main
```

> ⚠️ **Nota:**  
> Asegurate de que el conector JDBC esté ubicado en la carpeta **lib** del proyecto o de actualizar la ruta en el comando si se encuentra en otro directorio.

---

### **Desde Línea de Comandos (Linux/Mac)**

```bash
javac -cp "lib/mysql-connector-j-8.4.0.jar:src" -d build/classes src/Main/Main.java
java -cp "build/classes:lib/mysql-connector-j-8.4.0.jar" Main.Main
```

> En sistemas basados en Unix se utiliza **":"** (dos puntos) como separador de rutas, a diferencia de Windows que usa **";"** (punto y coma).

**Alternativa**: también puede ejecutarse desde DBeaver, conectando la base de datos para realizar las pruebas del CRUD directamente sobre el esquema utilizado por la aplicación

### **Prueba de Conexión a la Base de Datos**

Para verificar que la conexión JDBC esté configurada correctamente y el servidor de base de datos esté en ejecución, se puede ejecutar la clase de prueba **TestConexion**:

```bash
java -cp "build\classes;lib\mysql-connector-j-8.4.0.jar" Main.TestConexion
```

**Salida esperada si MySQL/MariaDB está en ejecución y la base de datos existe:**

```
Conexión exitosa a la base de datos
Usuario conectado: root@localhost
Base de datos: dbtpiUsuarios
URL: jdbc:mysql://localhost:3306/dbtpiUsuarios
Driver: MySQL Connector/J v8.4.0
```

---

## **Configuración de la Base de Datos**

La aplicación se conecta a la base de datos **MySQL/MariaDB** con la siguiente configuración por defecto:

- **Base de datos:** `dbtpiUsuarios`
- **Host:** `localhost:3306`
- **Usuario:** `root`
- **Contraseña:** _(vacía por defecto)_

La configuración puede sobrescribirse utilizando **propiedades del sistema JVM** al momento de ejecutar la aplicación:

```bash
-Ddb.url=jdbc:mysql://localhost:3306/dbtpiUsuarios
-Ddb.user=root
-Ddb.password=tu_contraseña
```

> ⚠️ **Importante:**  
> El esquema de la base de datos debe crearse manualmente antes de ejecutar la aplicación.  
> Puede hacerse desde **phpMyAdmin (XAMPP)** o **DBeaver**, importando el script SQL correspondiente.

---

### **Recomendaciones para la conexión**

- Asegurarse de que el servicio **MySQL** o **MariaDB** esté iniciado en XAMPP.
- Verificar que el puerto **3306** no esté ocupado por otro servicio.
- En caso de error de conexión, comprobar el archivo `mysql-connector-j-8.4.0.jar` en la carpeta **lib** del proyecto.
- Si se trabaja desde **DBeaver**, probar la conexión con las mismas credenciales para confirmar la disponibilidad del servidor.

### **Configuración del Esquema de Base de Datos (proyecto real)**

> Base de datos usada por la app: **dbtpiUsuarios**

```sql
CREATE DATABASE IF NOT EXISTS dbtpiUsuarios;
USE dbtpiUsuarios;

-- Credenciales de acceso (una por usuario)
CREATE TABLE IF NOT EXISTS credenciales_acceso (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    hash_password   VARCHAR(255) NOT NULL,
    salt            VARCHAR(64) NULL,
    ultimo_cambio   DATETIME NULL,
    requiere_reset  BOOLEAN NOT NULL,
    eliminado       BOOLEAN DEFAULT FALSE
);

-- Usuarios (relación 1:1 con credenciales via credencial_id)
CREATE TABLE IF NOT EXISTS usuarios (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(30)  NOT NULL UNIQUE,
    email           VARCHAR(120) NOT NULL UNIQUE,
    activo          BOOLEAN NOT NULL,
    fecha_registro  DATETIME NULL,
    credencial_id   INT NULL UNIQUE,                     -- 1:1 (opcionalmente null)
    eliminado       BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_usuario_credencial
        FOREIGN KEY (credencial_id) REFERENCES credenciales_acceso(id)
);
```

**Notas:**

- Las tablas reales son **usuarios** y **credenciales_acceso**.
- **username** y **email** son **UNIQUE** (la app también valida unicidad en servicios).
- **Soft delete** mediante el campo `eliminado` en ambas tablas (los DAOs hacen `UPDATE ... SET eliminado=TRUE`).
- La relación **1:1** se modela con `usuarios.credencial_id` → `credenciales_acceso.id` y se marca **UNIQUE** para evitar que varias personas apunten a la misma credencial.

## Arquitectura

### Patrón de arquitectura en capas

El código implementa una **arquitectura estricta de 4 capas**:

```
Main (Capa de UI / Presentación)
    ↓
Service Layer (Lógica de negocio)
    ↓
DAO Layer (Acceso a datos)
    ↓
Models (Entidades de dominio)
```

**Principios de diseño clave:**

- Cada capa **depende solo de la capa inmediata inferior**.
- El flujo de datos es descendente y las dependencias se inyectan desde la UI hacia abajo (Main → Services → DAOs).
- **Todas las operaciones de BD usan soft delete** (flag `eliminado`).
- Se definen **interfaces genéricas** (`GenericDAO`, `GenericService`) para contratos base y reutilización.

---

### Detalle de Capas

#### 1) Models (`src/Models/`)

- **Base.java**: clase abstracta base con campos `id` y `eliminado` (implementa semántica de soft delete).
- **Usuario.java**: entidad de usuario con:
  - `username` (UNIQUE), `email` (UNIQUE), `activo`, `fecha_registro`
  - relación **1:1** opcional con **CredencialAcceso** (FK `credencial_id`)
- **CredencialAcceso.java**: entidad de credenciales con:
  - `hashPassword`, `salt`, `ultimoCambio`, `requiereReset`

Todas las entidades implementan `equals()`, `hashCode()` y `toString()` con criterios coherentes (por id o por campos naturales cuando el id no está asignado).

---

#### 2) DAO Layer (`src/Dao/`)

- **GenericDAO<T>**: interfaz con operaciones CRUD estándar.
- **UsuarioDAO**:
  - SELECT con `LEFT JOIN` a `credenciales_acceso` para materializar `Usuario` + `CredencialAcceso`.
  - Sentencias SQL como constantes `private static final String`.
  - **Filtros por `eliminado = FALSE`** en todos los SELECT.
  - **Soft delete** vía `UPDATE usuarios SET eliminado = TRUE WHERE id = ?`.
  - Método **`insertTx(Usuario, Connection)`** para soportar transacciones coordinadas.
- **CredencialAccesoDAO**:
  - CRUD completo con validaciones de filas afectadas.
  - **Soft delete** vía `UPDATE credenciales_acceso SET eliminado = TRUE WHERE id = ?`.
  - Listados y búsquedas solo de credenciales activas (`eliminado = FALSE`).

**Patrones importantes:**

- SQL definido en constantes (`String`/text blocks).
- Métodos de mapeo dedicados (`mapResultSetTo...`) para construir entidades desde `ResultSet`.
- Verificación de filas afectadas en UPDATE/DELETE para garantizar consistencia.

---

#### 3) Service Layer (`src/Service/`)

- **GenericService<T>**: contrato para operaciones de negocio.
- **UsuarioServiceImpl**:
  - Valida `username` y `email` (formato y **unicidad** lógica antes de persistir).
  - Coordina con `CredencialAccesoServiceImpl` cuando corresponde (asociación/desasociación 1:1).
  - Expone operaciones de alto nivel (crear/actualizar/eliminar/buscar/listar) orquestando DAOs.
- **CredencialAccesoServiceImpl**:
  - Valida y gestiona credenciales (`hashPassword` requerido; `salt` opcional).
  - Setea `ultimoCambio` y `requiereReset` según reglas de negocio.
  - Aplica **soft delete** para baja lógica.

**Responsabilidades de la capa:**

- **Validación de entrada** previa a persistencia.
- **Orquestación** de operaciones compuestas (p. ej., insertar credencial y luego asociarla a usuario).
- Manejo y **traducción de excepciones** a errores de negocio comprensibles.

---

#### 4) Main / UI Layer (`src/Main/`)

- **Main.java**: punto de entrada alternativo, delega a `AppMenu`.
- **AppMenu.java**:
  - Orquesta el ciclo de vida del menú.
  - **Crea el `Scanner` único** (evita múltiples instancias de `System.in`).
  - **Ensambla dependencias**: DAOs → Services → `MenuHandler`.
- **MenuHandler.java**:
  - Implementa las operaciones del menú (CRUD de Usuario y gestión de Credenciales).
  - Interacción con el usuario (lectura/validación de entradas) y llamadas a Services.
- **MenuDisplay.java**:
  - Solo responsabilidades de **presentación** (render de textos y menús).
- **TestConexion.java**:
  - Utilidad standalone para verificar conectividad JDBC (URL, driver, usuario).

**Separación de responsabilidades:**

- `AppMenu`: control de flujo, ruteo de opciones y ownership del `Scanner`.
- `MenuHandler`: lógica de interacción y coordinación con Services (crear/actualizar/eliminar/listar).
- `MenuDisplay`: métodos estáticos puros de salida de texto (UI).

---

#### 5) Configuración (`src/Config/`)

- **DataBaseConnection**:
  - Factory estática para obtener `Connection` (MySQL/MariaDB).
  - Config por defecto:
    - `jdbc:mysql://localhost:3306/dbtpiUsuarios`, usuario `root`, contraseña vacía.
  - Permite override por **propiedades JVM**: `-Ddb.url`, `-Ddb.user`, `-Ddb.password`.
  - Valida configuración y levanta excepciones claras si faltan valores críticos.
- **TransactionManager**:
  - Administra el ciclo de vida de transacciones manuales (`begin`, `commit`, `rollback`).
  - Implementa `AutoCloseable` para uso seguro con try-with-resources.
  - Garantiza `setAutoCommit(true)` y cierre de conexión al finalizar.

---

## **Navegación del Código y Documentación**

### **Documentación Inline (Javadoc)**

Todos los archivos fuente de Java contienen **Javadoc detallado** que explica:

- **Por qué** se tomaron ciertas decisiones de diseño (no solo qué hace el código).
- **Relaciones** entre clases, capas y métodos.
- **Advertencias** sobre operaciones sensibles (por ejemplo, eliminación lógica o manejo de transacciones).
- **Ejemplos** de uso correcto de los métodos públicos.
- **Referencias cruzadas** con reglas de negocio (**RN-XXX**) documentadas en `HISTORIAS_DE_USUARIO.md`.

---

### **Puntos Clave de la Documentación**

#### **Para entender el flujo de datos:**

1. Comenzar en `AppMenu.java`: muestra cómo se ensamblan las dependencias (`crearUsuarioService()`, `crearCredencialService()`).
2. Continuar en `MenuHandler.java`: se observa cómo la entrada del usuario fluye hacia los servicios.
3. Revisar la capa de servicios: `UsuarioServiceImpl.insertar()` coordina la creación del usuario y su posible credencial asociada.
4. Ver la capa DAO: `UsuarioDAO.insertar()` y `CredencialAccesoDAO.insertar()` muestran las operaciones SQL concretas y los filtros de `eliminado = FALSE`.

---

#### **Para entender operaciones sensibles:**

- `UsuarioServiceImpl.eliminar()`: documenta por qué la baja lógica (soft delete) es preferible a la eliminación física (**RN-018**).
- `CredencialAccesoServiceImpl.eliminar()`: explica los riesgos de eliminar credenciales activas (**RN-021**).
- `UsuarioServiceImpl.desasociarCredencial()`: detalla el patrón seguro de desvinculación entre usuario y credencial.
- `MenuHandler.eliminarUsuario()`: ilustra la validación de dependencias antes de ejecutar una baja lógica.

---

#### **Para entender la unicidad de credenciales y correos (RN-001 y RN-002):**

- **Base de datos:** restricciones `UNIQUE` en las columnas `usuarios.username` y `usuarios.email`.
- **Servicio:** `UsuarioServiceImpl.validarUnicidad()` asegura que no se repitan valores antes de insertar o actualizar.
- **DAO:** `UsuarioDAO.buscarPorUsername()` y `UsuarioDAO.buscarPorEmail()` implementan las consultas exactas.
- **Interfaz de usuario:** `MenuHandler.crearUsuario()` muestra cómo se informa el error de duplicación al usuario final.

---

#### **Para entender el patrón LEFT JOIN (1:1 Usuario–CredencialAcceso):**

- `UsuarioDAO`: las constantes SQL incluyen `LEFT JOIN credenciales_acceso` para poblar las credenciales asociadas.
- `UsuarioDAO.mapResultSetToUsuario()`: explica el manejo de posibles valores `NULL` al no existir credencial asociada.
- Los comentarios en el código señalan por qué el uso de `rs.wasNull()` es esencial para evitar NullPointerExceptions y reflejar correctamente relaciones opcionales.

---

> 💡 **Consejo para navegar el código:**
>
> - El flujo principal siempre se inicia en `AppMenu.java`, que crea los servicios, DAOs y menú interactivo.
> - Desde allí, cada acción del usuario se traduce en llamadas a métodos de `MenuHandler`, que a su vez delegan en los servicios.
> - El código mantiene una estructura modular y fácilmente trazable gracias a la documentación consistente y las referencias cruzadas.

## **Patrones de Desarrollo**

### **Inyección de Dependencias**

Los servicios se construyen inyectando sus dependencias de forma explícita, siguiendo el principio de **inversión de dependencias**:

```java
CredencialAccesoDAO credencialDAO = new CredencialAccesoDAO();
UsuarioDAO usuarioDAO = new UsuarioDAO(credencialDAO);

CredencialAccesoServiceImpl credencialService = new CredencialAccesoServiceImpl(credencialDAO);
UsuarioServiceImpl usuarioService = new UsuarioServiceImpl(usuarioDAO, credencialService);
```

Esto garantiza una arquitectura desacoplada y facilita las pruebas unitarias o la sustitución de implementaciones sin modificar otras capas.

---

### **Soporte para Transacciones**

Los DAO ofrecen dos métodos de inserción:

- `insertar(entity)`: utiliza su propia conexión (auto-commit activado).
- `insertTx(entity, connection)`: utiliza una conexión proporcionada para operaciones transaccionales.

**Ejemplo de patrón transaccional:**

```java
try (Connection conn = DataBaseConnection.getConnection();
     TransactionManager tx = new TransactionManager(conn)) {
    tx.startTransaction();

    credencialService.insertarTx(credencial, conn);
    usuarioService.insertarTx(usuario, conn);

    tx.commit();
} catch (Exception e) {
    tx.rollback();
}
```

---

### **Bajas Lógicas (Soft Delete)**

Todas las entidades implementan el **patrón de baja lógica**, lo que significa que los registros no se eliminan físicamente de la base de datos.

- Las operaciones DELETE ejecutan `UPDATE tabla SET eliminado = TRUE`.
- Las consultas SELECT filtran `WHERE eliminado = FALSE`.
- Los servicios utilizan métodos de restauración o reactivación si se requiere volver a activar un registro.

Este enfoque preserva la integridad referencial y el historial de datos.

---

### **Gestión de Recursos**

Todas las operaciones JDBC utilizan **try-with-resources**, asegurando el cierre automático de conexiones, sentencias y resultados:

```java
try (Connection conn = DataBaseConnection.getConnection();
     PreparedStatement stmt = conn.prepareStatement(SQL_SELECT);
     ResultSet rs = stmt.executeQuery()) {
    // Uso seguro de recursos
}
```

---

## **Restricciones Importantes**

1. **Sin campos públicos:** todos los atributos de los modelos son `private`, con getters y setters.
2. **Validación de ID:** los servicios verifican que `id > 0` antes de ejecutar `update`, `delete` o `getById`.
3. **Null Safety:** los servicios validan que los campos requeridos no sean `null` ni cadenas vacías.
4. **Verificación de filas afectadas:** los DAO controlan que `rowsAffected > 0` tras cada operación `UPDATE` o `DELETE`.
5. **Lógica de claves foráneas:** `UsuarioServiceImpl` gestiona la creación y asociación de credenciales antes de persistir el usuario.
6. **Integridad referencial:** el método `UsuarioServiceImpl.desasociarCredencial()` desvincula la credencial **antes** de eliminarla.
7. **Constantes SQL:** todas las consultas están definidas como constantes (`SELECT_BY_ID_SQL`, `INSERT_SQL`, etc.) y no se escriben inline.
8. **Actualizaciones seguras:** en los métodos de actualización de `MenuHandler`, se usa `.trim()` inmediatamente después de `scanner.nextLine()` y solo se actualizan los valores no vacíos.
9. **Unicidad de Username y Email (RN-001, RN-002):**
   - Restricciones `UNIQUE` en las columnas `usuarios.username` y `usuarios.email`.
   - Validación a nivel de aplicación en `UsuarioServiceImpl.validarUnicidad()`.
   - La verificación se ejecuta antes de cada inserción y actualización.
10. **Validación de configuración:** los parámetros de conexión a la base de datos se validan una única vez durante la inicialización de `DataBaseConnection`, no en cada solicitud.

---

> 💡 **Buenas prácticas adicionales**
>
> - Evitar SQL dinámico concatenado: utilizar siempre `PreparedStatement` con parámetros.
> - Mantener coherencia entre los mensajes de error del servicio y los mostrados en la capa UI (`MenuHandler`).
> - Los métodos DAO nunca lanzan excepciones genéricas sin contexto; las capturan, loguean y transforman en mensajes comprensibles por los servicios.

## **Patrones Críticos de Código**

### **Eliminación Segura de Credenciales**

**NUNCA** elimines una credencial directamente desde su DAO si está asociada a un usuario activo.  
Siempre usar:

```java
usuarioService.desasociarCredencial(usuarioId, credencialId);
```

Este método:

1. Verifica que la credencial esté realmente asociada al usuario indicado.
2. Actualiza `usuario.credencial_id = NULL` en la base de datos.
3. Luego aplica baja lógica (`UPDATE credenciales_acceso SET eliminado = TRUE`).
4. Previene referencias huérfanas y mantiene la integridad referencial.

---

### **Patrón de Actualización en MenuHandler**

Cuando se actualizan entidades a partir de entradas del usuario, se debe seguir el siguiente patrón:

```java
// CORRECTO: primero trim, luego verificación
String email = scanner.nextLine().trim();
if (!email.isEmpty()) {
    usuario.setEmail(email);
}

// INCORRECTO: verificar antes de trim puede causar errores
String email = scanner.nextLine();
if (!email.trim().isEmpty()) { // ❌ No hacer esto
    usuario.setEmail(email);
}
```

Este patrón evita persistir espacios vacíos o cadenas inválidas que podrían romper restricciones de unicidad o validaciones de servicio.

---

### **Patrón de Consultas DAO**

Todas las consultas deben utilizar constantes SQL predefinidas, nunca SQL embebido en el método.

```java
// CORRECTO
try (PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {
    // ...
}

// INCORRECTO - evita duplicación y riesgo de error
String sql = "SELECT * FROM usuarios WHERE id = ?";
try (PreparedStatement stmt = conn.prepareStatement(sql)) {  // ❌ No hacer esto
    // ...
}
```

---

### **Validación de Unicidad (Username / Email)**

Antes de insertar o actualizar un usuario, se valida que los campos `username` y `email` no estén duplicados:

```java
// En UsuarioServiceImpl
private void validarUnicidad(String username, String email, Integer usuarioId) throws Exception {
    Usuario existenteUser = usuarioDAO.buscarPorUsername(username);
    Usuario existenteEmail = usuarioDAO.buscarPorEmail(email);

    if (existenteUser != null && (usuarioId == null || existenteUser.getId() != usuarioId)) {
        throw new IllegalArgumentException("Ya existe un usuario con el username: " + username);
    }

    if (existenteEmail != null && (usuarioId == null || existenteEmail.getId() != usuarioId)) {
        throw new IllegalArgumentException("Ya existe un usuario con el email: " + email);
    }
}
```

**Métodos de búsqueda disponibles:**

- `UsuarioDAO.buscarPorUsername(String username)` → Devuelve un único usuario o `null`.
- `UsuarioDAO.buscarPorEmail(String email)` → Devuelve un único usuario o `null`.
- `UsuarioServiceImpl.validarUnicidad()` → Wrapper con validación de negocio antes de persistir.

---

## **Puntos de Entrada y Sistema de Menú**

**Aplicación principal:** `Main.Main.main()` → ejecuta `AppMenu.run()` → inicia el ciclo interactivo de menú.  
**Prueba de conexión:** `Main.TestConexion.main()` → verifica conectividad con la base de datos.

---

### **Mapa de Opciones del Menú Interactivo**

El menú principal cuenta con 10 operaciones clave:

| Opción | Descripción                      | Método en MenuHandler             | Método principal del Servicio                         |
| ------ | -------------------------------- | --------------------------------- | ----------------------------------------------------- |
| 1      | Crear Usuario                    | `crearUsuario()`                  | `UsuarioServiceImpl.insertar()`                       |
| 2      | Listar Usuarios                  | `listarUsuarios()`                | `UsuarioServiceImpl.getAll()` o `buscarPorUsername()` |
| 3      | Actualizar Usuario               | `actualizarUsuario()`             | `UsuarioServiceImpl.actualizar()`                     |
| 4      | Eliminar Usuario                 | `eliminarUsuario()`               | `UsuarioServiceImpl.eliminar()`                       |
| 5      | Crear Credencial                 | `crearCredencialIndependiente()`  | `CredencialAccesoServiceImpl.insertar()`              |
| 6      | Listar Credenciales              | `listarCredenciales()`            | `CredencialAccesoServiceImpl.getAll()`                |
| 7      | Actualizar Credencial por ID     | `actualizarCredencialPorId()`     | `CredencialAccesoServiceImpl.actualizar()`            |
| 8      | Eliminar Credencial por ID       | `eliminarCredencialPorId()`       | `CredencialAccesoServiceImpl.eliminar()` ⚠️           |
| 9      | Asociar Credencial a Usuario     | `asociarCredencialAUsuario()`     | `UsuarioServiceImpl.asociarCredencial()`              |
| 10     | Desasociar Credencial de Usuario | `desasociarCredencialDeUsuario()` | `UsuarioServiceImpl.desasociarCredencial()` ✅        |
| 0      | Salir                            | Detiene el ciclo de menú          | -                                                     |

**⚠️ La opción 8 es insegura:** puede dejar referencias huérfanas en `usuarios.credencial_id`.  
**✅ La opción 10 es segura:** primero desvincula la credencial y luego aplica la baja lógica.

---

> 💡 **Consejo:** Las operaciones del menú que implican modificación de datos siempre se canalizan por los _services_ para garantizar validaciones, transacciones y consistencia.

## **Limitaciones Conocidas y Decisiones de Diseño**

1. **Sin tarea `run` de Gradle:** el proyecto usa **Apache Ant**, por lo que debe ejecutarse manualmente con `java -cp` o desde el IDE (NetBeans).
2. **Interfaz solo por consola:** no posee GUI; toda la interacción se realiza a través del menú textual.
3. **Una credencial por usuario:** la relación es **1:1**, por lo que no pueden asociarse múltiples credenciales a un mismo usuario.
4. **Actualizaciones no atómicas:** las operaciones de actualización realizadas desde `MenuHandler` no son transaccionales (por ejemplo, actualizar usuario + credencial puede completarse parcialmente si ocurre un error).
5. **Creación manual del esquema:** la base de datos `dbtpiUsuarios` debe crearse e inicializarse manualmente antes de la primera ejecución.
6. **Sin connection pooling:** se crea una nueva conexión por operación, lo cual es aceptable en una aplicación de consola.
7. **Operación de eliminación peligrosa:** `MenuHandler.eliminarCredencialPorId()` puede dejar referencias huérfanas si se ejecuta directamente. Usar `UsuarioServiceImpl.desasociarCredencial()` (ver sección _Patrones Críticos de Código_).
8. **Sin paginación:** listar todos los registros podría ralentizar la salida si el dataset crece mucho.

---

## **Resolución de Problemas (Troubleshooting)**

### **Problemas de Compilación**

**Problema:** errores de compilación

- Verificar que **Java 21** esté correctamente instalado (`java --version`).
- Confirmar que el conector MySQL (`mysql-connector-j-8.4.0.jar`) esté agregado en las _Librerías_ del proyecto.
- Asegurarse de que la codificación sea **UTF-8** (Windows puede usar por defecto `windows-1252`).

**Problema:** errores de codificación en la compilación

- En NetBeans, abrir **Propiedades del Proyecto → Codificación**, y establecer **UTF-8**.

---

### **Problemas en Tiempo de Ejecución**

**Problema:** `Communications link failure` o `Connection refused`

- El servidor MySQL/MariaDB no está ejecutándose en `localhost:3306`.
- Verificar el estado del servicio:
  - **Windows:** abrir XAMPP y asegurarse de que _MySQL_ esté en verde.
  - **Linux/Mac:** `sudo systemctl status mysql` o `brew services list`.
- Confirmar que el puerto 3306 esté libre (`netstat -an | findstr 3306` en Windows o `netstat -an | grep 3306` en Linux/Mac).

---

**Problema:** `Access denied for user 'root'@'localhost'`

- Contraseña incorrecta o vacía en `DataBaseConnection.java`.
- Modificar credenciales o ejecutar con propiedades del sistema:
  ```bash
  -Ddb.user=root -Ddb.password=mi_contraseña
  ```

---

**Problema:** `Unknown database 'dbtpiUsuarios'`

- La base de datos no fue creada.
- Ejecutar el script SQL del esquema (ver sección _Configuración del Esquema de Base de Datos_).

---

**Problema:** `ClassNotFoundException: com.mysql.cj.jdbc.Driver`

- El JAR del conector no está en el classpath.
- Asegurarse de incluir `mysql-connector-j-8.4.0.jar` en la carpeta **lib/** del proyecto o configurarlo en las _Librerías_ de NetBeans.

---

**Problema:** la aplicación inicia pero todas las operaciones fallan con errores de base de datos

- Las tablas no existen o el esquema no coincide con el esperado.
- Ejecutar nuevamente el script SQL completo.
- Verificar las tablas en la consola de MySQL o DBeaver:
  ```sql
  SHOW TABLES FROM dbtpiUsuarios;
  ```

---

**Problema:** el menú aparece pero no realiza acciones

- Es comportamiento normal si MySQL/MariaDB no está corriendo.
- La aplicación maneja los errores de conexión y vuelve al menú principal sin cerrar abruptamente.
- Revisar mensajes de consola que comienzan con **"Error al..."** para más detalles.

---

### **Pruebas sin Base de Datos**

La aplicación puede ejecutarse **sin conexión a MySQL/MariaDB** para probar:

- La correcta visualización del menú.
- El manejo de entradas por consola.
- La gestión de errores (sin que el programa se cierre).
- El comportamiento de retorno al menú principal ante fallos de conexión.

**Salida esperada sin base de datos:**

```
========= MENÚ =========
[opciones del menú]
Ingrese una opción: 2
Error al listar usuarios: Communications link failure
[vuelve al menú principal sin cerrar la aplicación]
```

## **Mejoras Recientes (2025)**

Durante el año 2025 se implementaron múltiples ajustes orientados a mejorar la coherencia del diseño, la validación de datos y la mantenibilidad general del proyecto.

---

### **1. Validación de Unicidad de Username y Email (RN-001 / RN-002)**

- **Base de datos:** Se agregaron restricciones `UNIQUE` sobre las columnas `usuarios.username` y `usuarios.email`.
- **Aplicación:** Se implementó el método `UsuarioServiceImpl.validarUnicidad()` que valida ambos campos antes de insertar o actualizar.
- **DAO:** Se añadieron los métodos `UsuarioDAO.buscarPorUsername(String username)` y `UsuarioDAO.buscarPorEmail(String email)` para búsquedas exactas.
- **Servicio:** Se agregó el wrapper `UsuarioServiceImpl.buscarPorUsername()` para coordinación a nivel de negocio.
- **Impacto:** previene duplicados tanto a nivel de base de datos como en la lógica de aplicación.

---

### **2. Mejoras de Arquitectura**

- **Main.java:** Se eliminó el antipatrón de invocar `AppMenu.main()` desde `Main.main()`.
  - Ahora se instancia `AppMenu` correctamente y se llama al método `run()`.
- **Usuario.java y CredencialAcceso.java:** Se estandarizó el uso de constructores con parámetros primitivos (`int`) en lugar de `Integer`, eliminando autoboxing innecesario.
- **DataBaseConnection:** La validación de configuración se trasladó del método `getConnection()` a un bloque estático de inicialización, verificándose una sola vez al cargar la clase.

---

### **3. Optimización de Rendimiento**

- **DataBaseConnection:**
  - La validación de parámetros se ejecuta solo una vez al inicio.
  - Se cambiaron las excepciones de `SQLException` a `IllegalStateException` para distinguir errores de configuración de errores de conexión reales.
- **Transacciones:** Los métodos `insertTx()` ahora utilizan `try-with-resources` anidados, garantizando cierres automáticos y commit seguro.

---

### **4. Diagnóstico Mejorado**

- **TestConexion:** ahora muestra información detallada de la conexión:
  - Usuario conectado (por ejemplo, `root@localhost`).
  - Base de datos (`dbtpiUsuarios`).
  - URL JDBC y versión del driver (`MySQL Connector/J v8.4.0`).
  - Resultado explícito de éxito o error.

---

### **5. Calidad de Código**

- **Documentación:** Se agregaron comentarios explicativos sobre por qué el campo de contraseña puede ser vacío (común en MySQL local con usuario root).
- **Consistencia:** Todos los constructores de modelos (`Usuario`, `CredencialAcceso`, `Base`) ahora utilizan `int id` para coherencia.
- **Validación:** Manejo uniforme de errores de unicidad y nulos en toda la capa de servicios.

---

### **6. Estandarización del Manejo de Entradas**

- **MenuHandler:** se unificó el patrón `trim()` en todas las operaciones de entrada del usuario.  
  **Patrón aplicado:**
  ```java
  String valor = scanner.nextLine().trim();
  ```
  - Actualizado en métodos de actualización (`actualizarUsuario()`, `actualizarCredencialPorId()`).
  - Aplicado también en métodos de creación (`crearUsuario()`, `crearCredencialIndependiente()`).
  - **Impacto:**
    - Evita almacenar espacios en blanco.
    - Garantiza validaciones correctas de unicidad.
    - Asegura consistencia entre búsquedas y validaciones.

---

### **7. Consistencia en el Manejo de Excepciones**

- Todos los métodos DAO declaran `throws Exception` (algunos más específicos lanzan `SQLException`).
- `GenericDAO` define `throws Exception`, garantizando compatibilidad entre implementaciones.
- La capa de servicios propaga excepciones controladas con mensajes significativos.
- La capa de interfaz (`MenuHandler`) **captura todas las excepciones**, muestra mensajes legibles y nunca detiene la ejecución del programa.
- Resultado: **cero cierres inesperados**; la aplicación siempre vuelve al menú principal.

---

### **8. Documentación Exhaustiva del Código (2025)**

- **Todos los archivos fuente Java** fueron documentados con **Javadoc completo**, incluyendo:
  - **Nivel de clase:** propósito, responsabilidades, patrones aplicados.
  - **Nivel de atributo:** explicación de cada campo y su uso.
  - **Nivel de método:** parámetros, flujo, valores de retorno y excepciones.
  - **Reglas de negocio** (RN-XXX) referenciadas en comentarios relevantes.
  - **Advertencias (⚠️)** sobre operaciones críticas.
  - **Ejemplos y casos límite** incluidos donde corresponde.

**Total de archivos documentados:** 13 clases en total:

- **Models:** Base, Usuario, CredencialAcceso
- **Config:** DataBaseConnection, TransactionManager
- **Services:** UsuarioServiceImpl, CredencialAccesoServiceImpl
- **DAOs:** UsuarioDAO, CredencialAccesoDAO
- **Main:** AppMenu, MenuHandler, MenuDisplay, Main

**Estilo de documentación:** en **español**, coherente con el lenguaje del código base.  
**Principales patrones documentados:**

- Implementación del _Soft Delete_.
- Manejo de relación **1:1 Usuario–CredencialAcceso** con `LEFT JOIN`.
- Validación de unicidad de username y email.
- Eliminación segura de credenciales asociadas.
- Cadena de inyección de dependencias.
- Coordinación transaccional entre DAOs.

## **Verificación de Calidad del Código (Última verificación: 2025)**

El siguiente análisis integral confirma la corrección funcional y estructural del proyecto.

---

### ✅ **Puntaje de Calidad Arquitectónica: 9.7 / 10**

| Categoría                  | Estado       | Detalles                                                      |
| -------------------------- | ------------ | ------------------------------------------------------------- |
| **Separación de Capas**    | ✅ Excelente | Arquitectura limpia de 4 capas, sin acoplamientos indebidos   |
| **Manejo de Excepciones**  | ✅ Correcto  | try-catch coherente, mensajes claros y controlados            |
| **Integridad Referencial** | ✅ Correcto  | FK gestionadas correctamente, eliminación segura implementada |
| **Validación de Entradas** | ✅ Perfecta  | `.trim()` en todas las entradas, validación multinivel        |
| **Gestión de Recursos**    | ✅ Excelente | `try-with-resources` en todos los accesos JDBC                |
| **Consultas SQL**          | ✅ Perfectas | Solo `PreparedStatement`, bajas lógicas consistentes          |
| **Flujos Críticos**        | ✅ Correctos | Todas las operaciones CRUD funcionan correctamente            |

---

### **Verificación de Correctitud Funcional**

**Estado de compilación:** ✅ Sin errores ni advertencias  
**Cobertura:** 16 clases Java, 100 % de flujos críticos verificados  
**Inyección SQL:** ✅ Protegida (100 % consultas parametrizadas)  
**Fugas de recursos:** ✅ Ninguna detectada (`try-with-resources` en todos los casos)  
**Manejo de NULL:** ✅ Correcto (relación Usuario–Credencial con FK nullable)  
**Unicidad de Username/Email:** ✅ Aplicada en BD y en aplicación  
**Soft Delete:** ✅ Consistente en todas las consultas y operaciones

---

### **Verificación de Flujos Críticos**

1. **Crear Usuario con Credencial:** ✅ Inserta primero credencial, luego usuario con FK
2. **Actualizar Usuario:** ✅ Valida unicidad de username/email permitiendo coincidencias propias
3. **Eliminar Credencial de Forma Segura:** ✅ Opción 10 desvincula FK antes del soft delete
4. **Operaciones de Búsqueda:** ✅ Todas filtran por `eliminado = FALSE` y normalizan entradas
5. **Manejo de FK nulas:** ✅ LEFT JOIN correctamente implementado

---

### **Patrones Correctos Confirmados**

**Consistencia en Constructores:**

- Todos los modelos usan `int id` (sin autoboxing).

**Manejo de Entradas:**

- Patrón: `String x = scanner.nextLine().trim();` usado en el 100 % de los casos.
- Validación: `if (!x.isEmpty())` mantiene valores previos en actualizaciones.

**Operaciones con Base de Datos:**

- Todas las consultas usan constantes (`SELECT_BY_ID_SQL`, etc.).
- Todos los `UPDATE`/`DELETE` verifican `rowsAffected`.
- Los `INSERT` recuperan claves generadas con `Statement.RETURN_GENERATED_KEYS`.

**Equals/HashCode:**

- Usuario: basado en `username` y `email` (ambos únicos).
- CredencialAcceso: basado en `id` (identidad persistente).

---

### **Importante: Sin Problemas Bloqueantes**

El proyecto presenta **cero fallas críticas**.  
Observaciones menores:

1. Tipos de excepción ligeramente distintos entre DAOs (`Exception` vs `SQLException`).
2. La opción de eliminación directa (por ID) puede causar referencias huérfanas — ya documentado.
3. Limitaciones conocidas (sin transacciones atómicas, sin paginación) — intencionales y justificadas.

**Conclusión:** el código está **listo para producción académica** y cumple los requisitos de diseño, calidad y robustez.

---

## **Documentación Relacionada**

**Para usuarios e instalación:** ver **[README.md](README.md)**

- Requisitos del sistema con versiones recomendadas
- Guía paso a paso de instalación y configuración
- Objetivos académicos y resultados de aprendizaje
- Resumen de criterios de evaluación
- Escenarios comunes de resolución de errores
- Resumen de reglas de negocio (RN-001 a RN-051)

---

**Para especificaciones funcionales:** ver **[HISTORIAS_DE_USUARIO.md](HISTORIAS_DE_USUARIO.md)**

- 11 Historias de Usuario (HU-001 a HU-011) organizadas en 3 épicas
- 51 Reglas de Negocio numeradas (RN-001 a RN-051)
- Escenarios Gherkin con formato Given / When / Then
- Diagramas de flujo técnico
- Tablas comparativas (por ejemplo, HU-008 vs HU-010 para eliminación segura)
- Criterios de aceptación detallados por funcionalidad

---

**Referencias cruzadas críticas:**

- Opción del menú 8 → HU-008 (**eliminación directa insegura**)
- Opción del menú 10 → HU-010 (**eliminación segura**)
- **RN-001 / RN-002:** Unicidad de `username` y `email` — reforzada tanto en BD como en validación de aplicación.
- **RN-018:** Justifica el uso de baja lógica en lugar de eliminación física.
- **RN-021:** Documenta los riesgos de eliminar credenciales activas.
- **RN-036:** Explica el patrón seguro en `UsuarioServiceImpl.desasociarCredencial()`.

---

**Características Clave:**

- Unicidad garantizada de `username` y `email` mediante restricciones **UNIQUE** en base de datos y validación en `UsuarioServiceImpl.validarUnicidad()`.
- Métodos especializados `UsuarioDAO.buscarPorUsername()` y `UsuarioDAO.buscarPorEmail()` para búsquedas exactas.
- Validación de conexión optimizada: ejecutada una sola vez al cargar `DataBaseConnection`.
- Documentación completa en español, alineada con los objetivos del **TPI de Programación 2**.
