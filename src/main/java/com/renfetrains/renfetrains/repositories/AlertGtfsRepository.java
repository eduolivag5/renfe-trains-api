package com.renfetrains.renfetrains.repositories;

import com.renfetrains.renfetrains.entities.AlertGtfs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertGtfsRepository extends JpaRepository<AlertGtfs, Long> {
    Optional<AlertGtfs> findByAlertId(String alertId);

    // Opcional: Para borrar alertas antiguas que ya no vienen en el JSON
    void deleteByAlertIdNotIn(List<String> activeAlertIds);
}