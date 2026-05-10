package com.renfetrains.renfetrains.repositories;

import com.renfetrains.renfetrains.entities.TrainGtfsRealtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TrainGtfsRepository extends JpaRepository<TrainGtfsRealtime, Long> {

    Optional<TrainGtfsRealtime> findByVehicleId(String vehicleId);

    Optional<TrainGtfsRealtime> findByTripId(String tripId);

    Optional<TrainGtfsRealtime> findByTripIdIgnoreCase(String tripId);

    /**
     * Elimina los trenes cuya última actualización sea anterior a la fecha límite.
     * @Modifying es obligatorio para operaciones DELETE o UPDATE.
     * Devolvemos int para que el servicio pueda loguear cuántos trenes se borraron.
     */
    @Modifying
    @Transactional
    int deleteByUltimaActualizacionBefore(LocalDateTime limite);
}