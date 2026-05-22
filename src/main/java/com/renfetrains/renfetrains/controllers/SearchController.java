package com.renfetrains.renfetrains.controllers;

import com.renfetrains.renfetrains.dtos.TrainSearchResultDTO;
import com.renfetrains.renfetrains.repositories.TripRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@Tag(name = "Buscador", description = "Endpoints para la búsqueda avanzada de rutas y horarios de trenes")
public class SearchController {

    @Autowired
    private TripRepository tripRepository;

    @Operation(
            summary = "Buscar itinerarios de trenes con filtros",
            description = "Realiza una búsqueda paginada de trenes disponibles entre un origen y un destino para una fecha concreta. " +
                    "Internamente procesa la fecha para adaptarla al calendario GTFS y permite filtrar por hora de salida y tipo de tren."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Búsqueda procesada correctamente (puede retornar una lista vacía si no hay trayectos disponibles)")
    })
    @GetMapping("/trains")
    public List<TrainSearchResultDTO> searchTrains(
            @Parameter(description = "Código o ID de la estación de origen", required = true, example = "18000")
            @RequestParam String origin,

            @Parameter(description = "Código o ID de la estación de destino", required = true, example = "71801")
            @RequestParam String destination,

            @Parameter(description = "Fecha del viaje en formato estándar (YYYY-MM-DD)", required = true, example = "2026-06-04")
            @RequestParam String date,

            @Parameter(description = "Hora mínima de salida a partir de la cual buscar (HH:mm:ss)", required = false, example = "08:30:00")
            @RequestParam(required = false) String startTime,

            @Parameter(description = "Tipo de servicio de tren (ej. AVE, MD, ALVIA)", required = false, example = "AVE")
            @RequestParam(required = false) String type,

            @Parameter(description = "Número de página para la paginación (basado en índice 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Cantidad de registros por página", example = "15")
            @RequestParam(defaultValue = "15") int size
    ) {
        LocalDate localDate = LocalDate.parse(date);
        String gtfsDate = localDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int dayOfWeek = localDate.getDayOfWeek().getValue();

        return tripRepository.findTrainsWithFilters(
                origin, destination, gtfsDate, dayOfWeek, startTime, type, PageRequest.of(page, size)
        );
    }
}