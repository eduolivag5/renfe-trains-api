package com.renfetrains.renfetrains.controllers;

import com.renfetrains.renfetrains.dtos.*;
import com.renfetrains.renfetrains.entities.TrainGtfsRealtime;
import com.renfetrains.renfetrains.repositories.TrainGtfsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/trains")
@CrossOrigin(origins = "*")
public class TrainController {

    @Autowired
    private TrainGtfsRepository gtfsRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/live-map")
    public List<TrainMapDTO> getLiveTrainsForMap() {
        List<TrainGtfsRealtime> liveEntities = gtfsRepository.findAll();

        return liveEntities.stream()
                .filter(t -> t.getTripId() != null && !t.getTripId().isEmpty())
                .map(t -> new TrainMapDTO(
                        t.getTripId(),
                        t.getVehicleId(),
                        t.getLatitud(),
                        t.getLongitud(),
                        t.getTipo(),
                        t.getLabel(),
                        t.getEstado()
                ))
                .collect(Collectors.toList());
    }

    @GetMapping("/detail/{tripId}")
    public LiveTrainDTO getTrainDetail(@PathVariable String tripId) {
        // 1. Obtener la posición actual y el stop_id donde está/va el tren
        TrainGtfsRealtime entity = gtfsRepository.findByTripId(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tren no encontrado"));

        String currentStopId = entity.getStopId();

        // 2. Consulta SQL: Traemos el itinerario con stop_id para poder comparar
        String sql =
                "SELECT s.stop_id, s.name as stop_name, st.arrival_time, st.stop_sequence " +
                        "FROM stop_times st " +
                        "JOIN stops s ON st.stop_id = s.stop_id " +
                        "WHERE st.trip_id = ? " +
                        "ORDER BY st.stop_sequence ASC";

        List<FullStopInfo> allStops = jdbcTemplate.query(sql, (rs, rowNum) ->
                new FullStopInfo(
                        rs.getString("stop_id"),
                        rs.getString("stop_name"),
                        rs.getString("arrival_time"),
                        rs.getInt("stop_sequence")
                ), tripId);

        // 3. ENCONTRAR LA SECUENCIA ACTUAL
        // Buscamos en el itinerario qué 'stop_sequence' tiene el 'currentStopId' del tren
        int currentSequence = allStops.stream()
                .filter(s -> s.stopId().equals(currentStopId))
                .map(FullStopInfo::sequence)
                .findFirst()
                .orElse(-1); // Si no se encuentra, usamos -1 para que ninguna salga como pasada

        // 4. Obtener metadatos de la ruta (Nombre y Color)
        String routeName = "Trayecto Desconocido";
        String color = "808080";
        try {
            Map<String, Object> routeData = jdbcTemplate.queryForMap(
                    "SELECT r.long_name, r.color FROM trips t JOIN routes r ON t.route_id = r.route_id WHERE t.trip_id = ? LIMIT 1",
                    tripId
            );
            routeName = (String) routeData.get("long_name");
            color = (String) routeData.get("color");
        } catch (Exception ignored) {}

        // 5. Mapear a DTO marcando isPassed comparando secuencias
        List<StopDetailDTO> stopDetails = allStops.stream().map(s -> {
            // Si la secuencia de esta parada es menor a la secuencia de la parada actual del tren, ya ha pasado.
            boolean isPassed = (currentSequence != -1) && (s.sequence() < currentSequence);

            return new StopDetailDTO(
                    s.sequence(),
                    null,
                    s.stopName(),
                    s.arrivalTime(),
                    s.arrivalTime(),
                    isPassed
            );
        }).collect(Collectors.toList());

        return new LiveTrainDTO(
                entity.getTripId(),
                null,
                entity.getTipo(),
                routeName,
                "#" + (color != null ? color.replace("#", "") : "808080"),
                entity.getLatitud(),
                entity.getLongitud(),
                entity.getEstado(),
                0,
                currentStopId,
                stopDetails
        );
    }

    // Helper record para procesar la query con stop_id incluido
    private record FullStopInfo(String stopId, String stopName, String arrivalTime, int sequence) {}
}