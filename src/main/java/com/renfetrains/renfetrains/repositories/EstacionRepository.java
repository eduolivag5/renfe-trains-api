package com.renfetrains.renfetrains.repositories;

import com.renfetrains.renfetrains.entities.Estacion;
import org.springframework.data.domain.Pageable; // IMPORTANTE: Añadir esta importación
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstacionRepository extends JpaRepository<Estacion, String> {

    List<Estacion> findByProvinciaIgnoreCase(String provincia);

    // CORRECCIÓN: Se añade Pageable como segundo parámetro.
    // Esto permite que Spring Data JPA aplique el LIMIT y OFFSET en la consulta SQL.
    List<Estacion> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);
}