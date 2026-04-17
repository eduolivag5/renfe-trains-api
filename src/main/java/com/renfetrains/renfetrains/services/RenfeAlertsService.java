package com.renfetrains.renfetrains.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renfetrains.renfetrains.entities.AlertGtfs;
import com.renfetrains.renfetrains.repositories.AlertGtfsRepository;
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
public class RenfeAlertsService {

    @Autowired
    private AlertGtfsRepository alertRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Scheduled(fixedRate = 300000) // Cada 5 minutos es suficiente para alertas
    @Transactional
    public void fetchAlerts() {
        String url = "https://gtfsrt.renfe.com/alerts.json";

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode entities = root.path("entity");
                LocalDateTime ahora = LocalDateTime.now();
                List<String> currentAlertIds = new ArrayList<>();

                for (JsonNode node : entities) {
                    String aId = node.path("id").asText();
                    currentAlertIds.add(aId);

                    JsonNode alertNode = node.path("alert");

                    AlertGtfs alerta = alertRepository.findByAlertId(aId)
                            .orElse(new AlertGtfs());

                    alerta.setAlertId(aId);

                    // 1. Extraer descripción (buscamos la versión en español)
                    JsonNode translations = alertNode.path("descriptionText").path("translation");
                    for (JsonNode t : translations) {
                        if (t.path("language").asText().equals("es")) {
                            alerta.setDescripcion(t.path("text").asText());
                        }
                    }

                    // 2. Extraer fecha de inicio (Timestamp Unix)
                    long startTimestamp = alertNode.path("activePeriod").get(0).path("start").asLong();
                    alerta.setFechaInicio(LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(startTimestamp), ZoneId.systemDefault()));

                    // 3. Extraer líneas afectadas (routeIds)
                    List<String> routes = new ArrayList<>();
                    for (JsonNode info : alertNode.path("informedEntity")) {
                        if (info.has("routeId")) {
                            routes.add(info.get("routeId").asText());
                        }
                    }
                    alerta.setRouteIds(routes);
                    alerta.setUltimaActualizacion(ahora);

                    alertRepository.save(alerta);
                }

                // 4. Limpieza: Borrar alertas que ya no están activas en Renfe
                if (!currentAlertIds.isEmpty()) {
                    alertRepository.deleteByAlertIdNotIn(currentAlertIds);
                }
            }
        } catch (Exception e) {
            System.err.println("Error en fetchAlerts: " + e.getMessage());
        }
    }
}