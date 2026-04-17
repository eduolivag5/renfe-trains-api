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

@Service
public class RenfeGtfsService {

    @Autowired
    private TrainGtfsRepository gtfsRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String URL_CERCANIAS = "https://gtfsrt.renfe.com/vehicle_positions.json";
    private static final String URL_MEDIA_LARGA = "https://gtfsrt.renfe.com/vehicle_positions_LD.json";

    @Scheduled(fixedRate = 30000)
    public void fetchAllRealtimeData() {
        System.out.println("--- Iniciando sincronización global Renfe ---");
        procesarEndpoint(URL_CERCANIAS, "CERCANIAS");
        procesarEndpoint(URL_MEDIA_LARGA, "MEDIA_LARGA_DISTANCIA");
    }

    @Transactional
    public void procesarEndpoint(String url, String tipoServicio) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.set("Accept", "application/json");
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

                        // Buscamos por vehicleId para hacer Upsert
                        TrainGtfsRealtime tren = gtfsRepository.findByVehicleId(vId)
                                .orElse(new TrainGtfsRealtime());

                        tren.setVehicleId(vId);
                        tren.setTipo(tipoServicio);

                        if (vWrap.has("trip")) {
                            tren.setTripId(vWrap.get("trip").path("tripId").asText());
                        }

                        if (vWrap.has("position")) {
                            tren.setLatitud(vWrap.get("position").path("latitude").asDouble());
                            tren.setLongitud(vWrap.get("position").path("longitude").asDouble());
                        }

                        // Campos adicionales del nuevo formato LD
                        tren.setStopId(vWrap.path("stopId").asText());
                        tren.setEstado(vWrap.path("currentStatus").asText());
                        tren.setLabel(vehicleInfo.path("label").asText());
                        tren.setUltimaActualizacion(ahora);

                        gtfsRepository.save(tren);
                    }
                    System.out.println("ÉXITO [" + tipoServicio + "]: " + entities.size() + " trenes.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error en " + tipoServicio + ": " + e.getMessage());
        }
    }
}