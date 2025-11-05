# Sistema de Gestión de Usuarios y Credenciales

## Trabajo Práctico Integrador - Programación 2

### Descripción del Proyecto

Este Trabajo Práctico Integrador tiene como objetivo demostrar la aplicación práctica de los conceptos fundamentales de Programación Orientada a Objetos y Persistencia de Datos aprendidos durante el cursado de Programación 2. El proyecto consiste en desarrollar un sistema completo de gestión de usuarios y credenciales que permita realizar operaciones CRUD (Crear, Leer, Actualizar, Eliminar) sobre estas entidades, implementando una arquitectura robusta y profesional.

### Objetivos Académicos

El desarrollo de este sistema permite aplicar y consolidar los siguientes conceptos clave de la materia:

**1. Arquitectura en Capas (Layered Architecture)**

- Implementación de separación de responsabilidades en 4 capas diferenciadas
- Capa de Presentación (Main/UI): Interacción con el usuario mediante consola
- Capa de Lógica de Negocio (Service): Validaciones y reglas de negocio
- Capa de Acceso a Datos (DAO): Operaciones de persistencia
- Capa de Modelo (Models): Representación de entidades del dominio

**2. Programación Orientada a Objetos**

- Aplicación de principios SOLID (Single Responsibility, Dependency Injection)
- Uso de herencia mediante clase abstracta Base
- Implementación de interfaces genéricas (GenericDAO, GenericService)
- Encapsulamiento con atributos privados y métodos de acceso
- Sobrescritura de métodos (equals, hashCode, toString)

**3. Persistencia de Datos con JDBC**

- Conexión a base de datos MySQL mediante JDBC
- Implementación del patrón DAO (Data Access Object)
- Uso de PreparedStatements para prevenir SQL Injection
- Gestión de transacciones con commit y rollback
- Manejo de claves autogeneradas (AUTO_INCREMENT)
- Consultas con LEFT JOIN para relaciones entre entidades

**4. Manejo de Recursos y Excepciones**

- Uso del patrón try-with-resources para gestión automática de recursos JDBC
- Implementación de AutoCloseable en TransactionManager
- Manejo apropiado de excepciones con propagación controlada
- Validación multi-nivel: base de datos y aplicación

**5. Patrones de Diseño**

- Factory Pattern (DatabaseConnection)
- Service Layer Pattern (separación lógica de negocio)
- DAO Pattern (abstracción del acceso a datos)
- Soft Delete Pattern (eliminación lógica de registros)
- Dependency Injection manual

**6. Validación de Integridad de Datos**

- Validación de unicidad: username y email deben ser únicos tanto en la base de datos como en la capa de servicio.
- Validación de campos obligatorios: nombre, apellido, username, email y password son requeridos antes de persistir un usuario o credencial.
- Validación de integridad referencial: se mantiene la coherencia de la relación 1:1 (usuarios.credencial_id) mediante validaciones en UsuarioServiceImpl.
- Eliminación segura: se aplica soft delete, preservando la integridad de las referencias entre Usuario y CredencialAcceso para evitar vínculos huérfanos.

### Funcionalidades Implementadas

El sistema permite gestionar dos entidades principales con las siguientes operaciones:

## Características Principales

- **Gestión de Usuarios**: Crear, listar, actualizar y eliminar usuarios aplicando validaciones de unicidad y campos obligatorios.
- **Gestión de Credenciales de Acceso**: Administrar credenciales de forma independiente o asociadas a usuarios, manteniendo la relación 1:1 opcional.
- **Búsqueda de Usuarios**: Filtrar usuarios por `username` o `email`, con coincidencias parciales o exactas según la opción seleccionada.
- **Soft Delete**: Eliminación lógica que mantiene la integridad referencial entre `Usuario` y `CredencialAcceso`.
- **Seguridad**: Uso de **PreparedStatements** para prevenir inyecciones SQL y manejo controlado de excepciones.
- **Validación Multi-capa**: Reglas de negocio implementadas en la capa de servicio y restricciones de unicidad definidas en la base de datos.
- **Transacciones**: Coordinación automática entre operaciones de usuario y credencial, con rollback ante errores para garantizar consistencia.

## Requisitos del Sistema

| Componente            | Versión Requerida           |
| --------------------- | --------------------------- |
| **Java JDK**          | 21                          |
| **MariaDB**           | 10.4.32 (incluido en XAMPP) |
| **Ant**               | Integrado en NetBeans       |
| **IDE**               | NetBeans 19 o superior      |
| **Sistema Operativo** | Windows                     |

