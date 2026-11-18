package com.autoscolombia.parqueadero.servicio;

import com.autoscolombia.parqueadero.excepcion.DatosInvalidosException;
import com.autoscolombia.parqueadero.modelo.MedioPago;
import com.autoscolombia.parqueadero.modelo.Pago;
import com.autoscolombia.parqueadero.repositorio.PagoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// CU-31: total recaudado entre dos fechas (solo pagos activos)
@Service
@Transactional(readOnly = true)
public class RecaudoService {

    // Total de un medio de pago
    public record TotalMedio(String medio, long cantidad, BigDecimal total) {
    }

    // Resultado del recaudo
    public record Recaudo(BigDecimal total, long cantidad, List<TotalMedio> porMedio) {
    }

    private final PagoRepository pagoRepository;

    public RecaudoService(PagoRepository pagoRepository) {
        this.pagoRepository = pagoRepository;
    }

    public Recaudo calcular(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new DatosInvalidosException("Selecciona la fecha inicial y la fecha final.");
        }
        if (hasta.isBefore(desde)) {
            throw new DatosInvalidosException(
                "La fecha final no puede ser anterior a la fecha inicial.");
        }

        List<Pago> pagos = pagoRepository.activosEntre(
            desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());

        BigDecimal total = BigDecimal.ZERO;
        for (Pago pago : pagos) {
            total = total.add(pago.getValor());
        }

        List<TotalMedio> porMedio = java.util.Arrays.stream(MedioPago.values())
            .map(medio -> {
                List<Pago> delMedio = pagos.stream()
                    .filter(p -> p.getMedio() == medio)
                    .toList();
                BigDecimal suma = BigDecimal.ZERO;
                for (Pago pago : delMedio) {
                    suma = suma.add(pago.getValor());
                }
                String nombre = medio == MedioPago.EFECTIVO ? "Efectivo"
                    : (medio == MedioPago.TRANSFERENCIA ? "Transferencia" : "Tarjeta");
                return new TotalMedio(nombre, delMedio.size(), suma);
            })
            .toList();

        return new Recaudo(total, pagos.size(), porMedio);
    }
}