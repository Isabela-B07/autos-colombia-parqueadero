package com.autoscolombia.parqueadero.repositorio;

import com.autoscolombia.parqueadero.modelo.Tarifa;
import com.autoscolombia.parqueadero.modelo.TipoVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarifaRepository extends JpaRepository<Tarifa, TipoVehiculo> {
}