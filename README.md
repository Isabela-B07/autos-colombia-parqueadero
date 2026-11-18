# Autos Colombia - Sistema de gestión de parqueadero

Aplicación web para gestionar el parqueadero "Autos Colombia", que presta el servicio por mensualidad. Incluye la **Iteración 1** (entrada y salida de vehículos, Evidencia de aprendizaje 1), la **Iteración 2** (gestión de usuarios y de celdas, Evidencia de aprendizaje 2) y la **Iteración 3** (gestión de pagos, Evidencia de aprendizaje 3) del diseño de un Sistema de información.

Los cambios de cada iteración están resumidos en [CHANGELOG.md](CHANGELOG.md). Además, el commit de cada iteración detalla las historias de usuario que implementa.

- **Institución:** Institución Universitaria Digital de Antioquia
- **Programa:** Ingeniería de Software
- **Docente:** Esteban Montoya
- **Autoras:** Isabela Blandon Velez y Salome Molina Reyes

## Tecnologías

- Java (se probó con Temurin 25; la versión exigida está en `java.version` del `pom.xml`)
- Spring Boot, Spring Web, Spring Data JPA, Spring Security y Validation
- Thymeleaf (pantallas HTML)
- MySQL 8.0 o superior
- Maven (incluido mediante `mvnw`, no hay que instalarlo)
- JUnit 5 y Mockito (pruebas automáticas)

## Funcionalidades
### Iteración 1: entrada y salida de vehículos

| Historia | Funcionalidad |
|----------|---------------|
| HU-01 | Inicio de sesión con usuario y contraseña (contraseñas cifradas con BCrypt) y roles |
| HU-02 | Registrar la entrada de un vehículo por su placa |
| HU-03 | Validar que la placa pertenezca a un usuario con mensualidad vigente |
| HU-04 | Evitar ingresos duplicados |
| HU-05 | Asignar celda automáticamente; si no hay celda libre, el vehículo entra y queda una novedad para el administrador |
| HU-06 | Registrar la salida, calcular el tiempo de permanencia y liberar la celda |
| HU-07 | Registrar novedades sobre vehículos que están dentro |
| HU-08 | Consultar los vehículos dentro del parqueadero |
| HU-09 | Consultar el historial de entradas y salidas (solo administrador) |
| HU-10 | Consultar novedades (solo administrador) |
| HU-11 | Ver la disponibilidad de celdas |

Formato de placa: carros con 3 letras y 3 números (`ABC123`) y motos con 3 letras, 2 números y 1 letra (`ABC12D`).

### Iteración 2: gestión de usuarios y de celdas

Todas las historias de esta iteración las ejecuta el administrador.

| Historia | Casos de uso | Funcionalidad |
|----------|--------------|---------------|
| HU-12 | CU-12 | Registrar un cliente (el documento no puede repetirse) |
| HU-13 | CU-13 | Registrar uno o más vehículos de un cliente (el tipo debe coincidir con el formato de la placa) |
| HU-14 | CU-14 | Registrar la mensualidad de un cliente (solo una vigente a la vez) |
| HU-15 | CU-15 | Renovar la mensualidad cuando está vencida o le faltan 5 días o menos, conservando el historial |
| HU-16 | CU-16 | Consultar y buscar clientes por nombre, documento o placa, con el estado de su mensualidad |
| HU-17 | CU-17 | Editar los datos del cliente y de sus vehículos (la placa solo se cambia si no tiene movimientos) |
| HU-18 | CU-18 | Activar o desactivar un cliente (un cliente inactivo no ingresa, pero su vehículo ya dentro puede salir) |
| HU-19 | CU-19 | Registrar empleados con su rol y contraseña cifrada |
| HU-20 | CU-20 | Registrar celdas |
| HU-21 | CU-21 | Editar una celda que no esté ocupada |
| HU-22 | CU-22 | Poner una celda en mantenimiento o habilitarla |
| HU-23 | CU-23 | Listar las celdas filtrando por tipo y estado |

### Iteración 3: gestión de pagos

Todas las pantallas de pagos las usa el administrador. La única regla que afecta al vigilante es la HU-27, que se aplica al registrar una entrada.

| Historia | Casos de uso | Funcionalidad |
|----------|--------------|---------------|
| HU-24 | CU-24 | Configurar la tarifa mensual de carro y de moto |
| HU-25 | CU-25 | Registrar el pago de la mensualidad con su medio de pago (efectivo, transferencia o tarjeta) y una referencia opcional |
| HU-26 | CU-26 | Consultar el estado de pago de la mensualidad (pagada o pendiente) |
| HU-27 | CU-27 | Rechazar la entrada de un vehículo cuya mensualidad está vigente pero sin pagar |
| HU-28 | CU-28 | Ver e imprimir el comprobante de pago |
| HU-29 | CU-29 | Consultar el historial de pagos filtrando por cliente, fechas y medio de pago |
| HU-30 | CU-30 | Anular un pago indicando el motivo (el pago no se elimina) |
| HU-31 | CU-31 | Consultar el total recaudado entre dos fechas, con el desglose por medio de pago |

