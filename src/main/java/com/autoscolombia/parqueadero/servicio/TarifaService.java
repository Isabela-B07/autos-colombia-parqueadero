package com.autoscolombia.parqueadero.servicio;

import com.autoscolombia.parqueadero.excepcion.DatosInvalidosException;
import com.autoscolombia.parqueadero.modelo.Tarifa;
import com.autoscolombia.parqueadero.modelo.TipoVehiculo;
import com.autoscolombia.parqueadero.repositorio.TarifaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Transactional(readOnly = true)
public class TarifaService {

    // Las tarifas no pueden pasar de este valor (cabe en la columna de la base de datos)
    private static final BigDecimal tarifaMaxima = new BigDecimal("100000000");

    // Tarifas que muestra la pantalla; null si todavía no se ha configurado
    public record Tarifas(BigDecimal carro, BigDecimal moto) {
    }

    private final TarifaRepository tarifaRepository;

    public TarifaService(TarifaRepository tarifaRepository) {
        this.tarifaRepository = tarifaRepository;
    }

    // CU-24: consultar las tarifas actuales
    public Tarifas consultar() {
        return new Tarifas(valorDe(TipoVehiculo.CARRO), valorDe(TipoVehiculo.MOTO));
    }

    // Valor de un tipo de vehículo; null si no está configurado (lo usa PagoService)
    public BigDecimal valorDe(TipoVehiculo tipo) {
        return tarifaRepository.findById(tipo).map(Tarifa::getValor).orElse(null);
    }

    // CU-24: cambiar las tarifas. Los pagos ya registrados conservan su valor.
    @Transactional
    public void actualizar(BigDecimal carro, BigDecimal moto) {
        validar("carro", carro);
        validar("moto", moto);
        guardar(TipoVehiculo.CARRO, carro);
        guardar(TipoVehiculo.MOTO, moto);
    }

    // ---------- Auxiliares ----------

    private void validar(String nombre, BigDecimal valor) {
        if (valor == null) {
            throw new DatosInvalidosException("Indica la tarifa de " + nombre + ".");
        }
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DatosInvalidosException(
                "La tarifa de " + nombre + " debe ser mayor que cero.");
        }
        if (valor.compareTo(tarifaMaxima) > 0) {
            throw new DatosInvalidosException(
                "La tarifa de " + nombre + " no puede superar 100.000.000.");
        }
    }

    private void guardar(TipoVehiculo tipo, BigDecimal valor) {
        Tarifa tarifa = tarifaRepository.findById(tipo).orElseGet(Tarifa::new);
        tarifa.setTipo(tipo);
        tarifa.setValor(valor.setScale(2, RoundingMode.HALF_UP));
        tarifaRepository.save(tarifa);
    }
}