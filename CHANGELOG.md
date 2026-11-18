# Historial de cambios

Resumen de lo que cambió en cada iteración del sistema "Autos Colombia". Cada iteración se subió al repositorio con un commit que indica las historias de usuario que incluye, y en este archivo se detalla qué archivos se tocaron.

Convención usada: **Nuevo** es código o tablas que no existían, y **Modificado** es código de una iteración anterior que se tuvo que tocar.

## Iteración 3: gestión de pagos

Evidencia de aprendizaje 3. Historias HU-24 a HU-31, casos de uso CU-24 a CU-31.

### Base de datos
- **Nuevo** script `iteracion3_pagos.sql` (en Drive, enlace en el README, igual que el script base): tablas `tarifa` y `pago`, disparador `trg_pago_no_delete` (los pagos no se eliminan), restricción de un solo pago activo por mensualidad, tarifas iniciales (carro $150.000 y moto $60.000) y un pago en efectivo para cada mensualidad ya existente.

### Código nuevo
- **Modelo:** `Tarifa`, `Pago`, `MedioPago`, `EstadoPago`.
- **Repositorio:** `TarifaRepository`, `PagoRepository`.
- **Servicio:** `TarifaService` (HU-24), `PagoService` (HU-25, HU-26, HU-28, HU-29 y HU-30), `RecaudoService` (HU-31).
- **Controlador:** `TarifaController`, `PagoController`, `RecaudoController`.
- **Excepción:** `MensualidadSinPagoException` (HU-27).
- **Pantallas:** `admin-tarifas.html`, `admin-pagos.html`, `admin-comprobante.html`, `admin-recaudo.html`.
- **Pruebas automáticas:** `PagoServiceTest` y `RecaudoServiceTest`.

### Código de iteraciones anteriores que se modificó
| Archivo | Cambio | Historia |
|---------|--------|----------|
| `servicio/UsuarioService.java` | `validarIngreso` también rechaza el ingreso si la mensualidad vigente no tiene un pago activo. El orden queda: cliente inactivo, mensualidad vencida, mensualidad sin pago. | HU-27 |
| `controlador/MovimientoController.java` | Al registrar una entrada, muestra el mensaje de `MensualidadSinPagoException`. La salida no se tocó. | HU-27 |
| `servicio/ClienteService.java` | Cada fila del listado de clientes incluye el estado de pago. | HU-26 |
| `templates/admin-clientes.html` | Nueva columna **Pago**, que muestra si la mensualidad está pagada o pendiente. | HU-26 |
| `controlador/MensualidadController.java` | Muestra el estado de pago y permite registrar el pago de la mensualidad. | HU-25 |
| `templates/admin-mensualidad.html` | Nueva sección "Pago de la mensualidad". | HU-25 |
| `templates/fragmentos.html` | Menú con las opciones Tarifas, Pagos y Recaudo, y con íconos de línea en lugar de emojis. | HU-24, HU-29, HU-31 |
| `static/css/estilos.css` | Estilos del comprobante, de la etiqueta "Pendiente" y de impresión. El menú lateral tiene scroll propio con el pie fijo para que quepan todas las opciones. | HU-28 |

### Reglas que cambian el comportamiento anterior
- Un vehículo con mensualidad vigente pero **sin pago** ya no puede ingresar. Un vehículo que ya estaba dentro sí puede salir.

## Iteración 2: gestión de usuarios y de celdas

Evidencia de aprendizaje 2. Historias HU-12 a HU-23, casos de uso CU-12 a CU-23.

### Código nuevo
- **Servicio:** `ClienteService`, `MensualidadService`, `EmpleadoService`, `GestionCeldaService`.
- **Controladores y pantallas** para clientes, vehículos, mensualidades, empleados y gestión de celdas.
- **Pruebas automáticas** del formato de placa (`ValidacionPlacaServiceTest`) y de las mensualidades (`MensualidadServiceTest`).
- No se agregaron tablas: se usaron las del script base.

### Código de la Iteración 1 que se modificó
- `UsuarioService` y `MovimientoController`: rechazan el ingreso de un cliente desactivado (HU-18).
- Los repositorios recibieron las consultas necesarias.
- El menú lateral suma las opciones de administración.

## Iteración 1: entrada y salida de vehículos

Evidencia de aprendizaje 1. Historias HU-01 a HU-11.

- Inicio de sesión con roles y contraseñas cifradas con BCrypt (HU-01).
- Registro de entrada y de salida, validación de mensualidad, ingresos duplicados y asignación automática de celdas (HU-02 a HU-06).
- Novedades, vehículos dentro, historial y disponibilidad de celdas (HU-07 a HU-11).
- Script base de la base de datos `autos_colombia`.