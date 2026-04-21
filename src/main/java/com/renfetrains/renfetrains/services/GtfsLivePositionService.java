package com.renfetrains.renfetrains.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renfetrains.renfetrains.entities.TrainGtfsRealtime;
import com.renfetrains.renfetrains.repositories.TrainGtfsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

/**
 * SERVICIO DE POSICIONAMIENTO EN TIEMPO REAL - RENFE (GTFS-RT)
 * -------------------------------------------------------------------
 * Este servicio rastrea la ubicación exacta y el estado de los trenes en movimiento.
 */

@Service
public class GtfsLivePositionService {

    @Autowired
    private TrainGtfsRepository gtfsRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // --- Colores ANSI ---
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_RESET = "\u001B[0m";

    private static final String URL_CERCANIAS = "https://gtfsrt.renfe.com/vehicle_positions.json";
    private static final String URL_MEDIA_LARGA = "https://gtfsrt.renfe.com/vehicle_positions_LD.json";

    @Scheduled(fixedRate = 30000)
    public void fetchAllRealtimeData() {
        // Log de "Iniciando" eliminado para limpiar la consola
        procesarEndpoint(URL_CERCANIAS, "CERCANIAS");
        procesarEndpoint(URL_MEDIA_LARGA, "MEDIA_LARGA_DISTANCIA");
        limpiarTrenesAntiguos();
    }

    @Transactional
    public void limpiarTrenesAntiguos() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(5);
        try {
            gtfsRepository.deleteByUltimaActualizacionBefore(limite);
            System.out.println("[LOG] [GtfsLivePositionService] [" + ANSI_PURPLE + "GTFS-LIVE-CLEAN" + ANSI_RESET + "] " + ANSI_GREEN + "OK" + ANSI_RESET);
        } catch (Exception e) {
            System.err.println(ANSI_RED + "[LOG] [GtfsLivePositionService] [ERROR-LIVE-CLEAN] " + e.getMessage() + ANSI_RESET);
        }
    }

    @Transactional
    public void procesarEndpoint(String url, String tipoServicio) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("entity")) {
                    JsonNode entities = root.get("entity");
                    LocalDateTime ahora = LocalDateTime.now();

                    for (JsonNode node : entities) {
                        JsonNode vWrap = node.get("vehicle");
                        if (vWrap == null) continue;
                        JsonNode vehicleInfo = vWrap.get("vehicle");
                        if (vehicleInfo == null || !vehicleInfo.has("id")) continue;

                        String vId = vehicleInfo.get("id").asText();
                        TrainGtfsRealtime tren = gtfsRepository.findByVehicleId(vId).orElse(new TrainGtfsRealtime());

                        tren.setVehicleId(vId);
                        tren.setTipo(tipoServicio);
                        if (vWrap.has("trip")) tren.setTripId(vWrap.get("trip").path("tripId").asText());
                        if (vWrap.has("position")) {
                            tren.setLatitud(vWrap.get("position").path("latitude").asDouble());
                            tren.setLongitud(vWrap.get("position").path("longitude").asDouble());
                        }
                        tren.setStopId(vWrap.path("stopId").asText());
                        tren.setEstado(vWrap.path("currentStatus").asText());
                        tren.setLabel(vehicleInfo.path("label").asText());
                        tren.setUltimaActualizacion(ahora);
                        gtfsRepository.save(tren);
                    }
                    System.out.println("[LOG] [GtfsLivePositionService] [" + ANSI_PURPLE + "GTFS-LIVE-" + tipoServicio + ANSI_RESET + "] " + ANSI_GREEN + "OK" + ANSI_RESET + " (Trenes: " + entities.size() + ")");
                }
            }
        } catch (Exception e) {
            System.err.println(ANSI_RED + "[LOG] [GtfsLivePositionService] [ERROR-LIVE-" + tipoServicio + "] " + e.getMessage() + ANSI_RESET);
        }
    }
}