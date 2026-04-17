package com.renfetrains.renfetrains.repositories;

import com.renfetrains.renfetrains.entities.TripUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TripUpdateRepository extends JpaRepository<TripUpdate, Long> {
    Optional<TripUpdate> findByTripId(String tripId);
}