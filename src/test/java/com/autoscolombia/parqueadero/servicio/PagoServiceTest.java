package com.autoscolombia.parqueadero.servicio;

import com.autoscolombia.parqueadero.excepcion.DatosInvalidosException;
import com.autoscolombia.parqueadero.modelo.Empleado;
import com.autoscolombia.parqueadero.modelo.EstadoPago;
import com.autoscolombia.parqueadero.modelo.MedioPago;
import com.autoscolombia.parqueadero.modelo.Mensualidad;
import com.autoscolombia.parqueadero.modelo.Pago;
import com.autoscolombia.parqueadero.modelo.TipoVehiculo;
import com.autoscolombia.parqueadero.modelo.Vehiculo;
import com.autoscolombia.parqueadero.repositorio.MensualidadRepository;
import com.autoscolombia.parqueadero.repositorio.PagoRepository;
import com.autoscolombia.parqueadero.repositorio.UsuarioRepository;
import com.autoscolombia.parqueadero.repositorio.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Reglas de pago de la mensualidad (HU-24 a HU-30, CU-24 a CU-30)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PagoServiceTest {

    private static final Integer idCliente = 1;
    private static final Integer idMensualidad = 7;
    private static final String usuarioAdmin = "admin";

    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private MensualidadRepository mensualidadRepository;
    @Mock
    private VehiculoRepository vehiculoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TarifaService tarifaService;

    private PagoService servicio;
    private Mensualidad mensualidad;

    @BeforeEach
    void preparar() {
        servicio = new PagoService(pagoRepository, mensualidadRepository,
            vehiculoRepository, usuarioRepository, tarifaService);

        mensualidad = mock(Mensualidad.class);
        when(mensualidad.getIdMensualidad()).thenReturn(idMensualidad);
        when(mensualidad.getFechaInicio()).thenReturn(LocalDate.now());
        when(mensualidad.getFechaFin()).thenReturn(LocalDate.now().plusDays(30));
        when(mensualidadRepository.findFirstByClienteIdUsuarioOrderByFechaFinDesc(idCliente))
            .thenReturn(Optional.of(mensualidad));

        when(usuarioRepository.buscarEmpleado(usuarioAdmin)).thenReturn(Optional.of(new Empleado()));
        when(tarifaService.valorDe(TipoVehiculo.CARRO)).thenReturn(new BigDecimal("150000"));
        when(tarifaService.valorDe(TipoVehiculo.MOTO)).thenReturn(new BigDecimal("60000"));
    }

    // Deja al cliente con los vehículos de los tipos indicados
    private void clienteConVehiculos(TipoVehiculo... tipos) {
        List<Vehiculo> vehiculos = new java.util.ArrayList<>();
        for (TipoVehiculo tipo : tipos) {
            Vehiculo vehiculo = mock(Vehiculo.class);
            when(vehiculo.getTipo()).thenReturn(tipo);
            vehiculos.add(vehiculo);
        }
        when(vehiculoRepository.findByClienteIdUsuarioOrderByPlaca(idCliente)).thenReturn(vehiculos);
    }

    // ---------- Valor de la mensualidad (CU-25) ----------

    @Test
    void elValorEsLaSumaDeLaTarifaDeCadaVehiculo() {
        clienteConVehiculos(TipoVehiculo.CARRO, TipoVehiculo.MOTO);

        BigDecimal valor = servicio.calcularValor(idCliente);

        assertEquals(0, new BigDecimal("210000").compareTo(valor));
    }

    @Test
    void conUnSoloCarroElValorEsLaTarifaDeCarro() {
        clienteConVehiculos(TipoVehiculo.CARRO);

        assertEquals(0, new BigDecimal("150000").compareTo(servicio.calcularValor(idCliente)));
    }

    @Test
    void sinVehiculosNoSePuedeCalcularElValor() {
        clienteConVehiculos();

        assertThrows(DatosInvalidosException.class, () -> servicio.calcularValor(idCliente));
    }

    @Test
    void siFaltaLaTarifaDeUnTipoSeRechaza() {
        clienteConVehiculos(TipoVehiculo.MOTO);
        when(tarifaService.valorDe(TipoVehiculo.MOTO)).thenReturn(null);

        assertThrows(DatosInvalidosException.class, () -> servicio.calcularValor(idCliente));
    }

    // ---------- Estado de pago (CU-26) ----------

    @Test
    void sinMensualidadElEstadoDePagoEsSinMensualidad() {
        when(mensualidadRepository.findFirstByClienteIdUsuarioOrderByFechaFinDesc(idCliente))
            .thenReturn(Optional.empty());

        assertEquals("SIN_MENSUALIDAD", servicio.estadoPago(idCliente));
    }

    @Test
    void conPagoActivoLaMensualidadEstaPagada() {
        when(pagoRepository.existsByMensualidadIdMensualidadAndEstado(idMensualidad, EstadoPago.ACTIVO))
            .thenReturn(true);

        assertEquals("PAGADA", servicio.estadoPago(idCliente));
    }

    @Test
    void sinPagoActivoLaMensualidadQuedaPendiente() {
        when(pagoRepository.existsByMensualidadIdMensualidadAndEstado(idMensualidad, EstadoPago.ACTIVO))
            .thenReturn(false);

        assertEquals("PENDIENTE", servicio.estadoPago(idCliente));
    }

    // ---------- Registrar pago (CU-25) ----------

    @Test
    void registrarPagoGuardaElValorCalculadoPorElSistema() {
        clienteConVehiculos(TipoVehiculo.CARRO, TipoVehiculo.MOTO);

        servicio.registrar(idCliente, MedioPago.TRANSFERENCIA, "  APR-123  ", usuarioAdmin);

        ArgumentCaptor<Pago> captor = ArgumentCaptor.forClass(Pago.class);
        verify(pagoRepository).save(captor.capture());
        Pago guardado = captor.getValue();
        assertEquals(0, new BigDecimal("210000").compareTo(guardado.getValor()));
        assertEquals(MedioPago.TRANSFERENCIA, guardado.getMedio());
        assertEquals("APR-123", guardado.getReferencia());
        assertEquals(mensualidad, guardado.getMensualidad());
    }

    @Test
    void laReferenciaEnBlancoSeGuardaVacia() {
        clienteConVehiculos(TipoVehiculo.CARRO);

        servicio.registrar(idCliente, MedioPago.EFECTIVO, "   ", usuarioAdmin);

        ArgumentCaptor<Pago> captor = ArgumentCaptor.forClass(Pago.class);
        verify(pagoRepository).save(captor.capture());
        assertNull(captor.getValue().getReferencia());
    }

    @Test
    void registrarSinMedioDePagoSeRechaza() {
        clienteConVehiculos(TipoVehiculo.CARRO);

        assertThrows(DatosInvalidosException.class,
            () -> servicio.registrar(idCliente, null, null, usuarioAdmin));
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void unaReferenciaDeMasDeCincuentaCaracteresSeRechaza() {
        clienteConVehiculos(TipoVehiculo.CARRO);

        assertThrows(DatosInvalidosException.class,
            () -> servicio.registrar(idCliente, MedioPago.TARJETA, "A".repeat(51), usuarioAdmin));
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void unaReferenciaDeCincuentaCaracteresSeAcepta() {
        clienteConVehiculos(TipoVehiculo.CARRO);

        servicio.registrar(idCliente, MedioPago.TARJETA, "A".repeat(50), usuarioAdmin);

        verify(pagoRepository).save(any(Pago.class));
    }

    @Test
    void sinMensualidadNoSePuedePagar() {
        when(mensualidadRepository.findFirstByClienteIdUsuarioOrderByFechaFinDesc(idCliente))
            .thenReturn(Optional.empty());

        assertThrows(DatosInvalidosException.class,
            () -> servicio.registrar(idCliente, MedioPago.EFECTIVO, null, usuarioAdmin));
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void unaMensualidadYaPagadaNoSePuedePagarDeNuevo() {
        clienteConVehiculos(TipoVehiculo.CARRO);
        when(pagoRepository.existsByMensualidadIdMensualidadAndEstado(idMensualidad, EstadoPago.ACTIVO))
            .thenReturn(true);

        assertThrows(DatosInvalidosException.class,
            () -> servicio.registrar(idCliente, MedioPago.EFECTIVO, null, usuarioAdmin));
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void siLaBaseDeDatosRechazaUnSegundoPagoActivoSeInformaQueYaEstaPagada() {
        clienteConVehiculos(TipoVehiculo.CARRO);
        when(pagoRepository.save(any(Pago.class))).thenThrow(new DataIntegrityViolationException("duplicado"));

        assertThrows(DatosInvalidosException.class,
            () -> servicio.registrar(idCliente, MedioPago.EFECTIVO, null, usuarioAdmin));
    }

    // ---------- Anular pago (CU-30) ----------

    private Pago pagoActivo() {
        Pago pago = new Pago();
        pago.setEstado(EstadoPago.ACTIVO);
        when(pagoRepository.findById(5)).thenReturn(Optional.of(pago));
        return pago;
    }

    @Test
    void anularMarcaElPagoComoAnuladoConSuMotivo() {
        Pago pago = pagoActivo();

        servicio.anular(5, "  Error en el medio de pago  ", usuarioAdmin);

        assertEquals(EstadoPago.ANULADO, pago.getEstado());
        assertEquals("Error en el medio de pago", pago.getMotivoAnulacion());
        assertTrue(pago.getFechaHoraAnulacion() != null);
        assertTrue(pago.getEmpleadoAnulacion() != null);
        verify(pagoRepository).save(pago);
    }

    @Test
    void anularSinMotivoSeRechaza() {
        pagoActivo();

        assertThrows(DatosInvalidosException.class, () -> servicio.anular(5, "   ", usuarioAdmin));
        assertThrows(DatosInvalidosException.class, () -> servicio.anular(5, null, usuarioAdmin));
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void unMotivoDeMasDeDoscientosCincuentaYCincoCaracteresSeRechaza() {
        pagoActivo();

        assertThrows(DatosInvalidosException.class,
            () -> servicio.anular(5, "A".repeat(256), usuarioAdmin));
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void anularUnPagoQueNoExisteSeRechaza() {
        when(pagoRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(DatosInvalidosException.class, () -> servicio.anular(99, "Motivo", usuarioAdmin));
    }

    @Test
    void unPagoYaAnuladoNoSePuedeAnularOtraVez() {
        Pago pago = new Pago();
        pago.setEstado(EstadoPago.ANULADO);
        when(pagoRepository.findById(5)).thenReturn(Optional.of(pago));

        assertThrows(DatosInvalidosException.class, () -> servicio.anular(5, "Motivo", usuarioAdmin));
        verify(pagoRepository, never()).save(any());
    }

    // ---------- Historial (CU-29) ----------

    @Test
    void elHistorialRechazaUnaFechaFinalAnteriorALaInicial() {
        LocalDate hoy = LocalDate.now();

        assertThrows(DatosInvalidosException.class,
            () -> servicio.historial(null, null, hoy, hoy.minusDays(1)));
    }
}