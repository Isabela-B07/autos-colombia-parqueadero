package com.autoscolombia.parqueadero.controlador;

import com.autoscolombia.parqueadero.excepcion.DatosInvalidosException;
import com.autoscolombia.parqueadero.servicio.TarifaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

// Tarifas de mensualidad: solo el administrador (la ruta /admin/** ya está protegida)
@Controller
@RequestMapping("/admin/tarifas")
public class TarifaController {

    private final TarifaService tarifaService;

    public TarifaController(TarifaService tarifaService) {
        this.tarifaService = tarifaService;
    }

    // CU-24: ver las tarifas actuales
    @GetMapping
    public String ver(Model modelo) {
        TarifaService.Tarifas tarifas = tarifaService.consultar();
        modelo.addAttribute("carro", texto(tarifas.carro()));
        modelo.addAttribute("moto", texto(tarifas.moto()));
        return "admin-tarifas";
    }

    // CU-24: guardar las tarifas
    @PostMapping
    public String guardar(@RequestParam(required = false) String carro,
                          @RequestParam(required = false) String moto,
                          Model modelo,
                          RedirectAttributes mensajes) {
        try {
            tarifaService.actualizar(numero("carro", carro), numero("moto", moto));
            mensajes.addFlashAttribute("exito", "Tarifas actualizadas correctamente.");
            return "redirect:/admin/tarifas";
        } catch (DatosInvalidosException e) {
            // Se conserva lo que el administrador escribió
            modelo.addAttribute("carro", carro == null ? "" : carro);
            modelo.addAttribute("moto", moto == null ? "" : moto);
            modelo.addAttribute("error", e.getMessage());
            return "admin-tarifas";
        }
    }

    // ---------- Auxiliares ----------

    private String texto(BigDecimal valor) {
        return valor == null ? "" : valor.stripTrailingZeros().toPlainString();
    }

    private BigDecimal numero(String nombre, String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(texto.trim());
        } catch (NumberFormatException e) {
            throw new DatosInvalidosException(
                "La tarifa de " + nombre + " no es un número válido.");
        }
    }
}