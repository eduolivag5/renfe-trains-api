package com.renfetrains.renfetrains.services;

import com.renfetrains.renfetrains.dtos.*;
import com.renfetrains.renfetrains.entities.StopTime;
import com.renfetrains.renfetrains.entities.TrainGtfsRealtime;
import com.renfetrains.renfetrains.repositories.StopTimeRepository;
import com.renfetrains.renfetrains.repositories.TrainGtfsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TrainService {

    @Autowired
    private TrainGtfsRepository gtfsRepository;

    @Autowired
    private StopTimeRepository stopTimeRepository;

    public List<TrainMapDTO> getLiveTrainsForMap() {
        return gtfsRepository.findAll().stream()
                .filter(t -> t.getTripId() != null
                        && t.getLatitud() != null
                        && t.getLongitud() != null) // <--- Añade esto
                .map(t -> new TrainMapDTO(
                        t.getTripId(),
                        t.getVehicleId(),
                        t.getLatitud(),
                        t.getLongitud(),
                        t.getTipo(),
                        t.getLabel(),
                        t.getEstado()
                )).collect(Collectors.toList());
    }

    public LiveTrainDTO getTrainDetailWithProgress(String tripId) {
        String cleanId = tripId.trim();

        // 1. Intentar buscar los datos de tiempo real (Opcional)
        Optional<TrainGtfsRealtime> liveOpt = gtfsRepository.findByTripIdIgnoreCase(cleanId);

        // 2. Obtener el itinerario (Esto viene de StopTime + Trips, SIEMPRE debería existir si el ID es de 'trips')
        List<StopTime> itinerary = stopTimeRepository.findItineraryWithStops(cleanId);

        if (itinerary.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No se encontró el itinerario para el viaje: " + cleanId);
        }

        // 3. Calcular progreso si hay datos en tiempo real
        int currentSeq = -1;
        String currentStopId = "";
        if (liveOpt.isPresent()) {
            currentStopId = liveOpt.get().getStopId();
            final String finalStopId = currentStopId;
            currentSeq = itinerary.stream()
                    .filter(st -> st.getStop().getStopId().equals(finalStopId))
                    .map(StopTime::getStopSequence)
                    .findFirst().orElse(-1);
        }

        // 4. Mapear paradas
        int finalCurrentSeq = currentSeq;
        List<StopDetailDTO> stops = itinerary.stream().map(st -> new StopDetailDTO(
                st.getStopSequence(),
                st.getStop().getStopId(),
                st.getStop().getName(),
                st.getArrivalTime(),
                st.getDepartureTime(),
                (finalCurrentSeq != -1 && st.getStopSequence() < finalCurrentSeq)
        )).collect(Collectors.toList());

        // 5. Construir objeto final (usando datos de Live si existen, sino valores por defecto)
        return liveOpt.map(live -> new LiveTrainDTO(
                live.getTripId(), live.getVehicleId(), live.getTipo(), live.getLabel(), "#E30613",
                live.getLatitud(), live.getLongitud(), live.getEstado(), finalCurrentSeq, live.getStopId(), stops
        )).orElseGet(() -> new LiveTrainDTO(
                cleanId, null, "PROGRAMADO", "Tren " + cleanId, "#808080",
                0.0, 0.0, "No disponible", -1, "", stops
        ));
    }
}