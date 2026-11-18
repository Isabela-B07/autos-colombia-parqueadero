package com.autoscolombia.parqueadero.controlador;

import com.autoscolombia.parqueadero.excepcion.DatosInvalidosException;
import com.autoscolombia.parqueadero.modelo.Cliente;
import com.autoscolombia.parqueadero.modelo.MedioPago;
import com.autoscolombia.parqueadero.modelo.Pago;
import com.autoscolombia.parqueadero.modelo.Vehiculo;
import com.autoscolombia.parqueadero.servicio.ClienteService;
import com.autoscolombia.parqueadero.servicio.MensualidadService;
import com.autoscolombia.parqueadero.servicio.PagoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.stream.Collectors;

// Registrar y renovar mensualidades y registrar su pago: solo el administrador
@Controller
@RequestMapping("/admin/clientes/{id}/mensualidad")
public class MensualidadController {

    private final MensualidadService mensualidadService;
    private final ClienteService clienteService;
    private final PagoService pagoService;

    public MensualidadController(MensualidadService mensualidadService,
                                 ClienteService clienteService,
                                 PagoService pagoService) {
        this.mensualidadService = mensualidadService;
        this.clienteService = clienteService;
        this.pagoService = pagoService;
    }

    @GetMapping
    public String ver(@PathVariable Integer id, Model modelo, RedirectAttributes mensajes) {
        try {
            Cliente cliente = clienteService.obtener(id);
            String placas = clienteService.vehiculosDe(id).stream()
                .map(Vehiculo::getPlaca)
                .collect(Collectors.joining(", "));

            modelo.addAttribute("cliente", cliente);
            modelo.addAttribute("placas", placas);
            modelo.addAttribute("estado", clienteService.estadoMensualidad(id));
            modelo.addAttribute("info", mensualidadService.consultar(id));
            modelo.addAttribute("historial", mensualidadService.historial(id));
            modelo.addAttribute("pago", pagoService.consultar(id));
            modelo.addAttribute("medios", MedioPago.values());
            return "admin-mensualidad";
        } catch (DatosInvalidosException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/clientes";
        }
    }

    // CU-14
    @PostMapping("/registrar")
    public String registrar(@PathVariable Integer id,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
                            RedirectAttributes mensajes) {
        try {
            mensualidadService.registrar(id, inicio, fin);
            mensajes.addFlashAttribute("exito", "Mensualidad registrada correctamente.");
        } catch (DatosInvalidosException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/clientes/" + id + "/mensualidad";
    }

    // CU-15
    @PostMapping("/renovar")
    public String renovar(@PathVariable Integer id,
                          @RequestParam(required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
                          RedirectAttributes mensajes) {
        try {
            mensualidadService.renovar(id, fin);
            mensajes.addFlashAttribute("exito", "Mensualidad renovada correctamente.");
        } catch (DatosInvalidosException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/clientes/" + id + "/mensualidad";
    }

    // CU-25: registrar el pago de la mensualidad
    @PostMapping("/pago")
    public String registrarPago(@PathVariable Integer id,
                                @RequestParam(required = false) MedioPago medio,
                                @RequestParam(required = false) String referencia,
                                Authentication autenticacion,
                                RedirectAttributes mensajes) {
        try {
                        Pago pago = pagoService.registrar(id, medio, referencia, autenticacion.getName());
            mensajes.addFlashAttribute("exito", "Pago registrado correctamente.");
            return "redirect:/admin/pagos/" + pago.getIdPago();
        } catch (DatosInvalidosException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/clientes/" + id + "/mensualidad";
    }
}