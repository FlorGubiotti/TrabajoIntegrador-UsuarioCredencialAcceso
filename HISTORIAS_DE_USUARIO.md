# Historias de Usuario - Sistema de Gestión de Usuarios y Credenciales

Especificaciones funcionales completas del sistema CRUD de usuarios y credenciales.

## Tabla de Contenidos

- [Épica 1: Gestión de Usuarios](#épica-1-gestión-de-usuarios)
- [Épica 2: Gestión de Credenciales](#épica-2-gestión-de-credenciales)
- [Épica 3: Operaciones Asociadas](#épica-3-operaciones-asociadas)
- [Reglas de Negocio](#reglas-de-negocio)
- [Modelo de Datos](#modelo-de-datos)

---

## Épica 1: Gestión de Usuarios

### HU-001: Crear Usuario

**Como** operador del sistema  
**Quiero** registrar un nuevo usuario con sus datos básicos  
**Para** mantener la información de usuarios y (opcionalmente) sus credenciales en la base de datos

#### Criterios de Aceptación

```gherkin
Escenario: Crear usuario sin credencial
  Dado que el operador selecciona "Crear usuario"
  Cuando ingresa username "anatorres" y email "ana.torres@example.com"
  Y responde "n" a crear una credencial
  Entonces el sistema crea el usuario con ID autogenerado y activo = TRUE
  Y muestra "Usuario creado exitosamente con ID: X"

Escenario: Crear usuario con credencial
  Dado que el operador selecciona "Crear usuario"
  Cuando ingresa username "lgomez" y email "l.gomez@example.com"
  Y responde "s" a crear una credencial
  Y proporciona hash_password "<hash>" y (opcional) salt "<salt>"
  Y establece requiere_reset = FALSE
  Entonces el sistema crea primero la credencial (registrando ultimo_cambio)
  Y luego crea el usuario referenciando esa credencial
  Y muestra "Usuario y credencial creados exitosamente con ID: X"

Escenario: Intento de crear usuario con username o email duplicado
  Dado que existe un usuario con username "anatorres" o email "ana.torres@example.com"
  Cuando el operador intenta registrar otro usuario con los mismos valores
  Entonces el sistema muestra "Ya existe un usuario con ese nombre de usuario o email"
  Y no crea el registro

Escenario: Intento de crear usuario con campos vacíos
  Dado que el operador selecciona "Crear usuario"
  Cuando deja vacío el username o el email (solo espacios o Enter)
  Entonces el sistema muestra "El campo no puede estar vacío"
  Y no crea el registro
```

#### Reglas de Negocio Aplicables

- **RN-001**: username y email son obligatorios; se aplican trim() y validaciones de formato.
- **RN-002**: username y email deben ser únicos en el sistema (validación en Service + constraints en BD).
- **RN-003**: El ID del usuario se genera automáticamente.
- **RN-004**: La credencial es opcional durante la creación de usuario.
- **RN-005**: La credencial almacena solo hash_password (y salt opcional); nunca texto plano.
- **RN-006**: La relación Usuario–Credencial es 1:1 opcional y se garantiza con UNIQUE (credencial_id).
- **RN-007**: Al crear/actualizar credencial, se registra ultimo_cambio y se puede marcar requiere_reset.

#### Implementación Técnica

- **Clase**: `MenuHandler.crearUsuario()`
- **Servicio**: `UsuarioServiceImpl.insertar()` (y `CredencialAccesoServiceImpl.insertar()` si se crea credencial)
- **Validación**: `UsuarioServiceImpl.validateUsuario()`, `validateUsernameUnique()`, `validateEmailUnique()`
- **Flujo**:

  1. Captura la entrada y aplica `.trim()` a `username` y `email`.
  2. Construye `Usuario` con `activo = true` y `fechaRegistro = now`.
  3. Si el operador elige crear credencial:
     - Construye `CredencialAcceso` con `hashPassword`, `salt` (opcional), `requiereReset` (false por defecto) y `ultimoCambio = now`.
     - Inserta **primero** la credencial y obtiene su `id`.
     - Setea `credencialId` en el `Usuario`.
  4. Abre transacción (`TransactionManager`) y **inserta Usuario** con FK `credencial_id` si corresponde.
  5. Confirma (`commit`) al finalizar con éxito; ante error realiza `rollback`.
  6. Retorna el **ID autogenerado** del usuario usando `RETURN_GENERATED_KEYS`.

- **Notas DAO/SQL**:
  - `UsuarioDAO.insert(...)` y `CredencialAccesoDAO.insert(...)` usan `PreparedStatement` con `RETURN_GENERATED_KEYS`.
  - Restricciones de unicidad en BD: `UNIQUE (username)`, `UNIQUE (email)` y `UNIQUE (credencial_id)` para garantizar 1→1.
  - FK `credencial_id` con `ON DELETE SET NULL` para no romper referencias si se elimina la credencial.

---

### HU-002: Listar Todos los Usuarios

**Como** operador del sistema  
**Quiero** ver un listado de todos los usuarios registrados  
**Para** consultar la información almacenada

#### Criterios de Aceptación

```gherkin
Escenario: Listar todos los usuarios con credencial asociada
  Dado que existen usuarios en el sistema
  Cuando el operador selecciona "Listar usuarios"
  Y elige opción "1" (listar todos)
  Entonces el sistema muestra todos los usuarios con eliminado = FALSE
  Y para cada usuario que tenga credencial muestra "Credencial ID: [id] (requiereReset=[true|false])"

Escenario: Listar usuarios sin credencial
  Dado que existen usuarios sin credencial asociada
  Cuando el operador lista todos los usuarios
  Entonces el sistema muestra esos usuarios sin línea de credencial

Escenario: No hay usuarios en el sistema
  Dado que no existen usuarios activos
  Cuando el operador lista todos los usuarios
  Entonces el sistema muestra "No se encontraron usuarios."
```

#### Reglas de Negocio Aplicables

- **RN-L1**: Solo se listan usuarios con eliminado = FALSE.
- **RN-L2**: La credencial (si existe) se obtiene mediante LEFT JOIN.
- **RN-L3**: Si credencial_id es NULL, no se muestra información de credencial.
- **RN-L4**: Los resultados no deben incluir usuarios ni credenciales marcados como eliminados.

#### Implementación Técnica

- **Clase**: `MenuHandler.listarUsuarios()`
- **Servicio**: `UsuarioServiceImpl.getAll()`
- **DAO**: `UsuarioDAO.getAll()` con `SELECT_ALL_SQL` + `LEFT JOIN` a credenciales
- **Query**:
  ```sql
  SELECT  u.id, u.username, u.email, u.activo, u.fecha_registro, u.credencial_id,
        c.id        AS cred_id,
        c.requiere_reset AS cred_requiere_reset,
        c.ultimo_cambio  AS cred_ultimo_cambio
    FROM usuarios u
    LEFT JOIN credenciales_acceso c ON u.credencial_id = c.id AND c.eliminado = FALSE
    WHERE u.eliminado = FALSE;
  ```

---

### HU-003: Buscar Usuario por Username o Email

**Como** operador del sistema  
**Quiero** buscar usuarios por su `username` o `email`  
**Para** localizar rápidamente un registro específico

#### Criterios de Aceptación

```gherkin
Escenario: Buscar por username exacto
  Dado que existen usuarios con username "anatorres" y "lgomez"
  Cuando el operador ingresa "anatorres" en la opción Buscar por username
  Entonces el sistema muestra el usuario con username "anatorres"
  Y no muestra otros usuarios

Escenario: Buscar por email exacto
  Dado que existen usuarios con email "ana.torres@example.com" y "l.gomez@example.com"
  Cuando el operador ingresa "ana.torres@example.com" en la opción Buscar por email
  Entonces el sistema muestra el usuario con email "ana.torres@example.com"
  Y no muestra otros usuarios

Escenario: Búsqueda sin resultados
  Dado que no existe un usuario con username "usuario_inexistente"
  Cuando el operador realiza la búsqueda por username "usuario_inexistente"
  Entonces el sistema muestra "No se encontraron usuarios."

Escenario: Búsqueda con espacios
  Dado que el operador ingresa "  anatorres  " con espacios
  Cuando se ejecuta la búsqueda por username
  Entonces el sistema aplica trim y busca por "anatorres"
```

#### Reglas de Negocio Aplicables

- **RN-B1**: Solo se consideran usuarios con eliminado = FALSE.
- **RN-B2**: La búsqueda es exacta por username o email (no se usa LIKE).
- **RN-B3**: Se aplica trim() a la entrada antes de consultar.
- **RN-B4**: No se permiten búsquedas vacías.
- **RN-B5**: Si credencial_id es NULL, no se imprime información de credencial.

#### Implementación Técnica

- **Clase**: `MenuHandler.listarUsuarios()`
  - Subopciones:
    - **(1)** Listar todos los usuarios
    - **(2)** Buscar por username
    - **(3)** Buscar por email
- **Servicio**:
  - `UsuarioServiceImpl.buscarPorUsername(String username)`
  - `UsuarioServiceImpl.buscarPorEmail(String email)`
- **DAO**:
  - `UsuarioDAO.findByUsername(String username)`
  - `UsuarioDAO.findByEmail(String email)`
- **Query**:

  ```sql
  -- Buscar por username (exacto)
    SELECT u.id, u.username, u.email, u.activo, u.fecha_registro, u.credencial_id,
        c.id AS cred_id, c.requiere_reset AS cred_requiere_reset, c.ultimo_cambio AS cred_ultimo_cambio
    FROM usuarios u
    LEFT JOIN credenciales_acceso c ON u.credencial_id = c.id AND c.eliminado = FALSE
    WHERE u.eliminado = FALSE AND u.username = ?;

    -- Buscar por email (exacto)
    SELECT u.id, u.username, u.email, u.activo, u.fecha_registro, u.credencial_id,
        c.id AS cred_id, c.requiere_reset AS cred_requiere_reset, c.ultimo_cambio AS cred_ultimo_cambio
    FROM usuarios u
    LEFT JOIN credenciales_acceso c ON u.credencial_id = c.id AND c.eliminado = FALSE
    WHERE u.eliminado = FALSE AND u.email = ?;
  ```

- **Parámetros**: valores exactos (sin comodines), un placeholder `?` para username o email.

---

### HU-004: Actualizar Usuario

**Como** operador del sistema  
**Quiero** modificar los datos de un usuario existente  
**Para** mantener su información actualizada y, si corresponde, su credencial asociada

#### Criterios de Aceptación

```gherkin
Escenario: Actualizar solo email
  Dado que existe el usuario ID 1 con email "ana.torres@example.com"
  Cuando el operador actualiza el usuario ID 1
  Y presiona Enter en username y activo
  Y escribe "ana.t@example.com" en email
  Entonces el sistema actualiza solo el email
  Y mantiene username y activo sin cambios

Escenario: Actualizar con username o email duplicado
  Dado que existen usuarios con username "anatorres" y "lgomez"
  Cuando el operador intenta cambiar el username de "lgomez" a "anatorres"
  Entonces el sistema muestra "Ya existe un usuario con ese nombre de usuario o email"
  Y no actualiza el registro

Escenario: Actualizar manteniendo el mismo username/email
  Dado que el usuario ID 1 tiene username "anatorres" y email "ana.torres@example.com"
  Cuando el operador modifica otros campos manteniendo username y/o email iguales
  Entonces el sistema permite la actualización
  Y no dispara error de duplicado

Escenario: Agregar credencial a usuario sin credencial
  Dado que el usuario ID 1 no tiene credencial
  Cuando el operador actualiza el usuario
  Y responde "s" a agregar credencial
  Y proporciona hash_password "<hash>" y (opcional) salt "<salt>"
  Entonces el sistema crea la credencial
  Y la asocia al usuario (credencial_id = nuevo ID)

Escenario: Actualizar credencial de usuario con credencial existente
  Dado que el usuario ID 2 tiene credencial asociada
  Cuando el operador responde "s" a actualizar credencial
  Y establece requiere_reset = TRUE
  Entonces el sistema actualiza la credencial (requiere_reset = TRUE, ultimo_cambio = now)
  Y mantiene la asociación al mismo usuario

Escenario: ID inválido
  Dado que el operador ingresa ID 0 o un ID inexistente
  Cuando intenta actualizar
  Entonces el sistema informa que el ID no es válido o no existe
  Y no realiza cambios

```

#### Reglas de Negocio Aplicables

- **RN-014**: Se valida unicidad de username y email excepto para el mismo usuario (actualización propia permitida).
- **RN-015**: Campos vacíos (Enter) mantienen valor original
- **RN-016**: Se requiere ID > 0 para actualizar
- **RN-017**: Se puede agregar o actualizar credencial durante la actualización del usuario.
- **RN-018**: Trim se aplica antes de validar si el campo está vacío

#### Implementación Técnica

- **Clase**: `MenuHandler.actualizarUsuario()`
- **Servicio**: `UsuarioServiceImpl.actualizar()`
- **Validación**:

  - `validateUsernameUniqueExcept(username, usuarioId)` permite mantener el mismo `username` sin lanzar error.
  - `validateEmailUniqueExcept(email, usuarioId)` permite conservar el mismo `email`.
  - `validateId(usuarioId)` asegura que el ID sea positivo y que el usuario exista.

- **Pattern (mantener valor actual si el campo queda vacío)**:

  ```java
  String username = scanner.nextLine().trim();
  if (!username.isEmpty()) {
      usuario.setUsername(username);
  }

  ```

---

### HU-005: Eliminar Usuario

**Como** operador del sistema  
**Quiero** eliminar un usuario del sistema  
**Para** mantener únicamente registros activos y evitar inconsistencias en los datos

#### Criterios de Aceptación

```gherkin
Escenario: Eliminar usuario existente
  Dado que existe el usuario con ID 1
  Cuando el operador elimina el usuario ID 1
  Entonces el sistema actualiza eliminado = TRUE
  Y muestra "Usuario eliminado exitosamente."

Escenario: Eliminar usuario inexistente
  Dado que no existe usuario con ID 999
  Cuando el operador intenta eliminar usuario ID 999
  Entonces el sistema muestra "No se encontró usuario con ID: 999"

Escenario: Usuario eliminado no aparece en listados
  Dado que el usuario ID 1 fue eliminado
  Cuando el operador lista todos los usuarios
  Entonces el usuario ID 1 no aparece en los resultados

Escenario: Eliminar usuario con credencial asociada
  Dado que el usuario ID 2 tiene una credencial asociada
  Cuando el operador elimina el usuario
  Entonces el sistema marca eliminado = TRUE en el usuario
  Y mantiene la credencial existente sin eliminarla físicamente
  (la FK credencial_id se conserva para trazabilidad)
```

#### Reglas de Negocio Aplicables

- **RN-019**: Eliminación es lógica, no física
- **RN-020**: Se ejecuta `UPDATE usuarios SET eliminado = TRUE`
- **RN-021**: La credencial asociada NO se elimina automáticamente
- **RN-022**: Se verifica `rowsAffected` para confirmar eliminación

#### Implementación Técnica

- **Clase**: `MenuHandler.eliminarUsuario()`
- **Servicio**: `UsuarioServiceImpl.eliminar()`
- **DAO**: `UsuarioDAO.eliminar()` con `UPDATE_SQL` (soft delete)
- **Query**: `UPDATE usuarios SET eliminado = TRUE WHERE id = ?`

---

## Épica 2: Gestión de Credenciales

### HU-006: Crear Credencial Independiente

**Como** operador del sistema  
**Quiero** crear una credencial sin asociarla a ningún usuario  
**Para** tener credenciales disponibles para asignación posterior

#### Criterios de Aceptación

```gherkin
Escenario: Crear credencial válida
  Dado que el operador selecciona "Crear credencial"
  Cuando ingresa un hash de contraseña "<hash>" y (opcional) un salt "<salt>"
  Entonces el sistema crea la credencial con ID autogenerado
  Y establece requiere_reset = FALSE y eliminado = FALSE
  Y muestra "Credencial creada exitosamente con ID: X"

Escenario: Crear credencial con campos vacíos
  Dado que el operador selecciona "Crear credencial"
  Cuando deja el hash de contraseña vacío
  Entonces el sistema muestra "El campo hash_password no puede estar vacío"
  Y no crea la credencial
```

#### Reglas de Negocio Aplicables

- **RN-C01**: hash_password es obligatorio.
- **RN-C02**: salt es opcional, pero si se ingresa se aplica .trim() antes de guardar.
- **RN-C03**: Se genera automáticamente:
  - id (AUTO_INCREMENT)
  - ultimo_cambio = CURRENT_TIMESTAMP
  - requiere_reset = FALSE
  - eliminado = FALSE
- **RN-C04**: No se asocia a ningún usuario al momento de la creación (credencial inicial sin usuario_id vinculado).

#### Implementación Técnica

- **Clase**: `MenuHandler.crearCredencialIndependiente()`
- **Helper**: `MenuHandler.cargarDatosCredencial()` (aplica `.trim()` a entradas opcionales como `salt`)
- **Servicio**: `CredencialAccesoServiceImpl.insertar(CredencialAcceso credencial)`
- **DAO**: `CredencialAccesoDAO.insert(CredencialAcceso credencial)` con `INSERT_SQL` y `RETURN_GENERATED_KEYS`

---

### HU-007: Listar Credenciales

**Como** operador del sistema  
**Quiero** ver todas las credenciales registradas  
**Para** consultar el estado de acceso y administración de contraseñas

#### Criterios de Aceptación

```gherkin
Escenario: Listar credenciales existentes
  Dado que existen credenciales activas en el sistema
  Cuando el operador selecciona "Listar credenciales"
  Entonces el sistema muestra el ID, fecha de último cambio y estado de requiere_reset
  Y solo muestra credenciales con eliminado = FALSE

Escenario: No hay credenciales
  Dado que no existen credenciales activas
  Cuando el operador lista credenciales
  Entonces el sistema muestra "No se encontraron credenciales."
```

#### Reglas de Negocio Aplicables

- **RN-C05**: Solo se listan credenciales con eliminado = FALSE.
- **RN-C06**: Se muestran tanto las credenciales asociadas a usuarios como las independientes.
- **RN-C07**: Los campos sensibles (hash_password, salt) no se muestran por motivos de seguridad.
- **RN-C08**: El listado debe ordenarse por id ASC o ultimo_cambio DESC según la implementación.

#### Implementación Técnica

- **Clase**: `MenuHandler.listarCredenciales()`
- **Servicio**: `CredencialAccesoServiceImpl.getAll()`
- **DAO**: `CredencialAccesoDAO.getAll()` con `SELECT_ALL_SQL`
- **Query**: `SELECT id, requiere_reset, ultimo_cambio FROM credenciales_acceso WHERE eliminado = FALSE ORDER BY id`

---

### HU-008: Eliminar Credencial por ID (Operación Peligrosa)

**Como** operador del sistema  
**Quiero** eliminar una credencial directamente por su ID  
**Para** remover credenciales no utilizadas

⚠️ **ADVERTENCIA**: Esta operación puede generar inconsistencias si la credencial está asociada a un usuario (según la configuración de la FK o la lógica aplicada).

#### Criterios de Aceptación

```gherkin
Escenario: Eliminar credencial no asociada
  Dado que existe credencial ID 5 sin usuarios asociados
  Cuando el operador elimina la credencial ID 5
  Entonces el sistema marca eliminado = TRUE
  Y muestra "Credencial eliminada exitosamente."

Escenario: Eliminar credencial asociada (problema)
  Dado que existe credencial ID 1 asociada al usuario ID 10
  Cuando el operador elimina la credencial ID 1 por esta opción
  Entonces el sistema marca la credencial como eliminada
  Y el usuario ID 10 puede quedar sin credencial o provocar error según la FK
  (si hay ON DELETE SET NULL: se desasocia; si no, puede fallar por restricción)
```

#### Reglas de Negocio Aplicables

- **RN-C09**: La eliminación es lógica sobre credenciales_acceso (eliminado = TRUE).
- **RN-C10**: No verifica si está asociada a un usuario antes de eliminar.
- **RN-C11**: Puede causar inconsistencia lógica si el usuario esperaba tener credencial (aunque la FK sea SET NULL).
- **RN-C12**: Usar HU-010 (Eliminar credencial por ID de usuario) como alternativa segura (desvincula primero).

#### Implementación Técnica

- **Clase**: `MenuHandler.eliminarCredencialPorId()`
- **Servicio**: `CredencialAccesoServiceImpl.eliminar(int id)`
- **DAO**: `CredencialAccesoDAO.delete(int id)` (soft delete con UPDATE)
- **Limitación**: No verifica `usuarios.credencial_id` antes de eliminar.

---

### HU-009: Actualizar Credencial por ID

**Como** operador del sistema  
**Quiero** actualizar los datos de una credencial existente usando su ID  
**Para** mantener las contraseñas y configuraciones de acceso actualizadas

#### Criterios de Aceptación

```gherkin
Escenario: Actualizar hash de contraseña
  Dado que existe credencial ID 3 con un hash registrado
  Cuando el operador actualiza la credencial ID 3
  Y escribe un nuevo hash en el campo correspondiente
  Y presiona Enter en los demás campos
  Entonces el sistema actualiza solo el hash de la contraseña
  Y mantiene los valores anteriores en salt y requiere_reset

Escenario: Actualizar credencial inexistente
  Dado que no existe credencial ID 999
  Cuando el operador intenta actualizarla
  Entonces el sistema muestra "Credencial no encontrada."

Escenario: Actualizar marca de requiere_reset
  Dado que existe credencial ID 2
  Cuando el operador elige establecer requiere_reset en TRUE
  Entonces el sistema actualiza el valor y registra la fecha en ultimo_cambio

```

#### Reglas de Negocio Aplicables

- **RN-C13**: Se permite actualizar credenciales por ID existente.
- **RN-C14**: Campos vacíos o nulos mantienen el valor original.
- **RN-C15**: El campo ultimo_cambio se actualiza automáticamente al modificar la credencial.
- **RN-C16**: No se permite dejar hash_password vacío.
- **RN-C17**: La actualización no puede alterar el ID ni forzar eliminación lógica.

#### Implementación Técnica

- **Clase**: `MenuHandler.actualizarCredencialPorId()`
- **Servicio**: `CredencialAccesoServiceImpl.actualizar(CredencialAcceso credencial)`
- **DAO**: `CredencialAccesoDAO.update(CredencialAcceso credencial)` con `UPDATE_SQL`
- **Pattern**: Usa `.trim()` y verifica `isEmpty()`

---

## Épica 3: Operaciones Asociadas

### HU-010: Eliminar Credencial por ID de Usuario (Operación Segura)

**Como** operador del sistema  
**Quiero** eliminar la credencial asociada a un usuario específico  
**Para** removerla sin dejar referencias inconsistentes

✅ **RECOMENDADO**: Esta es la forma segura de eliminar una credencial asociada

#### Criterios de Aceptación

```gherkin
Escenario: Eliminar credencial de usuario correctamente
  Dado que el usuario ID 1 tiene credencial ID 5
  Cuando el operador elimina la credencial por ID de usuario 1
  Entonces el sistema primero actualiza usuarios.credencial_id = NULL
  Y luego marca credenciales_acceso ID 5 como eliminado = TRUE
  Y muestra "Credencial eliminada exitosamente y referencia actualizada."

Escenario: Usuario sin credencial
  Dado que el usuario ID 1 no tiene credencial asociada
  Cuando el operador intenta eliminar su credencial
  Entonces el sistema muestra "El usuario no tiene credencial asociada."
  Y no ejecuta ninguna operación

Escenario: Validación de pertenencia
  Dado que el usuario ID 1 tiene credencial ID 5
  Cuando el servicio intenta eliminar la credencial ID 7 del usuario ID 1
  Entonces el sistema muestra "La credencial no pertenece a este usuario."
  Y no elimina nada
```

#### Reglas de Negocio Aplicables

- **RN-035U**: Se actualiza la FK (usuarios.credencial_id = NULL) antes de eliminar la credencial.
- **RN-036U**: Se valida que la credencial pertenezca al usuario indicado.
- **RN-037U**: Operación en dos pasos atómicos: UPDATE usuario → UPDATE credencial (soft delete).
- **RN-038U**: Debe ejecutarse en transacción para evitar estados intermedios.
- **RN-039U**: La eliminación de credenciales es lógica (eliminado = TRUE)

#### Implementación Técnica

- **Clase**: `MenuHandler.eliminarCredencialPorUsuario()`
- **Servicio**: `UsuarioServiceImpl.eliminarCredencialDeUsuario(int usuarioId, int credencialId)`
- **Flujo**:
  1. Valida IDs > 0
  2. Obtiene usuario por ID
  3. Valida que tenga credencial asociada
  4. Valida que el `credencial_id` coincida
  5. `usuario.setCredencial(null)`
  6. `usuarioDAO.actualizar(usuario)` → `credencial_id = NULL`
  7. `credencialAccesoServiceImpl.eliminar(credencialId)` → `eliminado = TRUE`

#### Comparación HU-008 vs HU-010

| Aspecto             | HU-008 (Por ID de Credencial)         | HU-010 (Por ID de Usuario)             |
| ------------------- | ------------------------------------- | -------------------------------------- |
| **Validación**      | No verifica asociación                | Verifica pertenencia                   |
| **Referencias**     | Puede dejar inconsistencias           | Actualiza FK primero                   |
| **Seguridad**       | ⚠️ Peligroso                          | ✅ Seguro                              |
| **Uso recomendado** | Solo para credenciales independientes | Para credenciales asociadas a usuarios |

---

### HU-011: Actualizar Credencial por Usuario

**Como** operador del sistema  
**Quiero** actualizar la credencial asociada a un usuario específico  
**Para** modificar su contraseña o configuración sin afectar otras credenciales

#### Criterios de Aceptación

```gherkin
Escenario: Actualizar credencial de usuario
  Dado que el usuario ID 1 tiene una credencial asociada
  Cuando el operador actualiza la credencial por usuario ID 1
  Y establece un nuevo hash o cambia el valor de requiere_reset
  Entonces el sistema actualiza la credencial correspondiente
  Y muestra "Credencial actualizada exitosamente."

Escenario: Usuario sin credencial asociada
  Dado que el usuario ID 1 no tiene credencial
  Cuando el operador intenta actualizar su credencial
  Entonces el sistema muestra "El usuario no tiene credencial asociada."
```

#### Reglas de Negocio Aplicables

- **RN-039U**: Solo se actualiza la credencial asociada al usuario especificado.
- **RN-040U**: Si varias entidades comparten credencial (caso no previsto), todas se afectan.
- **RN-041U**:Se requiere que el usuario tenga una credencial asociada para realizar la actualización.
- **RN-042U**: Campos vacíos mantienen los valores actuales.

#### Implementación Técnica

- **Clase**: `MenuHandler.actualizarCredencialPorUsuario()`
- **Servicio**: `CredencialAccesoServiceImpl.actualizar()`
- **Flujo**:
  1. Obtiene usuario por ID
  2. Valida que tenga credencial (`usuario.getCredencial() != null`)
  3. Captura nuevos valores con trim()
  4. Actualiza objeto CredencialAcceso
  5. Llama a `CredencialAccesoServiceImpl.actualizar()`

---

## Reglas de Negocio

### Validación de Datos (RN-001U a RN-013U)

| Código  | Regla                                        | Implementación                                                 |
| ------- | -------------------------------------------- | -------------------------------------------------------------- |
| RN-001U | `username` y `email` son obligatorios        | `UsuarioServiceImpl.validateUsuario()`                         |
| RN-002U | `username` y `email` deben ser únicos        | `UsuarioServiceImpl.validateUnicidad()` + DB UNIQUE constraint |
| RN-003U | Espacios iniciales/finales se eliminan       | `.trim()` en `MenuHandler`                                     |
| RN-004U | IDs se generan automáticamente               | `AUTO_INCREMENT` en BD                                         |
| RN-005U | `credencial_id` es opcional al crear usuario | FK nullable (`credencial_id INT NULL`)                         |
| RN-009U | Búsquedas son case-insensitive por collation | MySQL (utf8mb4_general_ci)                                     |
| RN-010U | Búsqueda exacta por username o email         | UsuarioDAO.findByUsername(...) / findByEmail(...)`             |
| RN-013U | No se permiten búsquedas vacías              | Validación en `UsuarioServiceImpl`                             |

---

### Operaciones de Base de Datos (RN-014U a RN-027U)

| Código  | Regla                                  | Implementación                                 |
| ------- | -------------------------------------- | ---------------------------------------------- |
| RN-014U | Validación antes de persistir          | Service layer valida antes de llamar DAO       |
| RN-015U | Campos vacíos mantienen valor original | Pattern `if (!x.isEmpty())`                    |
| RN-016U | ID > 0 requerido para update/delete    | Service valida `id > 0`                        |
| RN-019U | Eliminación es lógica                  | `UPDATE tabla SET eliminado = TRUE`            |
| RN-020U | Soft delete en lugar de DELETE físico  | Todos los DAOs usan `UPDATE`                   |
| RN-022U | Verificación de `rowsAffected`         | Todos los `UPDATE/DELETE` la aplican           |
| RN-026U | Solo listar no eliminados              | `WHERE eliminado = FALSE` en todas las queries |

---

### Integridad Referencial (RN-028U a RN-041U)

| Código  | Regla                                                      | Implementación                                               |
| ------- | ---------------------------------------------------------- | ------------------------------------------------------------ |
| RN-028U | HU-008 no verifica referencias                             | `CredencialAccesoDAO.eliminar()` sin validar asociación      |
| RN-029U | Puede dejar referencias inconsistentes                     | Usuario puede mantener `credencial_id` activo                |
| RN-030U | HU-010 es alternativa segura                               | `UsuarioServiceImpl.eliminarCredencialDeUsuario()`           |
| RN-035U | Actualizar FK antes de eliminar credencial                 | Orden: `usuario.setCredencial(null)` → actualizar → eliminar |
| RN-036U | Validar pertenencia usuario↔credencial                     | Verifica `usuario.getCredencial().getId() == credencialId`   |
| RN-037U | Operación en dos pasos                                     | `UPDATE usuarios` → `UPDATE credenciales_acceso`             |
| RN-040U | Si varias entidades comparten credencial, todas se afectan | FK 1→1 controlado con constraint UNIQUE                      |

---

### Transacciones y Coordinación (RN-042U a RN-051U)

| Código  | Regla                                                           | Implementación                                        |
| ------- | --------------------------------------------------------------- | ----------------------------------------------------- |
| RN-042U | `UsuarioServiceImpl` coordina con `CredencialAccesoServiceImpl` | Lógica de alta y baja conjunta                        |
| RN-043U | Insertar credencial antes que usuario (si aplica)               | `UsuarioServiceImpl.insertar()` líneas 30–38          |
| RN-044U | Try-with-resources para manejo de recursos                      | Todas las conexiones, statements y resultsets         |
| RN-045U | `PreparedStatements` para prevenir SQL injection                | 100% de queries                                       |
| RN-046U | LEFT JOIN para relación opcional usuario–credencial             | Consultas de `UsuarioDAO`                             |
| RN-047U | `NULL` seguro en FK                                             | `setCredencialId()` usa `stmt.setNull(Types.INTEGER)` |
| RN-048U | `TransactionManager` soporta rollback automático                | Implementa `AutoCloseable` con rollback en `close()`  |
| RN-049U | `equals()` / `hashCode()` de Usuario basados en `username`      | `username` es único                                   |
| RN-050U | `equals()` / `hashCode()` de CredencialAcceso basados en `id`   | Comparación por identidad                             |
| RN-051U | `Scanner` se cierra al salir del programa                       | `AppMenu.run()` línea 37                              |

---

## Modelo de Datos

### Diagrama Entidad-Relación

```
┌───────────────────────────────────────┐
│              usuarios                 │
├───────────────────────────────────────┤
│ id: INT PK AUTO_INCREMENT             │
│ username: VARCHAR(30) NOT NULL UNIQUE │
│ email: VARCHAR(120) NOT NULL UNIQUE   │
│ activo: BOOLEAN DEFAULT TRUE          │
│ fecha_registro: DATETIME DEFAULT NOW()│
│ credencial_id: INT FK NULL            │
│ eliminado: BOOLEAN DEFAULT FALSE      │
└──────────────┬────────────────────────┘
               │ 0..1
               │
               │ FK
               │
               ▼
┌───────────────────────────────────────┐
│         credenciales_acceso           │
├───────────────────────────────────────┤
│ id: INT PK AUTO_INCREMENT             │
│ hash_password: VARCHAR(255) NOT NULL  │
│ salt: VARCHAR(64) NULL                │
│ requiere_reset: BOOLEAN DEFAULT FALSE │
│ ultimo_cambio: DATETIME DEFAULT NOW() │
│ eliminado: BOOLEAN DEFAULT FALSE      │
└───────────────────────────────────────┘
```

### Constraints y Validaciones

```sql
-- Unicidad en username y email
ALTER TABLE usuarios ADD CONSTRAINT uq_usuarios_username UNIQUE (username);
ALTER TABLE usuarios ADD CONSTRAINT uq_usuarios_email UNIQUE (email);

-- Relación 1→1 opcional con credenciales
ALTER TABLE usuarios ADD CONSTRAINT fk_usuarios_credencial
  FOREIGN KEY (credencial_id) REFERENCES credenciales_acceso(id)
  ON UPDATE CASCADE
  ON DELETE SET NULL;

-- Garantiza relación 1→1 (una credencial por usuario)
ALTER TABLE usuarios ADD CONSTRAINT uq_usuarios_credencial UNIQUE (credencial_id);

-- Índices recomendados para performance
CREATE INDEX idx_usuario_activo ON usuarios(activo);
CREATE INDEX idx_usuario_eliminado ON usuarios(eliminado);
CREATE INDEX idx_credencial_eliminado ON credenciales_acceso(eliminado);
CREATE INDEX idx_credencial_reset ON credenciales_acceso(requiere_reset);
```

### Queries Principales

#### SELECT con JOIN

```sql
SELECT u.id, u.username, u.email, u.activo, u.fecha_registro,
       u.credencial_id, c.id AS cred_id, c.requiere_reset, c.ultimo_cambio
FROM usuarios u
LEFT JOIN credenciales_acceso c ON u.credencial_id = c.id AND c.eliminado = FALSE
WHERE u.eliminado = FALSE;
```

#### Búsqueda por username o email

```sql
SELECT u.id, u.username, u.email, u.activo, u.fecha_registro,
       u.credencial_id, c.id AS cred_id, c.requiere_reset, c.ultimo_cambio
FROM usuarios u
LEFT JOIN credenciales_acceso c ON u.credencial_id = c.id
WHERE u.eliminado = FALSE
  AND (u.username = ? OR u.email = ?);
```

#### Búsqueda por ID de credencial

```sql
SELECT u.id, u.username, u.email, u.activo, u.fecha_registro,
       u.credencial_id, c.hash_password, c.salt, c.requiere_reset
FROM usuarios u
LEFT JOIN credenciales_acceso c ON u.credencial_id = c.id
WHERE u.eliminado = FALSE AND u.credencial_id = ?;
```

#### Listado de credenciales activas

```sql
SELECT id, requiere_reset, ultimo_cambio
FROM credenciales_acceso
WHERE eliminado = FALSE
ORDER BY id ASC;
```

---

## Flujos Técnicos Críticos

### Crear Usuario con Credencial

```
Operador (MenuHandler)
↓ captura datos con .trim()
    ↓ UsuarioServiceImpl.insertar()
        ↓ validateUsuario()
        ↓ validateUnicidad(username, email)
        ↓ si credencial ≠ null y credencial.id == 0
            ↓ CredencialAccesoServiceImpl.insertar()
                ↓ validateCredencial()
                ↓ CredencialAccesoDAO.insertar()
                    ↓ INSERT INTO credenciales_acceso (...)
                    ↓ obtiene ID autogenerado
                    ↓ credencial.setId(generatedId)
                ↓ return
        ↓ UsuarioServiceImpl continúa
            ↓ UsuarioDAO.insertar(usuario)
                ↓ INSERT INTO usuarios (con credencial_id)
                ↓ obtiene ID autogenerado
                ↓ usuario.setId(generatedId)
            ↓ return
↓ Operador recibe: "Usuario y credencial creados exitosamente con ID: X"
```

### Flujo 2: Eliminar Credencial de Usuario (Operación Segura - HU-010U)

```
Operador (MenuHandler)
↓ ingresa usuarioId
    ↓ UsuarioServiceImpl.eliminarCredencialDeUsuario(usuarioId, credencialId)
        ↓ valida usuarioId > 0 && credencialId > 0
        ↓ usuario = usuarioDAO.getById(usuarioId)
        ↓ si usuario == null → "Usuario no encontrado"
        ↓ si usuario.getCredencial() == null → "El usuario no tiene credencial asociada"
        ↓ si usuario.getCredencial().getId() != credencialId → "La credencial no pertenece a este usuario"
        ↓ usuario.setCredencial(null)
        ↓ usuarioDAO.actualizar(usuario)
            ↓ UPDATE usuarios SET credencial_id = NULL WHERE id = usuarioId
        ↓ credencialServiceImpl.eliminar(credencialId)
            ↓ UPDATE credenciales_acceso SET eliminado = TRUE WHERE id = credencialId
        ↓ return
↓ Operador recibe: "Credencial eliminada exitosamente y referencia actualizada."
```

### Flujo 3: Validación de Username/Email Únicos en Update

```
Operador actualiza usuario
↓ UsuarioServiceImpl.actualizar(usuario)
    ↓ validateUsuario(usuario)
    ↓ validateUnicidad(username, email, usuario.getId())
        ↓ existente = usuarioDAO.buscarPorUsernameOEmail(username, email)
        ↓ si existente ≠ null
            ↓ si usuarioId == null || existente.getId() ≠ usuarioId
                → ✗ "Ya existe un usuario con ese username o email"
            ↓ else
                → ✓ es el mismo usuario (OK)
        ↓ else
            → ✓ username y email disponibles
    ↓ usuarioDAO.actualizar(usuario)
↓ return
```

---

## Resumen de Operaciones del Menú

| Opción | Operación                         | Handler                            | HU             |
| ------ | --------------------------------- | ---------------------------------- | -------------- |
| 1      | Crear usuario                     | `crearUsuario()`                   | HU-001         |
| 2      | Listar usuarios                   | `listarUsuarios()`                 | HU-002, HU-003 |
| 3      | Actualizar usuario                | `actualizarUsuario()`              | HU-004         |
| 4      | Eliminar usuario                  | `eliminarUsuario()`                | HU-005         |
| 5      | Crear credencial                  | `crearCredencialIndependiente()`   | HU-006         |
| 6      | Listar credenciales               | `listarCredenciales()`             | HU-007         |
| 7      | Actualizar credencial por ID      | `actualizarCredencialPorId()`      | HU-009         |
| 8      | Eliminar credencial por ID        | `eliminarCredencialPorId()`        | HU-008 ⚠️      |
| 9      | Actualizar credencial por usuario | `actualizarCredencialPorUsuario()` | HU-011         |
| 10     | Eliminar credencial por usuario   | `eliminarCredencialPorUsuario()`   | HU-010 ✅      |
| 0      | Salir                             | Sets `running = false`             | -              |

---

## Documentación Relacionada

- **README.md**: Guía de instalación, configuración y uso
- **CLAUDE.md**: Documentación técnica para desarrollo, arquitectura detallada, patrones de código

---

**Versión**: 1.0
**Total Historias de Usuario**: 11
**Total Reglas de Negocio**: 51
**Arquitectura**: 4 capas (Main → Service → DAO → Models)
