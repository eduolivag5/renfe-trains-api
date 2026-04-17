package com.renfetrains.renfetrains.repositories;

import com.renfetrains.renfetrains.entities.TrainGtfsRealtime;
import com.renfetrains.renfetrains.entities.TrainGtfsRealtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TrainGtfsRepository extends JpaRepository<TrainGtfsRealtime, Long> {
    Optional<TrainGtfsRealtime> findByVehicleId(String vehicleId);
}