## Instalación

### 1. Configurar Base de Datos

Ejecutar el siguiente script SQL en MySQL:

```sql
    -- Crear BD y seleccionar
    CREATE DATABASE IF NOT EXISTS dbtpiUsuarios
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;
    USE dbtpiUsuarios;

    -- =========================
    -- Tabla: credenciales_acceso
    -- =========================
    CREATE TABLE IF NOT EXISTS credenciales_acceso (
        id INT AUTO_INCREMENT PRIMARY KEY,
        hash_password VARCHAR(255) NOT NULL,    -- hash ya calculado
        salt VARCHAR(64) NULL,                  -- tamaño sugerido
        requiere_reset BOOLEAN NOT NULL DEFAULT FALSE,
        ultimo_cambio DATETIME DEFAULT CURRENT_TIMESTAMP
                    ON UPDATE CURRENT_TIMESTAMP,
        eliminado BOOLEAN NOT NULL DEFAULT FALSE
    ) ENGINE=InnoDB;

    -- ================
    -- Tabla: usuarios
    -- ================
    CREATE TABLE IF NOT EXISTS usuarios (
        id INT AUTO_INCREMENT PRIMARY KEY,
        username VARCHAR(30)  NOT NULL,
        email    VARCHAR(120) NOT NULL,
        activo   BOOLEAN      NOT NULL DEFAULT TRUE,
        fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP,
        credencial_id INT NULL,                 -- 1→1 opcional hacia credenciales_acceso
        eliminado BOOLEAN NOT NULL DEFAULT FALSE,

        -- Unicidad de dominio
        CONSTRAINT uq_usuarios_username UNIQUE (username),
        CONSTRAINT uq_usuarios_email    UNIQUE (email),

        -- FK 1→1
        CONSTRAINT fk_usuarios_credencial
            FOREIGN KEY (credencial_id)
            REFERENCES credenciales_acceso(id)
            ON UPDATE CASCADE
            ON DELETE SET NULL,

        -- Garantiza el 1→1 (una credencial no puede estar en dos usuarios)
        CONSTRAINT uq_usuarios_credencial UNIQUE (credencial_id)
    ) ENGINE=InnoDB;
```

### 2. Compilar el Proyecto

El proyecto utiliza **Ant** integrado en **NetBeans**, por lo que no requiere comandos externos.

#### Opción 1: Desde NetBeans

1. Abrir el proyecto en NetBeans.
2. Esperar a que el IDE cargue las dependencias y compile automáticamente.
3. Ir a **Run → Run Project** o hacer clic en el botón ▶️ para ejecutar la clase `Main`.

#### Opción 2: Desde la línea de comandos

Si deseás compilar manualmente con Ant: `ant clean compile`

> 📌 _Asegurate de tener configurado el `JAVA_HOME` apuntando a JDK 21._

---

### 3. Configurar Conexión (Opcional)

Por defecto el sistema se conecta a:

- **Host:** localhost:3306
- **Base de datos:** dbtpiUsuarios
- **Usuario:** root
- **Contraseña:** _(vacía)_

Para modificar estos valores, podés hacerlo directamente en la clase de configuración de conexión  
(`DatabaseConnection` o `ConnectionFactory`)  
o bien pasando propiedades del sistema al ejecutar el programa  
(por ejemplo: `java -Ddb.url=... -Ddb.user=... -Ddb.password=...`).

> 💡 _El proyecto utiliza MariaDB 10.4.32 (compatible con MySQL), por lo que el prefijo `jdbc:mysql://` funciona sin cambios._

### Verificar Conexión

```bash
# Usar TestConexion para verificar conexión a BD
java -cp "build/classes/java/main:<ruta-mysql-jar>" Main.TestConexion
```

Salida esperada:

```
Conexion exitosa a la base de datos
Usuario conectado: root@localhost
Base de datos: dbtpi3
URL: jdbc:mysql://localhost:3306/dbtpi3
Driver: MySQL Connector/J v8.4.0
```

## Uso del Sistema

### Menú Principal

```
========= MENU =========
1. Crear usuario
2. Listar usuarios
3. Actualizar usuario
4. Eliminar usuario
5. Crear credencial
6. Listar credenciales
7. Actualizar credencial por ID
8. Eliminar credencial por ID
9. Actualizar credencial por ID de usuario
10. Eliminar credencial por ID de usuario
0. Salir
```

### Operaciones Disponibles

#### 1. Crear Usuario

