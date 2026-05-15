package com.renfetrains.renfetrains.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renfetrains.renfetrains.entities.TrainGtfsRealtime;
import com.renfetrains.renfetrains.repositories.TrainGtfsRepository;
import com.renfetrains.renfetrains.repositories.TripRepository;
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
public class GtfsLivePositionService {

    @Autowired
    private TrainGtfsRepository gtfsRepository;

    @Autowired
    private TripRepository tripRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Colores para logs
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_CYAN = "\u001B[36m";

    private static final String URL_CERCANIAS = "https://gtfsrt.renfe.com/vehicle_positions.json";
    private static final String URL_MEDIA_LARGA = "https://gtfsrt.renfe.com/vehicle_positions_LD.json";

    @Scheduled(fixedRate = 30000)
    public void fetchAllRealtimeData() {
        System.out.println(ANSI_PURPLE + ">>> [CRON] INICIANDO ACTUALIZACIÓN GTFS-RT <<<" + ANSI_RESET);
        procesarEndpoint(URL_CERCANIAS, "CERCANIAS");
        procesarEndpoint(URL_MEDIA_LARGA, "MEDIA_LARGA_DISTANCIA");
    }

    @Transactional
    public void procesarEndpoint(String url, String tipoServicio) {
        System.out.println("[LOG] Conectando a: " + tipoServicio + " (" + url + ")");

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getBody() == null) {
                System.out.println(ANSI_RED + "[ERROR] Respuesta vacía de la API" + ANSI_RESET);
                return;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode entities = root.path("entity");

            System.out.println("[LOG] Recibidas " + entities.size() + " entidades de " + tipoServicio);

            LocalDateTime ahora = LocalDateTime.now();
            int contadorMatch = 0;
            int contadorFallback = 0;

            for (JsonNode node : entities) {
                try {
                    boolean huboMatch = actualizarIndividual(node, tipoServicio, ahora);
                    if (huboMatch) contadorMatch++; else contadorFallback++;
                } catch (Exception e) {
                    System.err.println(ANSI_RED + "[ERROR-LOOP] Error en item: " + e.getMessage() + ANSI_RESET);
                }
            }

            System.out.println("[RESULTADO] [" + tipoServicio + "] " + ANSI_GREEN + "Matches: " + contadorMatch + ANSI_RESET + " | " + ANSI_YELLOW + "Fallbacks: " + contadorFallback + ANSI_RESET);

        } catch (Exception e) {
            System.err.println(ANSI_RED + "[FATAL-ERROR-" + tipoServicio + "] " + e.getMessage() + ANSI_RESET);
        }
    }

    private boolean actualizarIndividual(JsonNode node, String tipoServicio, LocalDateTime ahora) {
        JsonNode vWrap = node.get("vehicle");
        if (vWrap == null) return false;

        JsonNode vehicleInfo = vWrap.get("vehicle");
        if (vehicleInfo == null || !vehicleInfo.has("id")) return false;

        String vId = vehicleInfo.get("id").asText();
        TrainGtfsRealtime tren = gtfsRepository.findByVehicleId(vId).orElse(new TrainGtfsRealtime());
        tren.setVehicleId(vId);

        String rawTripId = vWrap.path("trip").path("tripId").asText(null);
        boolean found = false;

        if (rawTripId != null && !rawTripId.isEmpty()) {
            tren.setTripId(rawTripId);

            // Intentamos buscar el nombre del servicio (Route Short Name)
            String shortName = identificarServicio(rawTripId);

            if (shortName != null && !shortName.trim().isEmpty()) {
                tren.setTipo(shortName.trim());
                found = true;
            } else {
                tren.setTipo(tipoServicio);
            }
        } else {
            tren.setTipo(tipoServicio);
        }

        // Posición y otros datos
        tren.setLatitud(vWrap.path("position").path("latitude").asDouble());
        tren.setLongitud(vWrap.path("position").path("longitude").asDouble());
        tren.setStopId(vWrap.path("stopId").asText());
        tren.setEstado(vWrap.path("currentStatus").asText());
        tren.setLabel(vehicleInfo.path("label").asText());
        tren.setUltimaActualizacion(ahora);

        gtfsRepository.save(tren);
        return found;
    }

    /**
     * Lógica para encontrar el ShortName gestionando los trip_id dinámicos de Renfe.
     */
    private String identificarServicio(String tripId) {
        // 1. Intento búsqueda directa (Ideal para Cercanías o si el trip_id coincide exacto)
        String res = buscarShortNamePorTrip(tripId);
        if (res != null) return res;

        // 2. Si falla y parece ser LD/MD (contiene guion para la fecha o es muy largo)
        if (tripId.contains("-")) {
            // Extraemos la parte anterior al primer guion (ej: "0510132026-05-08" -> "051013")
            // A veces Renfe concatena el código del tren. Probamos con el prefijo.
            String baseTrip = tripId.split("-")[0];
            res = buscarShortNamePorTrip(baseTrip);
            if (res != null) return res;

            // 3. Caso extremo: Si el ID base sigue sin estar, pero el trip_id empieza por
            // el código de servicio (los primeros 5 dígitos suelen ser el número de tren)
            if (baseTrip.length() > 5) {
                res = buscarShortNamePorTrip(baseTrip.substring(0, 5));
            }
        }

        return res;
    }

    private String buscarShortNamePorTrip(String tripId) {
        try {
            return tripRepository.findByTripIdWithRoute(tripId)
                    .map(trip -> (trip.getRoute() != null) ? trip.getRoute().getShortName() : null)
                    .orElse(null);
        } catch (Exception e) {
            // Error silencioso en logs para no saturar si falla un match
            return null;
        }
    }

    @Scheduled(initialDelay = 5000, fixedRate = 1800000)
    @Transactional
    public void limpiarTrenesAntiguos() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(15);
        try {
            int borrados = gtfsRepository.deleteByUltimaActualizacionBefore(limite);
            System.out.println(ANSI_CYAN + "[CLEANUP] Borrados " + borrados + " trenes obsoletos" + ANSI_RESET);
        } catch (Exception e) {
            System.err.println(ANSI_RED + "[ERROR-CLEAN] " + e.getMessage() + ANSI_RESET);
        }
    }
}