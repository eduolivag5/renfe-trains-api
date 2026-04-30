package com.renfetrains.renfetrains.controllers;

import com.renfetrains.renfetrains.dtos.LiveTrainDTO;
import com.renfetrains.renfetrains.dtos.TrainMapDTO;
import com.renfetrains.renfetrains.services.TrainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/trains")
public class TrainController {

    @Autowired
    private TrainService trainService;

    @GetMapping("/live-map")
    public ResponseEntity<List<TrainMapDTO>> getLiveTrainsForMap() {
        List<TrainMapDTO> trains = trainService.getLiveTrainsForMap();

        // CORRECCIÓN: Se añade CacheControl de 15 segundos.
        // Durante ese tiempo, el navegador usará la copia local y no pedirá datos a Supabase.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(15, TimeUnit.SECONDS))
                .body(trains);
    }

    @GetMapping("/detail/{tripId}")
    public ResponseEntity<LiveTrainDTO> getTrainDetail(@PathVariable String tripId) {
        LiveTrainDTO dto = trainService.getTrainDetailWithProgress(tripId);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }

        // CORRECCIÓN: Cache corta para detalles del tren para reducir consultas repetitivas
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.SECONDS))
                .body(dto);
    }
}