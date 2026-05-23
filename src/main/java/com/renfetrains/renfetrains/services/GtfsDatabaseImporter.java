package com.renfetrains.renfetrains.services;

import com.opencsv.CSVReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Repository
public class GtfsDatabaseImporter {

    private final JdbcTemplate jdbcTemplate;
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_RESET = "\u001B[0m";

    // Queries SQL
    private static final String SQL_ROUTES = "INSERT INTO routes (route_id, short_name, long_name, route_type, color, text_color, source, tipo_tren) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (route_id) DO UPDATE SET short_name = EXCLUDED.short_name, long_name = EXCLUDED.long_name, tipo_tren = EXCLUDED.tipo_tren";
    private static final String SQL_TRIPS = "INSERT INTO trips (trip_id, service_id, route_id, headsign, wheelchair_accessible, shape_id) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (trip_id) DO NOTHING";
    private static final String SQL_STOPS = "INSERT INTO stops (stop_id, name, lat, lon, wheelchair_boarding, source) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (stop_id) DO UPDATE SET name = EXCLUDED.name";
    private static final String SQL_CALENDAR = "INSERT INTO calendar (service_id, monday, tuesday, wednesday, thursday, friday, saturday, sunday, start_date, end_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (service_id) DO UPDATE SET start_date = EXCLUDED.start_date, end_date = EXCLUDED.end_date";
    private static final String SQL_STOP_TIMES = "INSERT INTO stop_times (trip_id, arrival_time, departure_time, stop_id, stop_sequence, stop_headsign, pickup_type, drop_off_type, shape_dist_traveled) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT ON CONSTRAINT stop_times_pkey DO NOTHING";

    public GtfsDatabaseImporter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void limpiarDatosAntiguos(String source) {
        long startTime = System.currentTimeMillis();
        try {
            System.out.println("[LOG] [" + ANSI_PURPLE + "GTFS-CLEAN-" + source + ANSI_RESET + "] Iniciando borrado secuencial de datos antiguos...");

            int rowsStopTimesTrips = jdbcTemplate.update("DELETE FROM stop_times WHERE trip_id IN (SELECT trip_id FROM trips WHERE route_id IN (SELECT route_id FROM routes WHERE source = ?))", source);
            System.out.println("[LOG] [" + ANSI_PURPLE + "GTFS-CLEAN-" + source + ANSI_RESET + "] -> stop_times (por trips) eliminados: " + rowsStopTimesTrips);

            int rowsStopTimesStops = jdbcTemplate.update("DELETE FROM stop_times WHERE stop_id IN (SELECT stop_id FROM stops WHERE source = ?)", source);
            System.out.println("[LOG] [" + ANSI_PURPLE + "GTFS-CLEAN-" + source + ANSI_RESET + "] -> stop_times (por stops residuales) eliminados: " + rowsStopTimesStops);

            int rowsTrips = jdbcTemplate.update("DELETE FROM trips WHERE route_id IN (SELECT route_id FROM routes WHERE source = ?)", source);
            System.out.println("[LOG] [" + ANSI_PURPLE + "GTFS-CLEAN-" + source + ANSI_RESET + "] -> trips eliminados: " + rowsTrips);

            int rowsRoutes = jdbcTemplate.update("DELETE FROM routes WHERE source = ?", source);
            System.out.println("[LOG] [" + ANSI_PURPLE + "GTFS-CLEAN-" + source + ANSI_RESET + "] -> routes eliminadas: " + rowsRoutes);

            int rowsStops = jdbcTemplate.update("DELETE FROM stops WHERE source = ?", source);
            System.out.println("[LOG] [" + ANSI_PURPLE + "GTFS-CLEAN-" + source + ANSI_RESET + "] -> stops eliminadas: " + rowsStops);

            long duration = System.currentTimeMillis() - startTime;
            System.out.println("[LOG] [" + ANSI_PURPLE + "GTFS-CLEAN-" + source + ANSI_RESET + "] " + ANSI_GREEN + "OK (Todo limpio en " + duration + " ms)" + ANSI_RESET);
        } catch (Exception e) {
            System.err.println(ANSI_RED + "[ERROR-CLEAN-" + source + "] Falló el borrado en cascada manual: " + e.getMessage() + ANSI_RESET);
            throw e;
        }
    }

