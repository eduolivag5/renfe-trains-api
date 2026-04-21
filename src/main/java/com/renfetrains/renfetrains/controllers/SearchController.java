package com.renfetrains.renfetrains.controllers;

import com.renfetrains.renfetrains.dtos.TrainSearchResultDTO;
import com.renfetrains.renfetrains.repositories.TripRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
            @RequestParam(required = false) String startTime, // Ej: "08:00:00"
            @RequestParam(required = false) String type      // Ej: "AVE", "MD", "CERCANIAS"
    ) {
        LocalDate localDate = LocalDate.parse(date);
        String gtfsDate = localDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int dayOfWeek = localDate.getDayOfWeek().getValue();

        return tripRepository.findTrainsWithFilters(
                origin, destination, gtfsDate, dayOfWeek, startTime, type
        );
    }
}