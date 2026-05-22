package com.renfetrains.renfetrains.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO que detalla la información, horarios y estado de paso de una parada específica dentro del itinerario de un tren")
public record StopDetailDTO(
        @Schema(description = "Número de orden secuencial de la parada dentro del trayecto", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        int sequence,

        @Schema(description = "Identificador único de la estación (GTFS stop_id). Puede ser nulo si no se requiere.", example = "18000", nullable = true)
        String stopId,

        @Schema(description = "Nombre legible de la estación de parada", example = "Madrid-Chamartín", requiredMode = Schema.RequiredMode.REQUIRED)
        String stopName,

        @Schema(description = "Hora teórica o planificada de llegada en formato HH:mm:ss", example = "14:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
        String scheduledArrival,

        @Schema(description = "Hora real o estimada de llegada (calculada sumando los retrasos en tiempo real)", example = "14:35:00", requiredMode = Schema.RequiredMode.REQUIRED)
        String actualArrival,

        @Schema(description = "Indicador de si el tren ya ha efectuado la parada y ha reanudado su marcha. Útil para que el frontend mitigue visualmente (pinte en gris) las estaciones pasadas.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean isPassed
) {}