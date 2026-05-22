package com.renfetrains.renfetrains.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "DTO que representa la información detallada de un tren activo en tiempo real junto con su itinerario")
public record LiveTrainDTO(
        @Schema(description = "Identificador único del viaje (GTFS trip_id)", example = "TRAIN_MD_12345", requiredMode = Schema.RequiredMode.REQUIRED)
        String tripId,

        @Schema(description = "Identificador de la línea o ruta ferroviaria", example = "LINEA_MD_R1", requiredMode = Schema.RequiredMode.REQUIRED)
        String routeId,

        @Schema(description = "Tipo de servicio de tren", example = "MD", requiredMode = Schema.RequiredMode.REQUIRED)
        String trainType,

        @Schema(description = "Nombre legible de la ruta del trayecto", example = "Madrid-Chamartín -> Jaén", requiredMode = Schema.RequiredMode.REQUIRED)
        String routeName,

        @Schema(description = "Código de color hexadecimal asociado a la línea para su renderizado en el mapa", example = "#FF5733")
        String color,

        @Schema(description = "Latitud de la posición actual del tren", example = "40.4721", requiredMode = Schema.RequiredMode.REQUIRED)
        double latitude,

        @Schema(description = "Longitud de la posición actual del tren", example = "-3.6823", requiredMode = Schema.RequiredMode.REQUIRED)
        double longitude,

        @Schema(description = "Estado actual del movimiento del tren", allowableValues = {"EN_MARCHA", "PARADO", "RETRASADO"}, example = "EN_MARCHA")
        String status,

        @Schema(description = "Minutos de retraso acumulados respecto al horario oficial", example = "5")
        int delayMinutes,

        @Schema(description = "ID de la próxima estación donde efectuará parada", example = "18000")
        String nextStopId,

        @Schema(description = "Lista detallada de las estaciones del itinerario, sus horarios y si ya han sido visitadas")
        List<StopDetailDTO> itinerary
) {}