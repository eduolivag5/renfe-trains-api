package com.renfetrains.renfetrains.services;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class RenfeFileDownloadService {

    // Configuramos un RestTemplate con timeout para evitar bloqueos infinitos
    private final RestTemplate restTemplate;

    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RESET = "\u001B[0m";

    public RenfeFileDownloadService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000); // 15 segundos máximo para conectar
        factory.setReadTimeout(60000);    // 60 segundos máximo para descargar los bytes
        this.restTemplate = new RestTemplate(factory);
    }

    public byte[] downloadFile(String url, String context) {
        try {
            System.out.println("[LOG] [DOWNLOAD] Iniciando descarga: " + context + " desde " + url);

            long startTime = System.currentTimeMillis();
            byte[] fileData = restTemplate.getForObject(url, byte[].class);
            long endTime = System.currentTimeMillis();

            if (fileData != null) {
                double megaBytes = (double) fileData.length / (1024 * 1024);
                long duration = (endTime - startTime) / 1000;

                System.out.println("[LOG] [DOWNLOAD] " + ANSI_GREEN + "¡Descarga completada con éxito!: " + context + ANSI_RESET);
                System.out.printf("[LOG] [DOWNLOAD] Detalles [%s]: %.2f MB descargados en %d segundos.%n", context, megaBytes, duration);
            } else {
                System.err.println(ANSI_RED + "[LOG] [ERROR-DOWNLOAD-" + context + "] El servidor respondió con un cuerpo vacío." + ANSI_RESET);
            }

            return fileData;

        } catch (Exception e) {
            System.err.println(ANSI_RED + "[LOG] [ERROR-DOWNLOAD-" + context + "] Error en la transferencia: " + e.getMessage() + ANSI_RESET);
            return null;
        }
    }
}