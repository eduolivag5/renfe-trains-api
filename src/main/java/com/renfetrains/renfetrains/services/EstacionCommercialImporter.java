package com.renfetrains.renfetrains.services;

import com.opencsv.CSVReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class EstacionCommercialImporter {

    private final JdbcTemplate jdbcTemplate;
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_RESET = "\u001B[0m";

    private static final String SQL_ESTACIONES = "INSERT INTO estaciones (codigo, descripcion, latitud, longitud, direccion, cp, poblacion, provincia, pais, cercanias, feve, comun) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (codigo) DO UPDATE SET descripcion = EXCLUDED.descripcion, latitud = EXCLUDED.latitud, longitud = EXCLUDED.longitud, direccion = EXCLUDED.direccion, cp = EXCLUDED.cp, poblacion = EXCLUDED.poblacion, provincia = EXCLUDED.provincia, pais = EXCLUDED.pais, cercanias = EXCLUDED.cercanias, feve = EXCLUDED.feve, comun = EXCLUDED.comun";

    public EstacionCommercialImporter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void importCsv(InputStream is) {
        int total = 0;
        int lineCounter = 1; // Para rastrear la línea física del archivo en caso de error
        long startTime = System.currentTimeMillis();

        System.out.println("[LOG] [DATABASE-ESTACIONES] Iniciando lectura y parseo del CSV de estaciones comerciales...");
        com.opencsv.CSVParser parser = new com.opencsv.CSVParserBuilder().withSeparator(';').build();

        try (CSVReader reader = new com.opencsv.CSVReaderBuilder(new InputStreamReader(is, StandardCharsets.UTF_8))
                .withCSVParser(parser).build()) {

            reader.readNext(); // Saltar Cabecera
            lineCounter++;

            List<Object[]> batch = new ArrayList<>();

            String[] line;
            while ((line = reader.readNext()) != null) {
                lineCounter++;
                if (line.length < 2 || line[0].trim().isEmpty()) continue;

                String codigo = line[0].replace("\"", "").trim();
                String descripcion = line[1].replace("\"", "").trim();
                Double latitud = null; Double longitud = null;

                try {
                    if (line.length > 2 && !line[2].trim().isEmpty()) latitud = Double.parseDouble(line[2].trim());
                    if (line.length > 3 && !line[3].trim().isEmpty()) longitud = Double.parseDouble(line[3].trim());
                } catch (Exception e) {
                    System.err.println(ANSI_YELLOW + "[WARN-PARSE-ESTACIONES] Error al parsear coordenadas en línea " + lineCounter + " (Estación: " + codigo + "). Se insertará sin coordenadas." + ANSI_RESET);
                }

                String direccion = (line.length > 4) ? line[4].replace("\"", "").trim() : "";
                String cp = (line.length > 5) ? line[5].replace("\"", "").trim() : "";
                String poblacion = (line.length > 6) ? line[6].replace("\"", "").trim() : "";
                String provincia = (line.length > 7) ? line[7].replace("\"", "").trim() : "";
                String pais = (line.length > 8) ? line[8].replace("\"", "").trim() : "ESPAÑA";
                String cercanias = (line.length > 9) ? line[9].replace("\"", "").trim() : "NO";
                String feve = (line.length > 10) ? line[10].replace("\"", "").trim() : "NO";
                String comun = (line.length > 11) ? line[11].replace("\"", "").trim() : "NO";

                batch.add(new Object[]{ codigo, descripcion, latitud, longitud, direccion, cp, poblacion, provincia, pais, cercanias, feve, comun });

                if (batch.size() >= 2000) {
                    System.out.println("[LOG] [DATABASE-ESTACIONES] Volcando lote de " + batch.size() + " registros a PostgreSQL...");
                    total += jdbcTemplate.batchUpdate(SQL_ESTACIONES, batch).length;
                    batch.clear();
                }
            }

            // Volcado del remanente final
            if (!batch.isEmpty()) {
                System.out.println("[LOG] [DATABASE-ESTACIONES] Volcando último lote de " + batch.size() + " registros a PostgreSQL...");
                total += jdbcTemplate.batchUpdate(SQL_ESTACIONES, batch).length;
            }

            long duration = System.currentTimeMillis() - startTime;
            System.out.println("[LOG] [DATABASE-ESTACIONES] " + ANSI_GREEN + "Proceso terminado con éxito. Total insertadas/actualizadas: " + total + " en " + duration + " ms." + ANSI_RESET);

        } catch (Exception e) {
            System.err.println(ANSI_RED + "[ERROR-SAVE-ESTACIONES] Fallo crítico procesando el CSV (Línea estimada: " + lineCounter + "): " + e.getMessage() + ANSI_RESET);
        }
    }
}