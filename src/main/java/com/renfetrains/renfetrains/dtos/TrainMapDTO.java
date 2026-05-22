package com.renfetrains.renfetrains.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO optimizado y ligero con las coordenadas y datos esenciales de un tren para su renderizado en tiempo real sobre el mapa")
public record TrainMapDTO(
        @Schema(description = "Identificador único del viaje (GTFS trip_id)", example = "TRAIN_AVE_9876", requiredMode = Schema.RequiredMode.REQUIRED)
        String tripId,

        @Schema(description = "Identificador físico o matrícula de la unidad de tren (vehicle_id)", example = "AVE-103-021", requiredMode = Schema.RequiredMode.REQUIRED)
        String vehicleId,

        @Schema(description = "Latitud de la posición geográfica actual del tren", example = "39.0012", requiredMode = Schema.RequiredMode.REQUIRED)
        double lat,

        @Schema(description = "Longitud de la posición geográfica actual del tren", example = "-1.8594", requiredMode = Schema.RequiredMode.REQUIRED)
        double lon,

        @Schema(description = "Tipo de servicio de tren (clave para asignar el icono o color en el mapa)", example = "AVE", requiredMode = Schema.RequiredMode.REQUIRED)
        String tipo,

        @Schema(description = "Etiqueta identificativa o código comercial del tren visible para el usuario", example = "AVE 05321", requiredMode = Schema.RequiredMode.REQUIRED)
        String label,

        @Schema(description = "Estado actual de circulación del vehículo", allowableValues = {"EN_MARCHA", "PARADO", "RETRASADO"}, example = "EN_MARCHA", requiredMode = Schema.RequiredMode.REQUIRED)
        String status
) {}