    public void saveRoutes(InputStream is, String source) {
        int total = 0;
        long startTime = System.currentTimeMillis();
        boolean isNacional = source.equals("NACIONAL");

        try (CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String[] line; reader.readNext(); // Saltar cabecera
            List<Object[]> batch = new ArrayList<>();

            while ((line = reader.readNext()) != null) {
                String id = line[0].trim();
                String sName = (isNacional ? line[2] : line[1]).trim();
                String lName = (isNacional ? line[3] : line[2]).trim();
                int type = Integer.parseInt((isNacional ? line[5] : line[3]).trim());
                String color = isNacional ? (line.length > 7 ? line[7].trim() : "FFFFFF") : (line.length > 4 ? line[4].trim() : "FFFFFF");
                String tColor = isNacional ? (line.length > 8 ? line[8].trim() : "000000") : (line.length > 5 ? line[5].trim() : "000000");

                batch.add(new Object[]{ id, sName, lName, type, color, tColor, source, sName });

                if (batch.size() >= 2000) {
                    total += jdbcTemplate.batchUpdate(SQL_ROUTES, batch).length;
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) total += jdbcTemplate.batchUpdate(SQL_ROUTES, batch).length;

            long duration = System.currentTimeMillis() - startTime;
            System.out.println("[LOG] [GTFS-ROUTES-" + source + "] " + ANSI_GREEN + "Total insertadas/actualizadas: " + total + " (" + duration + " ms)" + ANSI_RESET);
        } catch (Exception e) {
            System.err.println(ANSI_RED + "[ERROR-ROUTES-" + source + "] " + e.getMessage() + ANSI_RESET);
        }
    }

    public void saveStops(InputStream is, String source) {
        int total = 0;
        long startTime = System.currentTimeMillis();
        boolean isNacional = source.equals("NACIONAL");

        try (CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String[] line; reader.readNext();
            List<Object[]> batch = new ArrayList<>();

            while ((line = reader.readNext()) != null) {
                String id = line[0].trim();
                String name = isNacional ? line[2].trim() : line[1].trim();
                double lat = Double.parseDouble(isNacional ? line[4].trim() : line[2].trim());
                double lon = Double.parseDouble(isNacional ? line[5].trim() : line[3].trim());
                int wc = Integer.parseInt(isNacional ? (line.length > 11 ? line[11].trim() : "0") : (line.length > 4 ? line[4].trim() : "0"));

                batch.add(new Object[]{ id, name, lat, lon, wc, source });

                if (batch.size() >= 2000) {
                    total += jdbcTemplate.batchUpdate(SQL_STOPS, batch).length;
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) total += jdbcTemplate.batchUpdate(SQL_STOPS, batch).length;

            long duration = System.currentTimeMillis() - startTime;
            System.out.println("[LOG] [GTFS-STOPS-" + source + "] " + ANSI_GREEN + "Total insertadas/actualizadas: " + total + " (" + duration + " ms)" + ANSI_RESET);
        } catch (Exception e) {
            System.err.println(ANSI_RED + "[ERROR-STOPS-" + source + "] " + e.getMessage() + ANSI_RESET);
        }
    }

    public void saveTrips(InputStream is, String source) {
        int total = 0;
        long startTime = System.currentTimeMillis();
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
                try { wc = Integer.parseInt(isNacional ? (line.length > 8 ? line[8].trim() : "0") : (line.length > 4 ? line[4].trim() : "0")); } catch (Exception e) {}

                batch.add(new Object[]{ tId, sId, rId, head, wc, shape });

                if (batch.size() >= 2000) {
                    total += jdbcTemplate.batchUpdate(SQL_TRIPS, batch).length;
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) total += jdbcTemplate.batchUpdate(SQL_TRIPS, batch).length;

            long duration = System.currentTimeMillis() - startTime;
            System.out.println("[LOG] [GTFS-TRIPS-" + source + "] " + ANSI_GREEN + "Total insertados: " + total + " (" + duration + " ms)" + ANSI_RESET);
        } catch (Exception e) {
            System.err.println(ANSI_RED + "[ERROR-TRIPS-" + source + "] " + e.getMessage() + ANSI_RESET);
        }
    }

    public void saveCalendar(InputStream is, String source) {
        int total = 0;
        long startTime = System.currentTimeMillis();

        try (CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String[] line; reader.readNext();
            List<Object[]> batch = new ArrayList<>();

            while ((line = reader.readNext()) != null) {
                batch.add(new Object[]{
                        line[0].trim(), Integer.parseInt(line[1].trim()), Integer.parseInt(line[2].trim()),
                        Integer.parseInt(line[3].trim()), Integer.parseInt(line[4].trim()), Integer.parseInt(line[5].trim()),
                        Integer.parseInt(line[6].trim()), Integer.parseInt(line[7].trim()), line[8].trim(), line[9].trim()
                });
                if (batch.size() >= 2000) {
                    total += jdbcTemplate.batchUpdate(SQL_CALENDAR, batch).length;
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) total += jdbcTemplate.batchUpdate(SQL_CALENDAR, batch).length;

            long duration = System.currentTimeMillis() - startTime;
            System.out.println("[LOG] [GTFS-CALENDAR-" + source + "] " + ANSI_GREEN + "Total insertados: " + total + " (" + duration + " ms)" + ANSI_RESET);
        } catch (Exception e) {
            System.err.println(ANSI_RED + "[ERROR-CALENDAR-" + source + "] " + e.getMessage() + ANSI_RESET);
        }
    }

    public void saveStopTimes(InputStream is, String source) {
        int total = 0; int rowNum = 1;
        long startTime = System.currentTimeMillis();
        boolean isNacional = source.equals("NACIONAL");

        try (CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String[] line; reader.readNext();
            List<Object[]> batch = new ArrayList<>();

            System.out.println("[LOG] [GTFS-STOP_TIMES-" + source + "] Volcando registros en bloques de 2000 a la base de datos...");

            while ((line = reader.readNext()) != null) {
                rowNum++;
                try {
                    String tripId = line[0].trim();
                    String arr = line[1].trim().replace(" ", "0");
                    String dep = line[2].trim().replace(" ", "0");
                    String sId = line[3].trim();
                    int seq = Integer.parseInt(line[4].replace("\"", "").trim());
                    String head = null; int pick = 0, drop = 0; String dist = null;

                    if (isNacional) {
                        head = (line.length > 5 && !line[5].isEmpty()) ? line[5].trim() : null;
                        pick = (line.length > 6 && !line[6].isEmpty()) ? Integer.parseInt(line[6].trim()) : 0;
                        drop = (line.length > 7 && !line[7].isEmpty()) ? Integer.parseInt(line[7].trim()) : 0;
                        dist = (line.length > 8 && !line[8].isEmpty()) ? line[8].trim() : null;
                    }
                    batch.add(new Object[]{ tripId, arr, dep, sId, seq, head, pick, drop, dist });

                    if (batch.size() >= 2000) {
                        total += executeBatchWithRetry(batch, source, rowNum - 2000);
                        batch.clear();
                        if (total % 10000 == 0 || total % 20000 == 0) {
                            System.out.println("[LOG] [GTFS-STOP_TIMES-" + source + "] Progress: " + ANSI_CYAN + total + ANSI_RESET + " filas procesadas.");
                        }
                    }
                } catch (Exception e) {
                    System.err.println(ANSI_RED + "[PARSE-ERROR] Fila " + rowNum + " (" + source + "): " + e.getMessage() + ANSI_RESET);
                }
            }
            if (!batch.isEmpty()) total += executeBatchWithRetry(batch, source, rowNum - batch.size());

            long duration = (System.currentTimeMillis() - startTime) / 1000;
            System.out.println("[LOG] [" + ANSI_GREEN + "GTFS-STOP_TIMES-" + source + ANSI_RESET + "] " + ANSI_GREEN + "Carga Masiva Finalizada. Total: " + total + " guardados en " + duration + " segundos." + ANSI_RESET);
        } catch (Exception e) {
            System.err.println(ANSI_RED + "[FATAL-STOP_TIMES-" + source + "] " + e.getMessage() + ANSI_RESET);
        }
    }

    private int executeBatchWithRetry(List<Object[]> batch, String source, int startRow) {
        try {
            return jdbcTemplate.batchUpdate(SQL_STOP_TIMES, batch).length;
        } catch (Exception batchEx) {
            System.err.println(ANSI_YELLOW + "[WARN-BATCH] Error en lote completo cerca de la línea " + startRow + ". Activando inserción secuencial de rescate (Failsafe)..." + ANSI_RESET);
            int successCount = 0;
            for (int i = 0; i < batch.size(); i++) {
                try {
                    jdbcTemplate.update(SQL_STOP_TIMES, batch.get(i));
                    successCount++;
                } catch (Exception singleEx) {
                    System.err.println(ANSI_RED + "[DB-ERROR] Fila real en CSV: " + (startRow + i + 1) + " (" + source + "): " + singleEx.getMessage() + ANSI_RESET);
                }
            }
            return successCount;
        }
    }
}