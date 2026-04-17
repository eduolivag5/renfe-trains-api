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

    private static final String SQL_ROUTES = "INSERT INTO routes (route_id, short_name, long_name, route_type, color, text_color, source, tipo_tren) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT (route_id) DO UPDATE SET " +
            "short_name = EXCLUDED.short_name, " +
            "long_name = EXCLUDED.long_name, " +
            "route_type = EXCLUDED.route_type, " +
            "color = EXCLUDED.color, " +
            "text_color = EXCLUDED.text_color, " +
            "source = EXCLUDED.source, " +
            "tipo_tren = EXCLUDED.tipo_tren";

    private static final String SQL_TRIPS = "INSERT INTO trips (trip_id, route_id, headsign, wheelchair_accessible, shape_id) VALUES (?, ?, ?, ?, ?) ON CONFLICT (trip_id) DO UPDATE SET route_id = EXCLUDED.route_id, headsign = EXCLUDED.headsign, wheelchair_accessible = EXCLUDED.wheelchair_accessible, shape_id = EXCLUDED.shape_id";
    private static final String SQL_STOPS = "INSERT INTO stops (stop_id, name, lat, lon, wheelchair_boarding, source) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (stop_id) DO UPDATE SET name = EXCLUDED.name, lat = EXCLUDED.lat, lon = EXCLUDED.lon, wheelchair_boarding = EXCLUDED.wheelchair_boarding, source = EXCLUDED.source";
    private static final String SQL_CALENDAR = "INSERT INTO calendar (service_id, monday, tuesday, wednesday, thursday, friday, saturday, sunday, start_date, end_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (service_id) DO UPDATE SET start_date = EXCLUDED.start_date, end_date = EXCLUDED.end_date";

    @Scheduled(initialDelay = 5000, fixedRate = 86400000)
    public void runSync() {
        String[] urls = {
                "https://ssl.renfe.com/gtransit/Fichero_AV_LD/google_transit.zip",
                "https://ssl.renfe.com/ftransit/Fichero_CER_FOMENTO/fomento_transit.zip"
        };

        for (String url : urls) {
            try {
                String source = url.contains("AV_LD") ? "NACIONAL" : "CERCANIAS";
                System.out.println("\n>>>> INICIANDO SINCRONIZACIÓN: " + source);

                byte[] zipBytes = restTemplate.getForObject(url, byte[].class);
                if (zipBytes != null) {
                    processZip(zipBytes, source);
                    System.out.println(">>>> FINALIZADO CON ÉXITO: " + source);
                }
            } catch (Exception e) {
                System.err.println(">>>> ERROR CRÍTICO EN " + url + ": " + e.getMessage());
            }
        }
    }

    private void processZip(byte[] zipBytes, String source) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                InputStream preventCloseIs = new FilterInputStream(zis) { @Override public void close() {} };

                if (name.contains("routes.txt")) saveRoutes(preventCloseIs, source);
                else if (name.contains("stops.txt")) saveStops(preventCloseIs, source);
                else if (name.contains("calendar.txt")) saveCalendar(preventCloseIs);
                else if (name.contains("trips.txt")) saveTrips(preventCloseIs, source);

                zis.closeEntry();
            }
        }
    }

    private void saveRoutes(InputStream is, String source) {
        int count = 0;
        boolean isNacional = source.equals("NACIONAL");

        try (CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String[] line;
            reader.readNext(); // Saltar cabecera
            List<Object[]> batch = new ArrayList<>();

            while ((line = reader.readNext()) != null) {
                try {
                    String id = line[0].replace("\"", "").trim();
                    String sName = (isNacional ? line[2] : line[1]).replace("\"", "").trim();
                    String lName = (isNacional ? line[3] : line[2]).replace("\"", "").trim();
                    int type = Integer.parseInt((isNacional ? line[5] : line[3]).replace("\"", "").trim());
                    String color = isNacional ? line[7].replace("\"", "").trim() : (line.length > 4 ? line[4].trim() : "FFFFFF");
                    String tColor = isNacional ? line[8].replace("\"", "").trim() : (line.length > 5 ? line[5].trim() : "000000");

                    // Usamos sName (short_name) directamente como tipo de tren
                    String tipoTren = sName;

                    // Si es cercanías y el short_name suele ser un número de línea (C1, C2...),
                    // podrías querer mantener la etiqueta "CERCANIAS" o dejar el sName.
                    if (!isNacional && (sName.length() <= 3)) {
                        tipoTren = "CERCANIAS " + sName;
                    }

                    batch.add(new Object[]{ id, sName, lName, type, color, tColor, source, tipoTren });
                } catch (Exception e) {
                    // Errores de parseo menores
                }
            }
            if (!batch.isEmpty()) {
                count = jdbcTemplate.batchUpdate(SQL_ROUTES, batch).length;
            }
        } catch (Exception e) {
            System.err.println("Error Routes: " + e.getMessage());
        }
        System.out.println("   [Routes] Ok: " + count + " (" + source + ") actualizado usando short_name.");
    }

    private void saveStops(InputStream is, String source) {
        int count = 0, errors = 0;
        boolean isNacional = source.equals("NACIONAL");

        try (CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String[] line; reader.readNext();
            List<Object[]> batch = new ArrayList<>();
            while ((line = reader.readNext()) != null) {
                try {
                    String id = line[0].trim();
                    String name = isNacional ? line[2].trim() : line[1].trim();
                    double lat = Double.parseDouble(isNacional ? line[4].trim() : line[2].trim());
                    double lon = Double.parseDouble(isNacional ? line[5].trim() : line[3].trim());
                    int wc = Integer.parseInt(isNacional ? line[11].trim() : (line.length > 4 ? line[4].trim() : "0"));

                    batch.add(new Object[]{ id, name, lat, lon, wc, source });
                } catch (Exception e) { errors++; }
            }
            if (!batch.isEmpty()) count = jdbcTemplate.batchUpdate(SQL_STOPS, batch).length;
        } catch (Exception e) { System.err.println("Error Stops: " + e.getMessage()); }
        System.out.println("   [Stops]  Ok: " + count + " | Errores: " + errors);
    }

    private void saveTrips(InputStream is, String source) {
        int total = 0, errors = 0;
        boolean isNacional = source.equals("NACIONAL");

        try (CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String[] line; reader.readNext();
            List<Object[]> batch = new ArrayList<>();
            while ((line = reader.readNext()) != null) {
                try {
                    String rId = line[0].trim();
                    String tId = line[2].trim();
                    String head = line[3].trim();
                    String shape = isNacional ? line[7].trim() : (line.length > 6 ? line[6].trim() : "");
                    int wc = Integer.parseInt(isNacional ? line[8].trim() : (line.length > 4 ? line[4].trim() : "0"));

                    batch.add(new Object[]{ tId, rId, head, wc, shape });
                    if (batch.size() >= 1000) {
                        total += jdbcTemplate.batchUpdate(SQL_TRIPS, batch).length;
                        batch.clear();
                    }
                } catch (Exception e) { errors++; }
            }
            if (!batch.isEmpty()) total += jdbcTemplate.batchUpdate(SQL_TRIPS, batch).length;
        } catch (Exception e) { System.err.println("Error Trips: " + e.getMessage()); }
        System.out.println("   [Trips]  Ok: " + total + " | Errores: " + errors);
    }

    private void saveCalendar(InputStream is) {
        int count = 0, errors = 0;
        try (CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String[] line; reader.readNext();
            List<Object[]> batch = new ArrayList<>();
            while ((line = reader.readNext()) != null) {
                try {
                    batch.add(new Object[]{
                            line[0].trim(), Integer.parseInt(line[1].trim()), Integer.parseInt(line[2].trim()),
                            Integer.parseInt(line[3].trim()), Integer.parseInt(line[4].trim()), Integer.parseInt(line[5].trim()),
                            Integer.parseInt(line[6].trim()), Integer.parseInt(line[7].trim()), line[8].trim(), line[9].trim()
                    });
                } catch (Exception e) { errors++; }
            }
            if (!batch.isEmpty()) count = jdbcTemplate.batchUpdate(SQL_CALENDAR, batch).length;
        } catch (Exception e) { System.err.println("Error Calendar: " + e.getMessage()); }
        System.out.println("   [Calendar] Ok: " + count + " | Errores: " + errors);
    }
}