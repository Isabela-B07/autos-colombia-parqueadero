package com.autoscolombia.parqueadero.repositorio;

import com.autoscolombia.parqueadero.modelo.EstadoPago;
import com.autoscolombia.parqueadero.modelo.MedioPago;
import com.autoscolombia.parqueadero.modelo.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Integer> {

    Optional<Pago> findFirstByMensualidadIdMensualidadAndEstado(Integer idMensualidad, EstadoPago estado);

    boolean existsByMensualidadIdMensualidadAndEstado(Integer idMensualidad, EstadoPago estado);

    // Historial con filtros opcionales; "hasta" es exclusivo
    @Query("""
        SELECT p FROM Pago p
        WHERE (:texto IS NULL
               OR LOWER(p.mensualidad.cliente.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR p.mensualidad.cliente.documento LIKE CONCAT('%', :texto, '%'))
          AND (:medio IS NULL OR p.medio = :medio)
          AND (:desde IS NULL OR p.fechaHora >= :desde)
          AND (:hasta IS NULL OR p.fechaHora < :hasta)
        ORDER BY p.fechaHora DESC, p.idPago DESC
        """)
    List<Pago> filtrar(@Param("texto") String texto,
                       @Param("medio") MedioPago medio,
                       @Param("desde") LocalDateTime desde,
                       @Param("hasta") LocalDateTime hasta);

    // Recaudo: solo pagos activos dentro del rango; "hasta" es exclusivo
    @Query("""
        SELECT p FROM Pago p
        WHERE p.estado = com.autoscolombia.parqueadero.modelo.EstadoPago.ACTIVO
          AND p.fechaHora >= :desde
          AND p.fechaHora < :hasta
        """)
    List<Pago> activosEntre(@Param("desde") LocalDateTime desde,
                            @Param("hasta") LocalDateTime hasta);
}