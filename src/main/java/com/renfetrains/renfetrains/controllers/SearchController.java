package com.renfetrains.renfetrains.controllers;

import com.renfetrains.renfetrains.dtos.TrainSearchResultDTO;
import com.renfetrains.renfetrains.repositories.TripRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private TripRepository tripRepository;

    @GetMapping("/trains")
    public List<TrainSearchResultDTO> searchTrains(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam String date,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String type,
            // CORRECCIÓN: Parámetros de paginación para limitar la salida de datos
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        LocalDate localDate = LocalDate.parse(date);
        String gtfsDate = localDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int dayOfWeek = localDate.getDayOfWeek().getValue();

        // IMPORTANTE: Asegúrate de que tu TripRepository ahora acepte un objeto Pageable
        return tripRepository.findTrainsWithFilters(
                origin, destination, gtfsDate, dayOfWeek, startTime, type, PageRequest.of(page, size)
        );
    }
}