Reglas de negocio de los pagos:

- Solo se paga la mensualidad, en un único pago completo (no hay abonos). El pago se aplica a la última mensualidad del cliente.
- La tarifa se configura por tipo de vehículo. El valor a pagar es la suma de la tarifa de cada vehículo del cliente y lo calcula el sistema (no se puede editar).
- Una mensualidad solo puede tener un pago activo a la vez.
- Un pago no se elimina: se anula con motivo, fecha y empleado, y la mensualidad vuelve a quedar pendiente de pago. El total recaudado no cuenta los pagos anulados.
- Para registrar una entrada, la mensualidad debe estar vigente **y pagada**. Un vehículo que ya está dentro siempre puede salir.
- Orden de las validaciones al ingresar: cliente inactivo, mensualidad vencida y, por último, mensualidad sin pago.
- Los valores de dinero se guardan como `DECIMAL(12,2)` y se manejan con `BigDecimal`.

## Requisitos previos

1. JDK instalado (`java -version`)
2. MySQL 8.0 o superior en ejecución
3. Git

## Cómo ejecutarlo

### 1. Clonar el repositorio

```
git clone https://github.com/Isabela-B07/autos-colombia-parqueadero.git
cd autos-colombia-parqueadero
```

### 2. Crear la base de datos

Se ejecutan dos scripts, en este orden (por ejemplo, desde MySQL Workbench):

1. **Script base (Iteraciones 1 y 2):** https://drive.google.com/file/d/1BX3_kQmAg42a6xDaam7LfuSQ8NBMSrT0/view?usp=sharing. Crea la base de datos `autos_colombia` con sus tablas.
2. **Script de la Iteración 3:** https://drive.google.com/file/d/1DwnbHtf-7XHY4EfOQNoXSb61iKSeIazM/view?usp=sharing. Crea las tablas `tarifa` y `pago`, el disparador que impide eliminar pagos, las tarifas iniciales (carro $150.000 y moto $60.000) y un pago en efectivo para cada mensualidad que ya existía, para que los datos anteriores sigan funcionando.

### 3. Configurar la conexión

Por seguridad, la contraseña de MySQL no se sube al repositorio. Crea el archivo `src/main/resources/application-local.properties` con este contenido, cambiando `TU_CLAVE` por tu contraseña:

```
spring.datasource.url=jdbc:mysql://localhost:3306/autos_colombia?useSSL=false&serverTimezone=America/Bogota&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=TU_CLAVE
```

### 4. Ejecutar

En Windows (PowerShell) o Linux/Mac:

```
./mvnw spring-boot:run
```

La primera vez descarga Maven y las dependencias, así que puede tardar unos minutos. Cuando aparezca `Started ParqueaderoApplication`, abre:

```
http://localhost:8080
```

### 5. Ejecutar las pruebas automáticas

```
./mvnw test
```

Las pruebas usan JUnit 5 y Mockito y no necesitan la base de datos. Cubren las reglas de negocio de las Iteraciones 2 y 3: formato de placa, mensualidades, pagos y recaudo.

## Datos de prueba

Al arrancar con el perfil `local`, el sistema crea datos de prueba si no existen (solo para desarrollo).

**Usuarios del sistema**

| Usuario | Contraseña | Rol |
|---------|------------|-----|
| `admin` | `Admin123` | Administrador |
| `vigilante1` | `Vigilante123` | Vigilante |

**Vehículos**

| Placa | Tipo | Mensualidad |
|-------|------|-------------|
| `ABC123` | Carro | Vigente |
| `XYZ12A` | Moto | Vigente |
| `DEF456` | Carro | Vencida (se rechaza la entrada) |

Desde la Iteración 3, un vehículo con mensualidad vigente solo puede ingresar si esa mensualidad está pagada. Si en Clientes aparece como **Pendiente**, el administrador registra el pago en la pantalla de Mensualidad del cliente.

**Celdas:** `C01` a `C05` (carros), `M01` a `M03` (motos) y `C06` en mantenimiento.

## Pantallas y permisos

