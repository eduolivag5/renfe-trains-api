package com.renfetrains.renfetrains.repositories;

import com.renfetrains.renfetrains.entities.TrainGtfsRealtime;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TrainGtfsRepository extends JpaRepository<TrainGtfsRealtime, Long> {

    Optional<TrainGtfsRealtime> findByVehicleId(String vehicleId);

    // Búsqueda estándar (la que ya tenías)
    Optional<TrainGtfsRealtime> findByTripId(String tripId);

    /**
     * Busca un tren ignorando mayúsculas y minúsculas.
     * Esto soluciona problemas si el tripId viene del frontend en un formato
     * ligeramente distinto al de la base de datos.
     */
    Optional<TrainGtfsRealtime> findByTripIdIgnoreCase(String tripId);

    @Modifying
    @Transactional
    void deleteByUltimaActualizacionBefore(LocalDateTime limite);
}