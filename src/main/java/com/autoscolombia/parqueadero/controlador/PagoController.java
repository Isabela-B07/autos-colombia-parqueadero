package com.autoscolombia.parqueadero.controlador;

import com.autoscolombia.parqueadero.excepcion.DatosInvalidosException;
import com.autoscolombia.parqueadero.modelo.MedioPago;
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
import java.util.List;

// Pagos: solo el administrador (la ruta /admin/** ya está protegida)
@Controller
@RequestMapping("/admin/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    // CU-29: historial de pagos con filtros
    @GetMapping
    public String historial(@RequestParam(required = false) String q,
                            @RequestParam(required = false) MedioPago medio,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                            Model modelo) {
        try {
            modelo.addAttribute("filas", pagoService.historial(q, medio, desde, hasta));
        } catch (DatosInvalidosException e) {
            modelo.addAttribute("filas", List.of());
            modelo.addAttribute("error", e.getMessage());
        }
        modelo.addAttribute("q", q == null ? "" : q);
        modelo.addAttribute("medioFiltro", medio == null ? "" : medio.name());
        modelo.addAttribute("desde", desde);
        modelo.addAttribute("hasta", hasta);
        modelo.addAttribute("medios", MedioPago.values());
        return "admin-pagos";
    }

    // CU-28: comprobante de un pago
    @GetMapping("/{id}")
    public String comprobante(@PathVariable Integer id, Model modelo,
                              RedirectAttributes mensajes) {
        try {
            modelo.addAttribute("c", pagoService.comprobante(id));
            return "admin-comprobante";
        } catch (DatosInvalidosException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/pagos";
        }
    }

    // CU-30: anular un pago
    @PostMapping("/{id}/anular")
    public String anular(@PathVariable Integer id,
                         @RequestParam(required = false) String motivo,
                         Authentication autenticacion,
                         RedirectAttributes mensajes) {
        try {
            pagoService.anular(id, motivo, autenticacion.getName());
            mensajes.addFlashAttribute("exito",
                "Pago N.º " + id + " anulado. La mensualidad vuelve a quedar pendiente de pago.");
        } catch (DatosInvalidosException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/pagos";
    }
}