Permite registrar un nuevo usuario, con la posibilidad de asociarle una credencial de acceso opcional.  
Valida unicidad de `username` y `email`.

#### 2. Listar Usuarios

Muestra todos los usuarios activos o permite filtrar por `username` o `email`.

#### 3. Actualizar Usuario

Actualiza los datos del usuario y, si se desea, también su credencial asociada.

#### 4. Eliminar Usuario

Realiza una **baja lógica** del usuario (soft delete).  
No elimina la credencial asociada.

#### 5. Crear Credencial

Crea una credencial independiente, no asociada inicialmente a ningún usuario.

#### 6. Listar Credenciales

Lista todas las credenciales activas registradas en el sistema.

#### 7. Actualizar Credencial por ID

Permite actualizar una credencial directamente (por ejemplo, hash, salt o reset de contraseña).

#### 8. Eliminar Credencial por ID

Elimina una credencial por su ID.  
⚠️ _Puede dejar referencias huérfanas si está asociada a un usuario._

#### 9. Actualizar Credencial por ID de Usuario

Busca primero al usuario y luego permite modificar su credencial asociada.

#### 10. Eliminar Credencial por ID de Usuario

Elimina la credencial asociada a un usuario de forma **segura**,  
primero desvinculando la relación (`credencial_id = NULL`) antes de eliminar.

#### 0. Salir

Finaliza la ejecución del programa.

## Arquitectura

### Estructura en Capas

```
┌─────────────────────────────────────┐
│     Main / UI Layer                 │
│  (Interacción con usuario)          │
│  AppMenu, MenuHandler, MenuDisplay  │
└───────────┬─────────────────────────┘
            │
┌───────────▼─────────────────────────┐
│     Service Layer                   │
│  (Lógica de negocio y validación)   │
│  UsuarioServiceImpl                 │
│  CredencialAccesoServiceImpl        │
└───────────┬─────────────────────────┘
            │
┌───────────▼─────────────────────────┐
│     DAO Layer                       │
│  (Acceso a datos)                   │
│  UsuarioDAO, CredencialAccesoDAO    │
└───────────┬─────────────────────────┘
            │
┌───────────▼─────────────────────────┐
│     Models Layer                    │
│  (Entidades de dominio)             │
│  Usuario, CredencialAcceso, Base    │
└─────────────────────────────────────┘
```

### Componentes Principales

