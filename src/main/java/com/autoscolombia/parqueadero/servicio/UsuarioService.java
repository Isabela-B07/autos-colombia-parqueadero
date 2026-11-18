package com.autoscolombia.parqueadero.servicio;

import com.autoscolombia.parqueadero.excepcion.ClienteInactivoException;
import com.autoscolombia.parqueadero.excepcion.MensualidadSinPagoException;
import com.autoscolombia.parqueadero.excepcion.MensualidadVencidaException;
import com.autoscolombia.parqueadero.excepcion.PlacaInvalidaException;
import com.autoscolombia.parqueadero.modelo.Cliente;
import com.autoscolombia.parqueadero.modelo.EstadoPago;
import com.autoscolombia.parqueadero.modelo.Mensualidad;
import com.autoscolombia.parqueadero.modelo.Vehiculo;
import com.autoscolombia.parqueadero.repositorio.MensualidadRepository;
import com.autoscolombia.parqueadero.repositorio.PagoRepository;
import com.autoscolombia.parqueadero.repositorio.VehiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private final VehiculoRepository vehiculoRepository;
    private final MensualidadRepository mensualidadRepository;
    private final PagoRepository pagoRepository;
    private final ValidacionPlacaService validacionPlacaService;

    public UsuarioService(VehiculoRepository vehiculoRepository,
                          MensualidadRepository mensualidadRepository,
                          PagoRepository pagoRepository,
                          ValidacionPlacaService validacionPlacaService) {
        this.vehiculoRepository = vehiculoRepository;
        this.mensualidadRepository = mensualidadRepository;
        this.pagoRepository = pagoRepository;
        this.validacionPlacaService = validacionPlacaService;
    }

    // Devuelve el cliente dueño de la placa; si no existe, rechaza con el motivo
    public Cliente buscarPorPlaca(String placa) {
        String p = validacionPlacaService.normalizar(placa);
        Vehiculo vehiculo = vehiculoRepository.findByPlaca(p)
            .orElseThrow(() -> new PlacaInvalidaException(
                "La placa " + p + " no está registrada."));
        return vehiculo.getCliente();
    }

    public boolean tieneMensualidadVigente(String placa) {
        Cliente cliente = buscarPorPlaca(placa);
        return !mensualidadRepository
            .buscarVigentes(cliente.getIdUsuario(), LocalDate.now())
            .isEmpty();
    }

    // Autoriza o rechaza el ingreso: lanza excepción si algo falla
    public Cliente validarIngreso(String placa) {
        Cliente cliente = buscarPorPlaca(placa);
        if (!cliente.isActivo()) {
            throw new ClienteInactivoException(
                "El cliente está desactivado y no puede ingresar.");
        }

        List<Mensualidad> vigentes = mensualidadRepository
            .buscarVigentes(cliente.getIdUsuario(), LocalDate.now());
        if (vigentes.isEmpty()) {
            throw new MensualidadVencidaException(
                "La mensualidad del usuario está vencida.");
        }

        // Iteración 3 (HU-27): la mensualidad vigente debe tener un pago activo
        boolean pagada = vigentes.stream().anyMatch(m ->
            pagoRepository.existsByMensualidadIdMensualidadAndEstado(
                m.getIdMensualidad(), EstadoPago.ACTIVO));
        if (!pagada) {
            throw new MensualidadSinPagoException(
                "La mensualidad está pendiente de pago.");
        }
        return cliente;
    }
}