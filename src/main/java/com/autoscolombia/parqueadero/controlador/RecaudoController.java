package com.autoscolombia.parqueadero.controlador;

import com.autoscolombia.parqueadero.excepcion.DatosInvalidosException;
import com.autoscolombia.parqueadero.servicio.RecaudoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

// Recaudo: solo el administrador (la ruta /admin/** ya está protegida)
@Controller
@RequestMapping("/admin/recaudo")
public class RecaudoController {

    private final RecaudoService recaudoService;

    public RecaudoController(RecaudoService recaudoService) {
        this.recaudoService = recaudoService;
    }

    @GetMapping
    public String ver(@RequestParam(required = false)
                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                      @RequestParam(required = false)
                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                      Model modelo) {
        // La primera vez que se entra (sin fechas) no se calcula nada
        if (desde != null || hasta != null) {
            try {
                modelo.addAttribute("recaudo", recaudoService.calcular(desde, hasta));
            } catch (DatosInvalidosException e) {
                modelo.addAttribute("error", e.getMessage());
            }
        }
        modelo.addAttribute("desde", desde);
        modelo.addAttribute("hasta", hasta);
        return "admin-recaudo";
    }
}