package com.renfetrains.renfetrains.controllers;

import com.renfetrains.renfetrains.dtos.LiveTrainDTO;
import com.renfetrains.renfetrains.dtos.TrainMapDTO;
import com.renfetrains.renfetrains.services.TrainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/trains")
@Tag(name = "Trenes", description = "Endpoints para la gestión y consulta de trenes en tiempo real")
public class TrainController {

    @Autowired
    private TrainService trainService;

    @Operation(
            summary = "Obtener trenes en tiempo real para el mapa",
            description = "Devuelve una lista con las coordenadas y estado de los trenes activos. Cuenta con una caché de 15 segundos."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de trenes obtenida correctamente")
    })
    @GetMapping("/live-map")
    public ResponseEntity<List<TrainMapDTO>> getLiveTrainsForMap() {
        List<TrainMapDTO> trains = trainService.getLiveTrainsForMap();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(15, TimeUnit.SECONDS))
                .body(trains);
    }

    @Operation(
            summary = "Obtener detalle de un tren específico",
            description = "Devuelve la información detallada y el progreso de un tren utilizando su identificador de viaje (tripId)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalles del tren encontrados"),
            @ApiResponse(responseCode = "404", description = "No se encontró ningún tren con el tripId proporcionado")
    })
    @GetMapping("/detail/{tripId}")
    public ResponseEntity<LiveTrainDTO> getTrainDetail(
            @Parameter(description = "ID único del viaje del tren", example = "TRAIN_MD_12345")
            @PathVariable String tripId) {

        LiveTrainDTO dto = trainService.getTrainDetailWithProgress(tripId);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.SECONDS))
                .body(dto);
    }
}