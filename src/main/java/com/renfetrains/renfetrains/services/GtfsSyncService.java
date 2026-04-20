package com.renfetrains.renfetrains.services;

import com.opencsv.CSVReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class GtfsSyncService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RestTemplate restTemplate = new RestTemplate();

    // SQL Statements
    private static final String SQL_ROUTES = "INSERT INTO routes (route_id, short_name, long_name, route_type, color, text_color, source, tipo_tren) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (route_id) DO UPDATE SET " +
            "short_name = EXCLUDED.short_name, long_name = EXCLUDED.long_name, tipo_tren = EXCLUDED.tipo_tren";

    private static final String SQL_TRIPS = "INSERT INTO trips (trip_id, service_id, route_id, headsign, wheelchair_accessible, shape_id) " +
            "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (trip_id) DO NOTHING";

    private static final String SQL_STOPS = "INSERT INTO stops (stop_id, name, lat, lon, wheelchair_boarding, source) " +
            "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (stop_id) DO UPDATE SET name = EXCLUDED.name";

    private static final String SQL_CALENDAR = "INSERT INTO calendar (service_id, monday, tuesday, wednesday, thursday, friday, saturday, sunday, start_date, end_date) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (service_id) DO UPDATE SET start_date = EXCLUDED.start_date, end_date = EXCLUDED.end_date";

    private static final String SQL_STOP_TIMES = "INSERT INTO stop_times " +
            "(trip_id, arrival_time, departure_time, stop_id, stop_sequence, stop_headsign, pickup_type, drop_off_type, shape_dist_traveled) " +
            "VALUES (?, CAST(? AS interval), CAST(? AS interval), ?, ?, ?, ?, ?, ?) ON CONFLICT (trip_id, stop_sequence) DO NOTHING";

    @Scheduled(initialDelay = 5000, fixedRate = 86400000)
    public void runSync() {
        String[] urls = {
                "https://ssl.renfe.com/gtransit/Fichero_AV_LD/google_transit.zip",
                "https://ssl.renfe.com/ftransit/Fichero_CER_FOMENTO/fomento_transit.zip"
        };

        for (String url : urls) {
            try {
                String source = url.contains("AV_LD") ? "NACIONAL" : "CERCANIAS";
                System.out.println("\n[SINC] >>>> INICIANDO FUENTE: " + source);

                byte[] zipBytes = restTemplate.getForObject(url, byte[].class);
                if (zipBytes != null) {
                    processZip(zipBytes, source);
                    System.out.println("[SINC] >>>> FINALIZADO CON ÉXITO: " + source);
                }
            } catch (Exception e) {
                System.err.println("[SINC] >>>> ERROR CRÍTICO EN " + url + ": " + e.getMessage());
            }
        }
    }

    private void processZip(byte[] zipBytes, String source) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                // Buffer intermedio para evitar que el CSVReader cierre el ZipInputStream prematuramente
                InputStream preventCloseIs = new FilterInputStream(zis) { @Override public void close() {} };
                // Envolvemos en BufferedInputStream para mejorar rendimiento de lectura secuencial
                BufferedInputStream bis = new BufferedInputStream(preventCloseIs);

                if (name.contains("routes.txt")) saveRoutes(bis, source);
                else if (name.contains("stops.txt")) saveStops(bis, source);
                else if (name.contains("calendar.txt")) saveCalendar(bis);
                else if (name.contains("trips.txt")) saveTrips(bis, source);
                else if (name.contains("stop_times.txt")) saveStopTimes(bis, source);

                zis.closeEntry();
            }
        }
    }

    private void saveRoutes(InputStream is, String source) {
        int count = 0;
        boolean isNacional = source.equals("NACIONAL");
        try (CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String[] line; reader.readNext();
            List<Object[]> batch = new ArrayList<>();
            while ((line = reader.readNext()) != null) {
                String id = line[0].trim();
                String sName = (isNacional ? line[2] : line[1]).trim();
                String lName = (isNacional ? line[3] : line[2]).trim();
                int type = Integer.parseInt((isNacional ? line[5] : line[3]).trim());
                String color = isNacional ? line[7].trim() : (line.length > 4 ? line[4].trim() : "FFFFFF");
                String tColor = isNacional ? line[8].trim() : (line.length > 5 ? line[5].trim() : "000000");
                batch.add(new Object[]{ id, sName, lName, type, color, tColor, source, sName });
            }
            if (!batch.isEmpty()) count = jdbcTemplate.batchUpdate(SQL_ROUTES, batch).length;
        } catch (Exception e) { System.err.println("   [Routes] Error: " + e.getMessage()); }
        System.out.println("   [Routes] OK: " + count);
    }

    private void saveStops(InputStream is, String source) {
        int count = 0;
        boolean isNacional = source.equals("NACIONAL");
        try (CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String[] line; reader.readNext();
            List<Object[]> batch = new ArrayList<>();
            while ((line = reader.readNext()) != null) {
                String id = line[0].trim();
                String name = isNacional ? line[2].trim() : line[1].trim();
                double lat = Double.parseDouble(isNacional ? line[4].trim() : line[2].trim());
                double lon = Double.parseDouble(isNacional ? line[5].trim() : line[3].trim());
                int wc = Integer.parseInt(isNacional ? line[11].trim() : (line.length > 4 ? line[4].trim() : "0"));
                batch.add(new Object[]{ id, name, lat, lon, wc, source });
            }
            if (!batch.isEmpty()) count = jdbcTemplate.batchUpdate(SQL_STOPS, batch).length;
        } catch (Exception e) { System.err.println("   [Stops] Error: " + e.getMessage()); }
        System.out.println("   [Stops] OK: " + count);
    }

    private void saveTrips(InputStream is, String source) {
        int total = 0;
        boolean isNacional = source.equals("NACIONAL");
        try (CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String[] line; reader.readNext();
            List<Object[]> batch = new ArrayList<>();
            while ((line = reader.readNext()) != null) {
                if (line.length < 3) continue;
                String rId = line[0].trim();
                String sId = line[1].trim();
                String tId = line[2].trim();
                String head = (line.length > 3) ? line[3].trim() : "";
                String shape = (isNacional && line.length > 7) ? line[7].trim() : "";
                int wc = 0;
                try { wc = Integer.parseInt(isNacional ? line[8].trim() : (line.length > 4 ? line[4].trim() : "0")); } catch (Exception e) {}

                batch.add(new Object[]{ tId, sId, rId, head, wc, shape });
                if (batch.size() >= 2000) {
                    total += jdbcTemplate.batchUpdate(SQL_TRIPS, batch).length;
                    batch.clear();
                    System.out.println("      ... procesando Trips: " + total);
                }
            }
            if (!batch.isEmpty()) total += jdbcTemplate.batchUpdate(SQL_TRIPS, batch).length;
        } catch (Exception e) { System.err.println("   [Trips] Error: " + e.getMessage()); }
        System.out.println("   [Trips] OK: " + total);
    }

    private void saveCalendar(InputStream is) {
        int count = 0;
        try (CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String[] line; reader.readNext();
            List<Object[]> batch = new ArrayList<>();
            while ((line = reader.readNext()) != null) {
                batch.add(new Object[]{
                        line[0].trim(), Integer.parseInt(line[1].trim()), Integer.parseInt(line[2].trim()),
                        Integer.parseInt(line[3].trim()), Integer.parseInt(line[4].trim()), Integer.parseInt(line[5].trim()),
                        Integer.parseInt(line[6].trim()), Integer.parseInt(line[7].trim()), line[8].trim(), line[9].trim()
                });
            }
            if (!batch.isEmpty()) count = jdbcTemplate.batchUpdate(SQL_CALENDAR, batch).length;
        } catch (Exception e) { System.err.println("   [Calendar] Error: " + e.getMessage()); }
        System.out.println("   [Calendar] OK: " + count);
    }

    private void saveStopTimes(InputStream is, String source) {
        int total = 0;
        boolean isNacional = source.equals("NACIONAL");
        System.out.println("      [StopTimes] Iniciando inserción...");

        try (CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String[] line;
            reader.readNext(); // Saltar cabecera
            List<Object[]> batch = new ArrayList<>();

            while ((line = reader.readNext()) != null) {
                try {
                    String tripId = line[0].trim();

                    // Limpieza de tiempos: Aseguramos formato HH:mm:ss
                    // Algunos GTFS traen " 0:30:00" o "25:00:00"
                    String arr = line[1].trim().replace(" ", "0");
                    String dep = line[2].trim().replace(" ", "0");

                    String sId = line[3].trim();

                    // Parseo robusto de stop_sequence
                    int seq = Integer.parseInt(line[4].replace("\"", "").trim());

                    // Columnas opcionales
                    String head = (isNacional && line.length >= 6) ? line[5].trim() : null;
                    String pick = (isNacional && line.length >= 7) ? line[6].trim() : "0";
                    String drop = (isNacional && line.length >= 8) ? line[7].trim() : "0";
                    String dist = (isNacional && line.length >= 9) ? line[8].trim() : null;

                    batch.add(new Object[]{ tripId, arr, dep, sId, seq, head, pick, drop, dist });

                    if (batch.size() >= 2500) { // Reducimos un poco el batch para mayor estabilidad
                        jdbcTemplate.batchUpdate(SQL_STOP_TIMES, batch);
                        total += batch.size();
                        batch.clear();
                        if (total % 10000 == 0) System.out.println("      ... procesados StopTimes: " + total);
                    }
                } catch (Exception e) {
                    // Errores de parseo de una línea no detienen el proceso
                }
            }
            if (!batch.isEmpty()) {
                jdbcTemplate.batchUpdate(SQL_STOP_TIMES, batch);
                total += batch.size();
            }
        } catch (Exception e) {
            System.err.println("   [StopTimes] Error crítico: " + e.getMessage());
            e.printStackTrace(); // Esto nos dirá si es un problema de columna o de datos
        }
        System.out.println("   [StopTimes] OK: " + total);
    }
}