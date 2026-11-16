package com.renfetrains.renfetrains.controllers;

import com.renfetrains.renfetrains.entities.AlertGtfs;
import com.renfetrains.renfetrains.repositories.AlertGtfsRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/alertas")
@Tag(name = "Alertas", description = "Endpoints para consultar los avisos e incidencias del servicio en tiempo real (GTFS-RT)")
public class AlertController {

    @Autowired
    private AlertGtfsRepository alertRepository;

    @Operation(
            summary = "Obtener todas las alertas activas",
            description = "Devuelve todas las alertas vigentes sincronizadas desde el feed GTFS-RT de Renfe, " +
                    "ordenadas de más reciente a más antigua según su fecha de inicio. Cada alerta incluye las líneas afectadas."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de alertas (puede estar vacío si no hay incidencias activas)")
    })
    @GetMapping
    public List<AlertGtfs> getAll() {
        return alertRepository.findAllByOrderByFechaInicioDesc();
    }

    @Operation(
            summary = "Obtener el detalle de una alerta",
            description = "Devuelve los datos completos de una alerta a partir de su identificador de Renfe (alertId), " +
                    "incluyendo todas las líneas afectadas."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alerta encontrada correctamente"),
            @ApiResponse(responseCode = "404", description = "No existe ninguna alerta activa con el alertId proporcionado")
    })
    @GetMapping("/{alertId}")
    public ResponseEntity<AlertGtfs> getByAlertId(
            @Parameter(description = "Identificador de la alerta en el feed de Renfe", example = "AVISO_486178")
            @PathVariable String alertId) {
        return alertRepository.findWithRouteIdsByAlertId(alertId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Obtener las alertas de una línea",
            description = "Devuelve las alertas activas que afectan a la línea (routeId) indicada, " +
                    "ordenadas de más reciente a más antigua según su fecha de inicio."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de alertas de la línea (vacío si la línea no tiene incidencias activas)")
    })
    @GetMapping("/linea/{routeId}")
    public List<AlertGtfs> getByRouteId(
            @Parameter(description = "Identificador de la línea (route_id GTFS)", example = "10T0001C1")
            @PathVariable String routeId) {
        return alertRepository.findByRouteId(routeId);
    }
}
