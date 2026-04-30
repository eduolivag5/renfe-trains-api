package com.renfetrains.renfetrains.controllers;

import com.renfetrains.renfetrains.entities.Estacion;
import com.renfetrains.renfetrains.repositories.EstacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estaciones")
public class EstacionController {

    @Autowired
    private EstacionRepository estacionRepository;

    // CORRECCIÓN: Se añade PageRequest para limitar los resultados a 10.
    // Esto evita que búsquedas genéricas como "mad" devuelvan cientos de registros.
    @GetMapping("/search")
    public List<Estacion> search(@RequestParam("q") String query) {
        if (query.length() < 3) return List.of();
        return estacionRepository.findByNombreContainingIgnoreCase(query, PageRequest.of(0, 10));
    }

    // CORRECCIÓN: El método getAll() ha sido eliminado.
    // Descargar todas las estaciones en cada carga de página es el principal motivo de exceso de Egress.
    // Si necesitas una lista para un selector, usa el buscador superior.

    @GetMapping("/{codigo}")
    public ResponseEntity<Estacion> getById(@PathVariable String codigo) {
        return estacionRepository.findById(codigo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}