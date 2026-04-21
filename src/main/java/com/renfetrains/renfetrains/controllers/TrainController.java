package com.renfetrains.renfetrains.controllers;

import com.renfetrains.renfetrains.dtos.LiveTrainDTO;
import com.renfetrains.renfetrains.dtos.TrainMapDTO;
import com.renfetrains.renfetrains.services.TrainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trains")
public class TrainController {

    @Autowired
    private TrainService trainService;

    @GetMapping("/live-map")
    public List<TrainMapDTO> getLiveTrainsForMap() {
        return trainService.getLiveTrainsForMap();
    }

    @GetMapping("/detail/{tripId}")
    public LiveTrainDTO getTrainDetail(@PathVariable String tripId) {
        System.out.println("Buscando tren con ID: " + tripId);
        LiveTrainDTO dto = trainService.getTrainDetailWithProgress(tripId);
        if (dto == null) {
            System.out.println("El servicio devolvió NULL para el ID: " + tripId);
        }
        return dto;
    }
}