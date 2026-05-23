package com.renfetrains.renfetrains.scheduler;

import com.renfetrains.renfetrains.services.RenfeFileDownloadService;
import com.renfetrains.renfetrains.services.GtfsZipProcessorService;
import com.renfetrains.renfetrains.services.EstacionCommercialImporter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.io.ByteArrayInputStream;

@Component
public class GtfsOrchestratorScheduler {

    private final RenfeFileDownloadService downloadService;
    private final GtfsZipProcessorService gtfsProcessorService;
    private final EstacionCommercialImporter estacionesImporter;

    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_RESET = "\u001B[0m";

    public GtfsOrchestratorScheduler(RenfeFileDownloadService downloadService,
                                     GtfsZipProcessorService gtfsProcessorService,
                                     EstacionCommercialImporter estacionesImporter) {
        this.downloadService = downloadService;
        this.gtfsProcessorService = gtfsProcessorService;
        this.estacionesImporter = estacionesImporter;
    }

    @Scheduled(initialDelay = 0, fixedRate = 86400000) // Cada 24 horas
    public void runSync() {
        long orchestratorStartTime = System.currentTimeMillis();

        System.out.println("\n" + ANSI_CYAN + "======================================================================" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "[ORCHESTRATOR] [START] Iniciando Ciclo Global de Sincronización Renfe GTFS" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "======================================================================" + ANSI_RESET);

        String[] gtfsUrls = {
                "https://ssl.renfe.com/gtransit/Fichero_AV_LD/google_transit.zip",
                "https://ssl.renfe.com/ftransit/Fichero_CER_FOMENTO/fomento_transit.zip"
        };

        // 1. Sincronizar archivos GTFS (Zips)
        for (int i = 0; i < gtfsUrls.length; i++) {
            String url = gtfsUrls[i];
            String source = url.contains("AV_LD") ? "NACIONAL" : "CERCANIAS";

            System.out.println("\n[ORCHESTRATOR] [PASO 1." + (i + 1) + "] Procesando fuente GTFS: " + source);

            byte[] zipBytes = downloadService.downloadFile(url, source);
            if (zipBytes != null && zipBytes.length > 0) {
                System.out.println("[ORCHESTRATOR] Archivo descargado con éxito. Enviando a procesador de ZIP...");
                gtfsProcessorService.process(zipBytes, source);
            } else {
                System.err.println(ANSI_RED + "[ORCHESTRATOR] [ERROR] Saltando paso " + source + " porque la descarga devolvió datos vacíos." + ANSI_RESET);
            }
        }

        // 2. Sincronizar Maestro de Estaciones Comerciales (CSV)
        System.out.println("\n[ORCHESTRATOR] [PASO 2] Procesando Maestro de Estaciones Comerciales (CSV)...");
        String csvUrl = "https://ssl.renfe.com/ftransit/Fichero_estaciones/estaciones.csv";

        byte[] csvBytes = downloadService.downloadFile(csvUrl, "ESTACIONES-CSV");
        if (csvBytes != null && csvBytes.length > 0) {
            System.out.println("[ORCHESTRATOR] CSV descargado con éxito. Enviando al importador de base de datos...");
            estacionesImporter.importCsv(new ByteArrayInputStream(csvBytes));
        } else {
            System.err.println(ANSI_RED + "[ORCHESTRATOR] [ERROR] No se pudo importar el CSV de estaciones comerciales porque el archivo está vacío." + ANSI_RESET);
        }

        long totalDurationSeconds = (System.currentTimeMillis() - orchestratorStartTime) / 1000;
        long minutes = totalDurationSeconds / 60;
        long seconds = totalDurationSeconds % 60;

        System.out.println("\n" + ANSI_GREEN + "======================================================================" + ANSI_RESET);
        System.out.printf(ANSI_GREEN + "[ORCHESTRATOR] [END] Sincronización Finalizada con éxito en %d min y %d seg.%n", minutes, seconds);
        System.out.println(ANSI_GREEN + "======================================================================" + ANSI_RESET + "\n");
    }
}