package com.autoscolombia.parqueadero.excepcion;

public class MensualidadSinPagoException extends RuntimeException {
    public MensualidadSinPagoException(String mensaje) {
        super(mensaje);
    }
}