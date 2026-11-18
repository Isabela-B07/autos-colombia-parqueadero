package com.autoscolombia.parqueadero.servicio;

import com.autoscolombia.parqueadero.excepcion.DatosInvalidosException;
import com.autoscolombia.parqueadero.modelo.MedioPago;
import com.autoscolombia.parqueadero.modelo.Pago;
import com.autoscolombia.parqueadero.repositorio.PagoRepository;
import com.autoscolombia.parqueadero.servicio.RecaudoService.Recaudo;
import com.autoscolombia.parqueadero.servicio.RecaudoService.TotalMedio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Total recaudado entre dos fechas (HU-31, CU-31)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecaudoServiceTest {

    @Mock
    private PagoRepository pagoRepository;

    private RecaudoService servicio;
    private final LocalDate hoy = LocalDate.now();

    @BeforeEach
    void preparar() {
        servicio = new RecaudoService(pagoRepository);
    }

    private Pago pago(String valor, MedioPago medio) {
        Pago pago = new Pago();
        pago.setValor(new BigDecimal(valor));
        pago.setMedio(medio);
        return pago;
    }

    private TotalMedio delMedio(Recaudo recaudo, String nombre) {
        return recaudo.porMedio().stream()
            .filter(m -> m.medio().equals(nombre))
            .findFirst()
            .orElseThrow();
    }

    @Test
    void sumaElTotalYLoSeparaPorMedioDePago() {
        when(pagoRepository.activosEntre(any(), any())).thenReturn(List.of(
            pago("150000", MedioPago.EFECTIVO),
            pago("60000", MedioPago.EFECTIVO),
            pago("160000", MedioPago.TRANSFERENCIA)));

        Recaudo recaudo = servicio.calcular(hoy, hoy);

        assertEquals(0, new BigDecimal("370000").compareTo(recaudo.total()));
        assertEquals(3, recaudo.cantidad());
        assertEquals(2, delMedio(recaudo, "Efectivo").cantidad());
        assertEquals(0, new BigDecimal("210000").compareTo(delMedio(recaudo, "Efectivo").total()));
        assertEquals(1, delMedio(recaudo, "Transferencia").cantidad());
        assertEquals(0, delMedio(recaudo, "Tarjeta").cantidad());
    }

    @Test
    void sinPagosElTotalEsCero() {
        when(pagoRepository.activosEntre(any(), any())).thenReturn(List.of());

        Recaudo recaudo = servicio.calcular(hoy, hoy);

        assertEquals(0, BigDecimal.ZERO.compareTo(recaudo.total()));
        assertEquals(0, recaudo.cantidad());
    }

    @Test
    void elRangoIncluyeElDiaFinalCompleto() {
        when(pagoRepository.activosEntre(any(), any())).thenReturn(List.of());

        servicio.calcular(hoy.minusDays(3), hoy);

        ArgumentCaptor<LocalDateTime> desde = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> hasta = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(pagoRepository).activosEntre(desde.capture(), hasta.capture());
        assertEquals(hoy.minusDays(3).atStartOfDay(), desde.getValue());
        assertEquals(hoy.plusDays(1).atStartOfDay(), hasta.getValue());
    }

    @Test
    void unaFechaFinalAnteriorALaInicialSeRechaza() {
        assertThrows(DatosInvalidosException.class, () -> servicio.calcular(hoy, hoy.minusDays(1)));
        verify(pagoRepository, never()).activosEntre(any(), any());
    }

    @Test
    void sinFechasSeRechaza() {
        assertThrows(DatosInvalidosException.class, () -> servicio.calcular(null, hoy));
        assertThrows(DatosInvalidosException.class, () -> servicio.calcular(hoy, null));
    }
}