package com.renfetrains.renfetrains.services;

import com.renfetrains.renfetrains.dtos.RenfeResponseDTO;
import com.renfetrains.renfetrains.dtos.TrenDTO;
import com.renfetrains.renfetrains.entities.TrenEnVivo;
import com.renfetrains.renfetrains.repositories.TrenRepository;
import org.springframework.beans.BeanUtils;
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
import java.util.ArrayList;
import java.util.List;

@Service
public class RenfeDataService {

    @Autowired
    private TrenRepository trenRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(fixedRate = 60000)
    @Transactional // Asegura que la operación masiva sea consistente
    public void fetchFlota() {
        String url = "https://tiempo-real.renfe.com/renfe-visor/flota.json?v=" + System.currentTimeMillis();

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            System.out.println("--- Sincronizando Posiciones en Tiempo Real ---");

            ResponseEntity<RenfeResponseDTO> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, RenfeResponseDTO.class
            );

            if (response.getBody() != null && response.getBody().getTrenes() != null) {
                List<TrenDTO> listaDtos = response.getBody().getTrenes();
                List<TrenEnVivo> trenesParaGuardar = new ArrayList<>();
                LocalDateTime ahora = LocalDateTime.now();

                for (TrenDTO dto : listaDtos) {
                    TrenEnVivo tren = new TrenEnVivo();
                    BeanUtils.copyProperties(dto, tren);

                    // --- LOGICA DE LIMPIEZA DE TRIP_ID ---
                    // Renfe envía tripId como "1390320260415" (ID + Fecha).
                    // El GTFS estático solo tiene "13903".
                    if (tren.getTripId() != null && tren.getTripId().length() > 5) {
                        // El formato suele ser: [ID Tren][AñoMesDía].
                        // Suponiendo que el año empieza por "202", cortamos ahí.
                        String idBruto = tren.getTripId();
                        int indexFecha = idBruto.indexOf("202");
                        if (indexFecha > 0) {
                            tren.setTripId(idBruto.substring(0, indexFecha).trim());
                        }
                    }

                    tren.setUltimaActualizacion(ahora);
                    trenesParaGuardar.add(tren);
                }

                // Guardado masivo: mucho más eficiente para el Pooler de Supabase
                trenRepository.saveAll(trenesParaGuardar);

                System.out.println("EXITO: " + trenesParaGuardar.size() + " trenes en vivo actualizados.");
            }

        } catch (Exception e) {
            System.err.println("ERROR en tiempo real: " + e.getMessage());
        }
    }
}