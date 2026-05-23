package com.renfetrains.renfetrains.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class GtfsZipProcessorService {

    private final GtfsDatabaseImporter databaseImporter;
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_RESET = "\u001B[0m";

    public GtfsZipProcessorService(GtfsDatabaseImporter databaseImporter) {
        this.databaseImporter = databaseImporter;
    }

    @Transactional
    public void process(byte[] zipBytes, String source) {
        if (zipBytes == null || zipBytes.length == 0) {
            System.err.println(ANSI_RED + "[ERROR-ZIP-PROCESS-" + source + "] Los bytes del archivo ZIP están vacíos o son nulos." + ANSI_RESET);
            return;
        }

        long totalStartTime = System.currentTimeMillis();
        System.out.println("[LOG] [ZIP-PROCESS-" + source + "] Iniciando procesamiento de los datos GTFS...");

        try {
            // 1. Limpieza de base de datos
            long cleanStartTime = System.currentTimeMillis();
            System.out.println("[LOG] [ZIP-PROCESS-" + source + "] Vaciando tablas antiguas para la fuente...");
            databaseImporter.limpiarDatosAntiguos(source);
            System.out.println("[LOG] [ZIP-PROCESS-" + source + "] " + ANSI_GREEN + "Limpieza completada en " + (System.currentTimeMillis() - cleanStartTime) + " ms." + ANSI_RESET);

            // PASADA 1: Maestros
            System.out.println("[LOG] [ZIP-PROCESS-" + source + "] " + ANSI_CYAN + "--- PASADA 1: Procesando entidades maestras ---" + ANSI_RESET);
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (name.contains("stop_times.txt")) {
                        zis.closeEntry();
                        continue;
                    }

                    InputStream bis = createUncloseableStream(zis);
                    long entryStartTime = System.currentTimeMillis();

                    if (name.contains("routes.txt")) {
                        System.out.println("[LOG] [ZIP-PROCESS-" + source + "] Leyendo e insertando 'routes.txt'...");
                        databaseImporter.saveRoutes(bis, source);
                        System.out.println("[LOG] [ZIP-PROCESS-" + source + "] " + ANSI_GREEN + "'routes.txt' guardado (" + (System.currentTimeMillis() - entryStartTime) + " ms)." + ANSI_RESET);
                    }
                    else if (name.contains("stops.txt")) {
                        System.out.println("[LOG] [ZIP-PROCESS-" + source + "] Leyendo e insertando 'stops.txt'...");
                        databaseImporter.saveStops(bis, source);
                        System.out.println("[LOG] [ZIP-PROCESS-" + source + "] " + ANSI_GREEN + "'stops.txt' guardado (" + (System.currentTimeMillis() - entryStartTime) + " ms)." + ANSI_RESET);
                    }
                    else if (name.contains("calendar.txt")) {
                        System.out.println("[LOG] [ZIP-PROCESS-" + source + "] Leyendo e insertando 'calendar.txt'...");
                        databaseImporter.saveCalendar(bis, source);
                        System.out.println("[LOG] [ZIP-PROCESS-" + source + "] " + ANSI_GREEN + "'calendar.txt' guardado (" + (System.currentTimeMillis() - entryStartTime) + " ms)." + ANSI_RESET);
                    }
                    else if (name.contains("trips.txt")) {
                        System.out.println("[LOG] [ZIP-PROCESS-" + source + "] Leyendo e insertando 'trips.txt'...");
                        databaseImporter.saveTrips(bis, source);
                        System.out.println("[LOG] [ZIP-PROCESS-" + source + "] " + ANSI_GREEN + "'trips.txt' guardado (" + (System.currentTimeMillis() - entryStartTime) + " ms)." + ANSI_RESET);
                    }
                    zis.closeEntry();
                }
            }

            // PASADA 2: Horarios (stop_times)
            System.out.println("[LOG] [ZIP-PROCESS-" + source + "] " + ANSI_CYAN + "--- PASADA 2: Procesando horarios pesados ---" + ANSI_RESET);
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().contains("stop_times.txt")) {
                        long stopTimesStartTime = System.currentTimeMillis();
                        System.out.println("[LOG] [ZIP-PROCESS-" + source + "] " + ANSI_YELLOW + "Iniciando la carga masiva de 'stop_times.txt' (Esto puede tardar)..." + ANSI_RESET);

                        databaseImporter.saveStopTimes(createUncloseableStream(zis), source);

                        long stopTimesDuration = (System.currentTimeMillis() - stopTimesStartTime) / 1000;
                        System.out.println("[LOG] [ZIP-PROCESS-" + source + "] " + ANSI_GREEN + "¡'stop_times.txt' procesado con éxito en " + stopTimesDuration + " segundos!" + ANSI_RESET);

                        zis.closeEntry();
                        break;
                    }
                    zis.closeEntry();
                }
            }

            long totalDuration = (System.currentTimeMillis() - totalStartTime) / 1000;
            System.out.println("[LOG] [GTFS-SYNC-" + source + "] " + ANSI_GREEN + "COMPLETADO EXITOSAMENTE en " + totalDuration + " segundos." + ANSI_RESET);
        } catch (Exception e) {
            System.err.println(ANSI_RED + "[ERROR-ZIP-PROCESS-" + source + "] Fallo crítico en el procesamiento del ZIP: " + e.getMessage() + ANSI_RESET);
            e.printStackTrace(); // Te vendrá genial ver el árbol de fallos si rompe OpenCSV internamente
        }
    }

    private InputStream createUncloseableStream(ZipInputStream zis) {
        return new BufferedInputStream(new FilterInputStream(zis) {
            @Override public void close() {} // Evita que OpenCSV cierre el flujo del ZIP entero
        });
    }
}