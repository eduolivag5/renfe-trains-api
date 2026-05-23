package com.renfetrains.renfetrains.controllers;

import com.renfetrains.renfetrains.entities.Estacion;
import com.renfetrains.renfetrains.repositories.EstacionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/estaciones")
@Tag(name = "Estaciones", description = "Endpoints para la búsqueda y gestión de estaciones ferroviarias")
public class EstacionController {

    @Autowired
    private EstacionRepository estacionRepository;

    @Operation(
            summary = "Buscar estaciones por nombre",
            description = "Busca estaciones que contengan el texto proporcionado de forma insensible a mayúsculas/minúsculas. " +
                    "Requiere un mínimo de 3 caracteres y devuelve un máximo de 10 resultados para optimizar el rendimiento."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Búsqueda realizada con éxito (puede devolver una lista vacía si no hay coincidencias o si la consulta tiene menos de 3 caracteres)")
    })
    @GetMapping("/search")
    public List<Estacion> search(
            @Parameter(description = "Texto de búsqueda (mínimo 3 caracteres)", example = "Madrid")
            @RequestParam("q") String query) {
        if (query.length() < 3) return List.of();
        return estacionRepository.findByNombreContainingIgnoreCase(query, PageRequest.of(0, 10));
    }

    @Operation(
            summary = "Obtener una estación por su código",
            description = "Devuelve los datos completos de una estación específica utilizando su identificador único o código."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estación encontrada correctamente"),
            @ApiResponse(responseCode = "404", description = "No se encontró ninguna estación con el código proporcionado")
    })
    @GetMapping("/{codigo}")
    public ResponseEntity<Estacion> getById(
            @Parameter(description = "Código único de la estación", example = "18000")
            @PathVariable String codigo) {
        return estacionRepository.findById(codigo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}