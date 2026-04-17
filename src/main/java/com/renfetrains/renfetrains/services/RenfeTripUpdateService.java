package com.renfetrains.renfetrains.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renfetrains.renfetrains.entities.StopTimeUpdate;
import com.renfetrains.renfetrains.entities.TripUpdate;
import com.renfetrains.renfetrains.repositories.TripUpdateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class RenfeTripUpdateService {

    @Autowired
    private TripUpdateRepository tripUpdateRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String URL_CERCANIAS = "https://gtfsrt.renfe.com/trip_updates.json";
    private static final String URL_LD = "https://gtfsrt.renfe.com/trip_updates_LD.json";

    @Scheduled(fixedRate = 60000) // Los retrasos cambian menos que la posición, cada minuto está bien
    public void fetchAllTripUpdates() {
        procesarEndpoint(URL_CERCANIAS, "CERCANIAS");
        procesarEndpoint(URL_LD, "MEDIA_LARGA_DISTANCIA");
    }

    @Transactional
    public void procesarEndpoint(String url, String tipo) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getBody() == null) return;

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode entities = root.path("entity");
            LocalDateTime ahora = LocalDateTime.now();

            for (JsonNode node : entities) {
                JsonNode tu = node.path("tripUpdate");
                String tId = tu.path("trip").path("tripId").asText();

                TripUpdate tripUpdate = tripUpdateRepository.findByTripId(tId)
                        .orElse(new TripUpdate());

                tripUpdate.setTripId(tId);
                tripUpdate.setTipo(tipo);
                tripUpdate.setUltimaActualizacion(ahora);

                // Retraso global (común en LD y a veces en Cercanías)
                if (tu.has("delay")) {
                    tripUpdate.setDelayGlobal(tu.get("delay").asInt());
                }

                // Procesar paradas (típico de Cercanías)
                List<StopTimeUpdate> stops = new ArrayList<>();
                if (tu.has("stopTimeUpdate")) {
                    for (JsonNode sNode : tu.get("stopTimeUpdate")) {
                        StopTimeUpdate stop = new StopTimeUpdate();
                        stop.setStopId(sNode.path("stopId").asText());

                        JsonNode arrival = sNode.path("arrival");
                        if (!arrival.isMissingNode()) {
                            stop.setDelay(arrival.path("delay").asInt());
                            long unixTime = arrival.path("time").asLong();
                            stop.setTiempoEstimado(LocalDateTime.ofInstant(
                                    Instant.ofEpochSecond(unixTime), ZoneId.systemDefault()));
                        }
                        stops.add(stop);
                    }
                }

                // Actualizamos la lista de paradas (JPA borrará las antiguas y pondrá las nuevas)
                tripUpdate.getStopTimeUpdates().clear();
                tripUpdate.getStopTimeUpdates().addAll(stops);

                tripUpdateRepository.save(tripUpdate);
            }
            System.out.println("Actualizados TripUpdates de " + tipo + ": " + entities.size());

        } catch (Exception e) {
            System.err.println("Error en TripUpdates " + tipo + ": " + e.getMessage());
        }
    }
}