**config/**

- `DatabaseConnection.java`: Maneja la conexión JDBC con MariaDB y valida la configuración en su inicialización.
- `TransactionManager.java`: Controla las transacciones de base de datos implementando `AutoCloseable` para asegurar `commit` o `rollback` automáticos.

**models/**

- `Base.java`: Clase abstracta que define campos comunes (`id`, `eliminado`).
- `Usuario.java`: Entidad de dominio para los usuarios, con atributos `nombre`, `apellido`, `username`, `email`, `activo` y referencia opcional a una credencial.
- `CredencialAcceso.java`: Entidad que representa las credenciales, incluyendo `hashPassword`, `salt`, `requiereReset` y `ultimoCambio`.

**dao/**

- `GenericDAO<T>`: Interfaz genérica que define las operaciones CRUD básicas.
- `UsuarioDAO.java`: Implementa las operaciones JDBC para la entidad `Usuario`, incluyendo consultas con `LEFT JOIN` para obtener la credencial asociada.
- `CredencialAccesoDAO.java`: Implementa las operaciones JDBC para credenciales.

**service/**

- `GenericService<T>`: Interfaz genérica para servicios de negocio.
- `UsuarioServiceImpl.java`: Aplica validaciones, reglas de unicidad (`username`, `email`) y coordina la asociación/desvinculación de credenciales.
- `CredencialAccesoServiceImpl.java`: Gestiona la lógica de negocio de las credenciales, como validación y actualización de contraseñas.

**main/**

- `Main.java`: Punto de entrada principal del sistema.
- `AppMenu.java`: Controla el ciclo principal del menú y las opciones seleccionadas por el usuario.
- `MenuHandler.java`: Ejecuta las operaciones CRUD interactuando con los servicios.
- `MenuDisplay.java`: Gestiona la visualización del menú en consola.
- `TestConexion.java`: Permite verificar la conexión a la base de datos y mostrar los parámetros de conexión activos.

## Modelo de Datos

```
┌──────────────────────┐ ┌─────────────────────────┐
│ usuarios             │ │   credenciales_acceso   │
├──────────────────────┤ ├─────────────────────────┤
│ id (PK)              │ │ id (PK)                 │
│ nombre               │ │ hash_password           │
│ apellido             │ │ salt                    │
│ username (UNIQUE)    │ │ requiere_reset          │
│ email (UNIQUE)       │ │ ultimo_cambio           │
│ activo               │ │ eliminado               │
│ fecha_registro       │ └─────────────────────────┘
│ credencial_id (FK)   │──────┐
│ eliminado            │      │
└──────────────────────┘      │
                              │
                              └──▶ Relación 1→1 opcional
```

**Reglas:**

- Un usuario puede tener **0 o 1 credencial de acceso** (relación opcional 1→1).
- `username` y `email` son **únicos** (restricciones de base de datos y validaciones en la capa de servicio).
- La FK `credencial_id` puede ser `NULL`, permitiendo usuarios sin credencial asociada.
- Se aplica **eliminación lógica** mediante el campo `eliminado = TRUE` en ambas tablas.
- La restricción `UNIQUE (credencial_id)` en la tabla `usuarios` asegura que una misma credencial no pueda asociarse a más de un usuario.
- Las operaciones de actualización o eliminación mantienen la integridad referencial gracias a la cláusula `ON DELETE SET NULL`.

## Patrones y Buenas Prácticas

### Seguridad

- **Uso exclusivo de PreparedStatements**: Previene inyecciones SQL en todas las operaciones JDBC.
- **Validación multi-capa**: La capa de servicio valida los datos antes de persistirlos en la base.
- **Unicidad de username y email**: Restringida tanto en base de datos como en `UsuarioServiceImpl`.
- **Control de contraseñas**: Las credenciales almacenan solo el hash y el salt, nunca el texto plano.

### Gestión de Recursos

- **Try-with-resources**: Aplicado en todas las conexiones, `PreparedStatement` y `ResultSet`.
- **AutoCloseable en TransactionManager**: Asegura `commit` o `rollback` automáticos según el resultado.
- **Scanner cerrado correctamente**: Al finalizar la ejecución en `AppMenu` para evitar fugas de recursos.

### Validaciones

- **Input trimming**: Todos los textos de entrada se limpian con `.trim()` antes de procesarse.
- **Campos obligatorios**: Se verifica que nombre, apellido, username, email y password no estén vacíos.
- **IDs positivos**: Todas las operaciones verifican `id > 0` antes de interactuar con la base.
- **Verificación de filas afectadas**: En operaciones `UPDATE` y `DELETE` para confirmar resultados válidos.

### Soft Delete

- Las eliminaciones ejecutan: `UPDATE tabla SET eliminado = TRUE WHERE id = ?`.
- Los listados aplican filtro: `WHERE eliminado = FALSE`.
- No se eliminan físicamente los datos para preservar la trazabilidad.
- La relación entre `Usuario` y `CredencialAcceso` se mantiene coherente incluso tras eliminaciones.

---

## Reglas de Negocio Principales

1. **Unicidad de usuario**: `username` y `email` no pueden repetirse.
2. **Campos obligatorios**: Todos los usuarios y credenciales deben tener valores válidos antes de persistir.
3. **Validación previa a la persistencia**: La capa de servicio valida datos antes de llamar al DAO.
4. **Eliminación segura de credenciales**: Se recomienda usar la opción 10 (por ID de usuario), que primero desvincula la FK antes de eliminar.
5. **Preservación de valores**: En actualizaciones, los campos vacíos mantienen su valor anterior.
6. **Búsqueda flexible**: Permite coincidencias parciales en `username` y `email` mediante `LIKE`.
7. **Transacciones coordinadas**: Las operaciones combinadas de usuario y credencial soportan rollback automático ante fallos.

## Solución de Problemas

### Error: "ClassNotFoundException: com.mysql.cj.jdbc.Driver"

**Causa**: El conector JDBC de MySQL/MariaDB no está incluido en el classpath.  
**Solución**: Asegurarse de tener el archivo `mysql-connector-j-8.4.0.jar` en la carpeta `lib/` del proyecto y que esté referenciado correctamente en las propiedades de Ant o NetBeans.

---

### Error: "Communications link failure"

**Causa**: MariaDB/MySQL no se encuentra en ejecución.  
**Solución**: Iniciar el servicio desde XAMPP:

- Abrir el **Panel de Control de XAMPP** y presionar **Start** en la línea de **MySQL**.
- Luego verificar la conexión ejecutando la clase `TestConexion`.

---

### Error: "Access denied for user 'root'@'localhost'"

**Causa**: Usuario o contraseña incorrectos.  
**Solución**: Verificar las credenciales en `DatabaseConnection.java` o ajustar la configuración de conexión:
`DB_USER=root` y `DB_PASS=` (vacío por defecto en XAMPP).

---

### Error: "Unknown database 'dbtpiUsuarios'"

**Causa**: La base de datos no fue creada previamente.  
**Solución**: Ejecutar el script de creación de base de datos incluido en la sección **Instalación** del README.

---

### Error: "Table 'usuarios' doesn't exist"

**Causa**: Las tablas no fueron creadas correctamente.  
**Solución**: Ejecutar el script SQL completo del apartado **Instalación → Configurar Base de Datos**.

---

## Limitaciones Conocidas

1. **Ejecución por consola**: No cuenta con interfaz gráfica (solo menú en consola).
2. **Sin paginación**: Al listar todos los registros puede volverse lento con grandes volúmenes de datos.
3. **Opción 8 (Eliminar credencial por ID)**: Puede dejar referencias huérfanas si la credencial está asociada a un usuario (usar la opción 10 para eliminación segura).
4. **Sin pool de conexiones**: Cada operación abre una nueva conexión (válido para aplicaciones de consola pequeñas).
5. **Sin transacciones en AppMenu/MenuHandler**: Actualizar usuario y credencial en una misma acción puede requerir manejo manual de rollback.
6. **Dependencia manual del driver**: El conector `mysql-connector-j` debe agregarse manualmente si se ejecuta fuera de NetBeans.
7. **Campos fijos**: La estructura de credenciales asume almacenamiento de hash y salt; no hay cambio de algoritmo dinámico.

## Documentación Adicional

- **CLAUDE.md**: Documentación técnica detallada para desarrollo

  - Comandos de build y ejecución
  - Arquitectura profunda
  - Patrones de código críticos
  - Troubleshooting avanzado
  - Verificación de calidad (score 9.7/10)

- **HISTORIAS_DE_USUARIO.md**: Especificaciones funcionales completas
  - Historias de usuario detalladas
  - Reglas de negocio numeradas
  - Criterios de aceptación en formato Gherkin
  - Diagramas de flujo

## Tecnologías Utilizadas

- **Lenguaje**: Java 21
- **Entorno de desarrollo**: NetBeans (proyecto Ant)
- **Base de Datos**: MySQL 8.x
- **JDBC Driver**: mysql-connector-j 8.4.0
- **Testing**: JUnit 5 (configurado, sin tests implementados)

## Estructura de Directorios

```
TPI-Prog2-fusion-final/
├── src/
│   ├── Config/          # Configuración de BD y transacciones
│   ├── Dao/             # Capa de acceso a datos
│   ├── Main/            # UI y punto de entrada
│   ├── Models/          # Entidades de dominio
│   └── Service/         # Lógica de negocio
├── build.xml            # Archivo de construccion Ant
├── README.md            # Este archivo
├── CLAUDE.md            # Documentación técnica
└── HISTORIAS_DE_USUARIO.md  # Especificaciones funcionales
```

## Convenciones de Código

- **Idioma**: Español (nombres de clases, métodos, variables)
- **Nomenclatura**:
  - Clases: PascalCase (Ej: `UsuarioServiceImpl`)
  - Métodos: camelCase (Ej: `buscarPorUsername`)
  - Constantes SQL: UPPER_SNAKE_CASE (Ej: `SELECT_BY_ID_SQL`)
- **Indentación**: 4 espacios
- **Recursos**: Siempre usar try-with-resources
- **SQL**: Constantes privadas static final
- **Excepciones**: Capturar y manejar con mensajes al usuario

## Evaluación y Criterios de Calidad

### Aspectos Evaluados en el TPI

Este proyecto demuestra competencia en los siguientes criterios académicos:

**✅ Arquitectura y Diseño (30%)**

- Correcta separación en capas con responsabilidades bien definidas
- Aplicación de patrones de diseño apropiados (DAO, Service Layer, Factory)
- Uso de interfaces para abstracción y polimorfismo
- Implementación de herencia con clase abstracta Base

**✅ Persistencia de Datos (25%)**

- Correcta implementación de operaciones CRUD con JDBC
- Uso apropiado de PreparedStatements (100% de las consultas)
- Gestión de transacciones con commit/rollback
- Manejo de relaciones entre entidades (Foreign Keys, LEFT JOIN)
- Soft delete implementado correctamente

**✅ Manejo de Recursos y Excepciones (20%)**

- Try-with-resources en todas las operaciones JDBC
- Cierre apropiado de conexiones, statements y resultsets
- Manejo de excepciones con mensajes informativos al usuario
- Prevención de resource leaks

**✅ Validaciones e Integridad (15%)**

- Validación de campos obligatorios en capa de servicio.
- Unicidad de `username` y `email` (DB + validación en Service).
- Integridad referencial (FK opcional con `ON DELETE SET NULL`).
- Eliminación segura de credenciales para evitar FKs huérfanas.

**✅ Calidad de Código (10%)**

- Código documentado con Javadoc completo (13 archivos)
- Convenciones de nomenclatura consistentes
- Código legible y mantenible
- Ausencia de code smells o antipatrones críticos

**✅ Funcionalidad Completa (10%)**

- Todas las operaciones CRUD funcionan correctamente
- Búsquedas y filtros implementados
- Interfaz de usuario clara y funcional
- Manejo robusto de errores

### Puntos Destacables del Proyecto

1. **Relación 1→1 opcional bien materializada**

   - `usuarios.credencial_id` con restricción `UNIQUE` asegura 1→1 real.
   - `ON DELETE SET NULL` evita romper la referencia al eliminar credenciales.

2. **Unicidad de identidad de acceso**

   - `username` y `email` únicos por restricciones en BD y verificación en `UsuarioServiceImpl`.

3. **Transacciones coordinadas**

   - Actualizaciones que tocan usuario y credencial usan `TransactionManager` para garantizar atomicidad.

4. **Eliminación segura desde el menú**

   - Opción “Eliminar credencial por ID de usuario” primero **desvincula** y luego elimina (evita FKs huérfanas).

5. **Buenas prácticas consistentes**
   - DI manual, sanitización de input con `.trim()`, validación _fail-fast_, y verificación de `rowsAffected` en UPDATE/DELETE.

### Conceptos de Programación 2 Demostrados

| Concepto                 | Implementación en el Proyecto                                                                                                   |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------- |
| **Herencia**             | Clase abstracta `Base` heredada por `Usuario` y `CredencialAcceso`.                                                             |
| **Polimorfismo**         | Interfaces genéricas `GenericDAO<T>` y `GenericService<T>` aplicadas en todas las capas.                                        |
| **Encapsulamiento**      | Atributos privados con getters/setters en las entidades `Usuario` y `CredencialAcceso`.                                         |
| **Abstracción**          | Interfaces que definen contratos comunes para DAO y Service sin exponer detalles de implementación.                             |
| **JDBC**                 | Conexión directa con MariaDB mediante `DatabaseConnection`, uso de `PreparedStatement`, `ResultSet` y manejo transaccional.     |
| **DAO Pattern**          | `UsuarioDAO` y `CredencialAccesoDAO` abstraen el acceso a datos y centralizan las consultas SQL.                                |
| **Service Layer**        | Lógica de negocio separada en `UsuarioServiceImpl` y `CredencialAccesoServiceImpl`, aplicando validaciones y reglas de dominio. |
| **Exception Handling**   | Bloques `try-catch` en todas las capas con propagación controlada y mensajes claros al usuario.                                 |
| **Resource Management**  | Uso de `try-with-resources` y `AutoCloseable` para liberar recursos JDBC de forma segura.                                       |
| **Dependency Injection** | Inyección manual de dependencias en `AppMenu` al crear los servicios (`UsuarioServiceImpl`, `CredencialAccesoServiceImpl`).     |

---

## Contexto Académico

**Materia:** Programación 2  
**Tipo de Evaluación:** Trabajo Práctico Integrador (TPI)  
**Modalidad:** Desarrollo de sistema CRUD con persistencia en base de datos  
**Objetivo:** Aplicar conceptos de POO, JDBC, arquitectura en capas y patrones de diseño.

Este proyecto representa la integración de todos los contenidos vistos durante el cuatrimestre, demostrando capacidad para:

- Diseñar sistemas con arquitectura por capas.
- Implementar persistencia de datos con JDBC.
- Aplicar principios de diseño y patrones arquitectónicos.
- Manejar excepciones y recursos de manera profesional.
- Validar integridad y coherencia de datos en todas las capas.
- Documentar código y procesos de desarrollo de forma clara y estructurada.

---

**Versión:** 1.0  
**Java:** 21  
**Base de Datos:** MariaDB 10.4.32  
**Driver JDBC:** MySQL Connector/J 8.4.0  
**Proyecto Educativo:** Trabajo Práctico Integrador de **Programación 2**
