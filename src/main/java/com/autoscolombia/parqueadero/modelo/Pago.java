package com.autoscolombia.parqueadero.modelo;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Pago de una mensualidad (RF-33). El número de comprobante es el idPago (RF-36).
@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Integer idPago;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_mensualidad", nullable = false)
    private Mensualidad mensualidad;

    @Column(name = "valor", nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio", nullable = false)
    private MedioPago medio;

    @Column(name = "referencia", length = 50)
    private String referencia;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_empleado", nullable = false)
    private Empleado empleado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPago estado = EstadoPago.ACTIVO;

    @Column(name = "motivo_anulacion", length = 255)
    private String motivoAnulacion;

    @Column(name = "fecha_hora_anulacion")
    private LocalDateTime fechaHoraAnulacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado_anulacion")
    private Empleado empleadoAnulacion;

    public boolean estaActivo() {
        return estado == EstadoPago.ACTIVO;
    }

    public Integer getIdPago() { return idPago; }
    public void setIdPago(Integer idPago) { this.idPago = idPago; }

    public Mensualidad getMensualidad() { return mensualidad; }
    public void setMensualidad(Mensualidad mensualidad) { this.mensualidad = mensualidad; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public MedioPago getMedio() { return medio; }
    public void setMedio(MedioPago medio) { this.medio = medio; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public Empleado getEmpleado() { return empleado; }
    public void setEmpleado(Empleado empleado) { this.empleado = empleado; }

    public EstadoPago getEstado() { return estado; }
    public void setEstado(EstadoPago estado) { this.estado = estado; }

    public String getMotivoAnulacion() { return motivoAnulacion; }
    public void setMotivoAnulacion(String motivoAnulacion) { this.motivoAnulacion = motivoAnulacion; }

    public LocalDateTime getFechaHoraAnulacion() { return fechaHoraAnulacion; }
    public void setFechaHoraAnulacion(LocalDateTime fechaHoraAnulacion) { this.fechaHoraAnulacion = fechaHoraAnulacion; }

    public Empleado getEmpleadoAnulacion() { return empleadoAnulacion; }
    public void setEmpleadoAnulacion(Empleado empleadoAnulacion) { this.empleadoAnulacion = empleadoAnulacion; }
}