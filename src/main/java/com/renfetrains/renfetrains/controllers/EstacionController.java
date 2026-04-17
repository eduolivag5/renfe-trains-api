package com.renfetrains.renfetrains.controllers;

import com.renfetrains.renfetrains.entities.Estacion;
import com.renfetrains.renfetrains.repositories.EstacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estaciones")
public class EstacionController {

    @Autowired
    private EstacionRepository estacionRepository;

    // Endpoint de búsqueda para el buscador fluido
    @GetMapping("/search")
    public List<Estacion> search(@RequestParam("q") String query) {
        if (query.length() < 3) return List.of(); // No buscar hasta que haya 3 letras
        return estacionRepository.findByNombreContainingIgnoreCase(query);
    }

    @GetMapping
    public List<Estacion> getAll() {
        return estacionRepository.findAll();
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<Estacion> getById(@PathVariable String codigo) {
        return estacionRepository.findById(codigo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