| Pantalla | Ruta | Rol |
|----------|------|-----|
| Login | `/login` | Público |
| Panel principal | `/panel` | Vigilante y administrador |
| Registrar entrada | `/entrada` | Vigilante y administrador |
| Registrar salida | `/salida` | Vigilante y administrador |
| Registrar novedad | `/novedades/registrar` | Vigilante y administrador |
| Vehículos dentro | `/consultas/dentro` | Vigilante y administrador |
| Celdas | `/consultas/celdas` | Vigilante y administrador |
| Historial | `/admin/historial` | Solo administrador |
| Consulta de novedades | `/admin/novedades` | Solo administrador |
| Clientes (incluye la columna Pago) | `/admin/clientes` | Solo administrador |
| Registrar y editar cliente | `/admin/clientes/nuevo` y `/admin/clientes/{id}/editar` | Solo administrador |
| Mensualidad del cliente (incluye registrar el pago) | `/admin/clientes/{id}/mensualidad` | Solo administrador |
| Empleados | `/admin/empleados` | Solo administrador |
| Gestión de celdas | `/admin/celdas` | Solo administrador |
| Tarifas | `/admin/tarifas` | Solo administrador |
| Pagos (historial y anular) | `/admin/pagos` | Solo administrador |
| Comprobante de pago | `/admin/pagos/{id}` | Solo administrador |
| Recaudo | `/admin/recaudo` | Solo administrador |

## Arquitectura

Aplicación web por capas. El código fluye en una sola dirección: `controlador` → `servicio` → `repositorio` → base de datos.

```
src/main/java/com/autoscolombia/parqueadero
├── modelo        Entidades y enumeraciones (Usuario, Cliente, Empleado, Vehiculo,
│                 Mensualidad, Celda, Movimiento, Novedad, Rol, TipoVehiculo,
│                 EstadoCelda, TipoNovedad). Iteración 3: Tarifa, Pago, MedioPago,
│                 EstadoPago
├── repositorio   Único acceso a la base de datos (Spring Data JPA). Iteración 3:
│                 TarifaRepository, PagoRepository
├── servicio      Reglas de negocio. Iteración 1: MovimientoService, UsuarioService,
│                 CeldaService, NovedadService, ValidacionPlacaService, AuthService,
│                 DetalleEmpleadoService, DatosIniciales. Iteración 2: ClienteService,
│                 MensualidadService, EmpleadoService, GestionCeldaService.
│                 Iteración 3: TarifaService, PagoService, RecaudoService
├── controlador   Reciben las solicitudes de las pantallas. Iteración 3:
│                 TarifaController, PagoController, RecaudoController
├── seguridad     Cifrado de contraseñas y configuración de roles
└── excepcion     Reglas de negocio incumplidas (placa inválida, mensualidad vencida,
                  ingreso activo, sin celda disponible, cliente inactivo,
                  datos inválidos). Iteración 3: mensualidad sin pago
```

Las pantallas están en `src/main/resources/templates` (Thymeleaf) y los estilos en `src/main/resources/static/css`.

### Cambios sobre el código de iteraciones anteriores

Las Iteraciones 2 y 3 se construyeron sobre la misma estructura por capas, sin cambiarla. Los cambios en código existente fueron mínimos:

**Iteración 2**
- `UsuarioService` y `MovimientoController` rechazan el ingreso de un cliente desactivado.
- Se agregaron consultas a los repositorios y las opciones de administración al menú.

**Iteración 3**
- `UsuarioService.validarIngreso` ahora también rechaza el ingreso cuando la mensualidad vigente no tiene un pago activo (`MensualidadSinPagoException`, HU-27).
- `MovimientoController` muestra ese mensaje al rechazar una entrada. La salida no cambió: un vehículo que ya está dentro siempre puede salir.
- `ClienteService` agrega el estado de pago a cada fila del listado de clientes, y la pantalla Clientes muestra la nueva columna **Pago**.
- La pantalla de Mensualidad del cliente tiene una sección nueva para ver el estado de pago y registrar el pago.
- El menú lateral suma las opciones Tarifas, Pagos y Recaudo, tiene scroll propio con el pie fijo para que quepan todas y usa íconos de línea en lugar de emojis.

**Autenticación:** el inicio de sesión lo ejecuta Spring Security. Consulta al empleado mediante `DetalleEmpleadoService`, compara la contraseña cifrada con BCrypt y aplica los permisos por rol.

**Integridad de los datos:** la base de datos impide dos ingresos activos de una misma placa y que una celda se asigne a dos vehículos a la vez. Los movimientos y las novedades no se eliminan físicamente. Desde la Iteración 3, la base de datos también impide dos pagos activos para una misma mensualidad y un disparador impide eliminar pagos: solo se anulan.

## Documentación del diseño

El diseño completo (requerimientos, historias de usuario, arquitectura, mockups, modelo de datos, casos de uso, diagramas de clases, de paquetes, de secuencia y de componentes, y plan de pruebas) está en los documentos de las Evidencias de aprendizaje 1, 2 y 3.

Las Iteraciones 1 y 2 usan el script base. La Iteración 3 agrega las tablas `tarifa` y `pago` con su propio script, el segundo enlace del paso 2 de "Cómo ejecutarlo".