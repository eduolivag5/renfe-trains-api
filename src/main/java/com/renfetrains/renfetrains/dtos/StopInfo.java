package com.renfetrains.renfetrains.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO simplificado que contiene la información básica y el horario de una parada específica")
public record StopInfo(
        @Schema(description = "Nombre legible de la estación de parada", example = "Albacete-Los Llanos", requiredMode = Schema.RequiredMode.REQUIRED)
        String stopName,

        @Schema(description = "Hora programada de llegada de la parada en formato HH:mm:ss", example = "16:45:00", requiredMode = Schema.RequiredMode.REQUIRED)
        String arrivalTime,

        @Schema(description = "Número de orden secuencial de la parada dentro del itinerario del viaje", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        int sequence
) {}