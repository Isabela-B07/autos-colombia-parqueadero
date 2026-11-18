package com.autoscolombia.parqueadero.servicio;

import com.autoscolombia.parqueadero.excepcion.DatosInvalidosException;
import com.autoscolombia.parqueadero.modelo.Cliente;
import com.autoscolombia.parqueadero.modelo.Empleado;
import com.autoscolombia.parqueadero.modelo.EstadoPago;
import com.autoscolombia.parqueadero.modelo.MedioPago;
import com.autoscolombia.parqueadero.modelo.Mensualidad;
import com.autoscolombia.parqueadero.modelo.Pago;
import com.autoscolombia.parqueadero.modelo.Vehiculo;
import com.autoscolombia.parqueadero.repositorio.MensualidadRepository;
import com.autoscolombia.parqueadero.repositorio.PagoRepository;
import com.autoscolombia.parqueadero.repositorio.UsuarioRepository;
import com.autoscolombia.parqueadero.repositorio.VehiculoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PagoService {

    private static final DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Qué debe mostrar la pantalla de mensualidad sobre el pago del cliente:
    // estado = SIN_MENSUALIDAD, PENDIENTE (se puede registrar el pago) o PAGADA
    // valor = lo que se cobraría (null si falta alguna tarifa)
    // idPago = el pago activo, si la mensualidad ya está pagada
    // periodo = el periodo de la mensualidad que se paga, por ejemplo "14/11/2026 al 13/12/2026"
    public record InfoPago(String estado, BigDecimal valor, Integer idPago, String periodo) {
    }

    // Datos del comprobante de pago (CU-28)
    public record ComprobantePago(Integer numero, String fecha, String cliente, String documento,
                                  String placas, String periodo, BigDecimal valor, String medio,
                                  String referencia, String empleado, boolean anulado,
                                  String motivoAnulacion, String fechaAnulacion,
                                  String empleadoAnulacion) {
    }

    // Una fila del historial de pagos (CU-29)
    public record FilaPago(Integer numero, String fecha, String cliente, String documento,
                           String periodo, BigDecimal valor, String medio, boolean anulado) {
    }

    private final PagoRepository pagoRepository;
    private final MensualidadRepository mensualidadRepository;
    private final VehiculoRepository vehiculoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TarifaService tarifaService;

    public PagoService(PagoRepository pagoRepository,
                       MensualidadRepository mensualidadRepository,
                       VehiculoRepository vehiculoRepository,
                       UsuarioRepository usuarioRepository,
                       TarifaService tarifaService) {
        this.pagoRepository = pagoRepository;
        this.mensualidadRepository = mensualidadRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.usuarioRepository = usuarioRepository;
        this.tarifaService = tarifaService;
    }

    // CU-26: estado de pago de la última mensualidad del cliente
    public InfoPago consultar(Integer idCliente) {
        Mensualidad mensualidad = mensualidadRepository
            .findFirstByClienteIdUsuarioOrderByFechaFinDesc(idCliente)
            .orElse(null);
        if (mensualidad == null) {
            return new InfoPago("SIN_MENSUALIDAD", null, null, null);
        }

        String periodo = mensualidad.getFechaInicio().format(formato)
            + " al " + mensualidad.getFechaFin().format(formato);

        Pago activo = pagoRepository
            .findFirstByMensualidadIdMensualidadAndEstado(
                mensualidad.getIdMensualidad(), EstadoPago.ACTIVO)
            .orElse(null);
        if (activo != null) {
            return new InfoPago("PAGADA", activo.getValor(), activo.getIdPago(), periodo);
        }

        // Pendiente: se muestra lo que se cobraría; si falta una tarifa, queda sin valor
        BigDecimal valor;
        try {
            valor = calcularValor(idCliente);
        } catch (DatosInvalidosException e) {
            valor = null;
        }
        return new InfoPago("PENDIENTE", valor, null, periodo);
    }

    // Solo el estado de pago de la última mensualidad (para el listado de clientes)
    public String estadoPago(Integer idCliente) {
        return mensualidadRepository
            .findFirstByClienteIdUsuarioOrderByFechaFinDesc(idCliente)
            .map(m -> pagoRepository.existsByMensualidadIdMensualidadAndEstado(
                          m.getIdMensualidad(), EstadoPago.ACTIVO)
                      ? "PAGADA" : "PENDIENTE")
            .orElse("SIN_MENSUALIDAD");
    }

    // CU-25: el valor es la suma de la tarifa de cada vehículo del cliente
    public BigDecimal calcularValor(Integer idCliente) {
        List<Vehiculo> vehiculos = vehiculoRepository.findByClienteIdUsuarioOrderByPlaca(idCliente);
        if (vehiculos.isEmpty()) {
            throw new DatosInvalidosException("El cliente no tiene vehículos registrados.");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Vehiculo vehiculo : vehiculos) {
            BigDecimal tarifa = tarifaService.valorDe(vehiculo.getTipo());
            if (tarifa == null) {
                String tipo = vehiculo.getTipo().name().equals("CARRO") ? "carro" : "moto";
                throw new DatosInvalidosException(
                    "Falta configurar la tarifa de " + tipo + " en la pantalla Tarifas.");
            }
            total = total.add(tarifa);
        }
        return total;
    }

    // CU-25: registrar el pago de la última mensualidad del cliente
    @Transactional
    public Pago registrar(Integer idCliente, MedioPago medio, String referencia,
                          String nombreUsuarioEmpleado) {
        if (medio == null) {
            throw new DatosInvalidosException("Selecciona el medio de pago.");
        }
        String ref = (referencia == null || referencia.isBlank()) ? null : referencia.trim();
        if (ref != null && ref.length() > 50) {
            throw new DatosInvalidosException("La referencia no puede pasar de 50 caracteres.");
        }

        Mensualidad mensualidad = mensualidadRepository
            .findFirstByClienteIdUsuarioOrderByFechaFinDesc(idCliente)
            .orElseThrow(() -> new DatosInvalidosException(
                "El cliente no tiene mensualidad. Regístrala primero."));

        if (pagoRepository.existsByMensualidadIdMensualidadAndEstado(
                mensualidad.getIdMensualidad(), EstadoPago.ACTIVO)) {
            throw new DatosInvalidosException("La mensualidad ya está pagada.");
        }

        BigDecimal valor = calcularValor(idCliente);
        Empleado empleado = usuarioRepository.buscarEmpleado(nombreUsuarioEmpleado)
            .orElseThrow(() -> new DatosInvalidosException("El empleado no existe."));

        Pago pago = new Pago();
        pago.setMensualidad(mensualidad);
        pago.setValor(valor);
        pago.setMedio(medio);
        pago.setReferencia(ref);
        pago.setFechaHora(LocalDateTime.now());
        pago.setEmpleado(empleado);

        try {
            return pagoRepository.save(pago);
        } catch (DataIntegrityViolationException e) {
            // La base de datos no deja dos pagos activos (por ejemplo, un doble clic)
            throw new DatosInvalidosException("La mensualidad ya está pagada.");
        }
    }

    // CU-30: anular un pago. No se elimina: queda marcado como anulado con su motivo,
    // y la mensualidad vuelve a quedar pendiente de pago.
    @Transactional
    public void anular(Integer idPago, String motivo, String nombreUsuarioEmpleado) {
        String texto = motivo == null ? "" : motivo.trim();
        if (texto.isEmpty()) {
            throw new DatosInvalidosException("Indica el motivo de la anulación.");
        }
        if (texto.length() > 255) {
            throw new DatosInvalidosException("El motivo no puede pasar de 255 caracteres.");
        }

        Pago pago = pagoRepository.findById(idPago)
            .orElseThrow(() -> new DatosInvalidosException("El pago no existe."));
        if (!pago.estaActivo()) {
            throw new DatosInvalidosException("El pago ya estaba anulado.");
        }

        Empleado empleado = usuarioRepository.buscarEmpleado(nombreUsuarioEmpleado)
            .orElseThrow(() -> new DatosInvalidosException("El empleado no existe."));

        pago.setEstado(EstadoPago.ANULADO);
        pago.setMotivoAnulacion(texto);
        pago.setFechaHoraAnulacion(LocalDateTime.now());
        pago.setEmpleadoAnulacion(empleado);
        pagoRepository.save(pago);
    }

    // CU-28: datos del comprobante de un pago (también de los anulados)
    public ComprobantePago comprobante(Integer idPago) {
        Pago pago = pagoRepository.findById(idPago)
            .orElseThrow(() -> new DatosInvalidosException("El pago no existe."));
        Mensualidad mensualidad = pago.getMensualidad();
        Cliente cliente = mensualidad.getCliente();

        String placas = vehiculoRepository
            .findByClienteIdUsuarioOrderByPlaca(cliente.getIdUsuario()).stream()
            .map(Vehiculo::getPlaca)
            .collect(Collectors.joining(", "));
        String periodo = mensualidad.getFechaInicio().format(formato)
            + " al " + mensualidad.getFechaFin().format(formato);
        DateTimeFormatter conHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        boolean anulado = !pago.estaActivo();

        return new ComprobantePago(
            pago.getIdPago(),
            pago.getFechaHora().format(conHora),
            cliente.getNombre(),
            cliente.getDocumento(),
            placas,
            periodo,
            pago.getValor(),
            pago.getMedio().name().equals("EFECTIVO") ? "Efectivo"
                : (pago.getMedio().name().equals("TRANSFERENCIA") ? "Transferencia" : "Tarjeta"),
            pago.getReferencia(),
            pago.getEmpleado().getNombre(),
            anulado,
            pago.getMotivoAnulacion(),
            pago.getFechaHoraAnulacion() == null ? null : pago.getFechaHoraAnulacion().format(conHora),
            pago.getEmpleadoAnulacion() == null ? null : pago.getEmpleadoAnulacion().getNombre());
    }

    // CU-29: historial de pagos con filtros por cliente, fechas y medio de pago
    public List<FilaPago> historial(String texto, MedioPago medio,
                                    LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new DatosInvalidosException(
                "La fecha final no puede ser anterior a la fecha inicial.");
        }
        String filtro = (texto == null || texto.isBlank()) ? null : texto.trim();
        LocalDateTime inicio = desde == null ? null : desde.atStartOfDay();
        LocalDateTime fin = hasta == null ? null : hasta.plusDays(1).atStartOfDay();

        DateTimeFormatter conHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return pagoRepository.filtrar(filtro, medio, inicio, fin).stream()
            .map(p -> new FilaPago(
                p.getIdPago(),
                p.getFechaHora().format(conHora),
                p.getMensualidad().getCliente().getNombre(),
                p.getMensualidad().getCliente().getDocumento(),
                p.getMensualidad().getFechaInicio().format(formato)
                    + " al " + p.getMensualidad().getFechaFin().format(formato),
                p.getValor(),
                p.getMedio().name().equals("EFECTIVO") ? "Efectivo"
                    : (p.getMedio().name().equals("TRANSFERENCIA") ? "Transferencia" : "Tarjeta"),
                !p.estaActivo()))
            .toList();
    }
}