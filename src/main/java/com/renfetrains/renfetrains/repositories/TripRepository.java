package com.renfetrains.renfetrains.repositories;

import com.renfetrains.renfetrains.entities.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, String> {}