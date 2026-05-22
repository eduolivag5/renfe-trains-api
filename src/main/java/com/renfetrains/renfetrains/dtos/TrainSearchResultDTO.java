package com.renfetrains.renfetrains.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO que representa un resultado individual de trayecto disponible en la búsqueda de trenes")
public record TrainSearchResultDTO(
        @Schema(description = "Identificador único del viaje (GTFS trip_id)", example = "TRAIN_ALVIA_3421", requiredMode = Schema.RequiredMode.REQUIRED)
        String tripId,

        @Schema(description = "Hora de salida programada desde la estación de origen (HH:mm:ss)", example = "08:15:00", requiredMode = Schema.RequiredMode.REQUIRED)
        String departureTime,

        @Schema(description = "Hora de llegada programada a la estación de destino (HH:mm:ss)", example = "11:45:00", requiredMode = Schema.RequiredMode.REQUIRED)
        String arrivalTime,

        @Schema(description = "Tipo de servicio de tren comercial", example = "ALVIA", requiredMode = Schema.RequiredMode.REQUIRED)
        String trainType,

        @Schema(description = "Nombre descriptivo de la ruta completa del tren", example = "Madrid-Atocha -> Alicante-Terminal", requiredMode = Schema.RequiredMode.REQUIRED)
        String routeName
) {}