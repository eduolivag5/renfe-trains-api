package com.renfetrains.renfetrains.repositories;

import com.renfetrains.renfetrains.entities.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, String> {

    Optional<Route> findByRouteId(String routeId);

}
