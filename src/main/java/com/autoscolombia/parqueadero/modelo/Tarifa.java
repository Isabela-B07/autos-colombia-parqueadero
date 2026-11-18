package com.autoscolombia.parqueadero.modelo;

import jakarta.persistence.*;

import java.math.BigDecimal;

// Valor mensual por tipo de vehículo (RF-32)
@Entity
@Table(name = "tarifa")
public class Tarifa {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo")
    private TipoVehiculo tipo;

    @Column(name = "valor", nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    public TipoVehiculo getTipo() { return tipo; }
    public void setTipo(TipoVehiculo tipo) { this.tipo = tipo; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
}