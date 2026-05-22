package com.renfetrains.renfetrains.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "DTO detallado que mapea las propiedades en tiempo real de un tren individual proveniente del feed de datos")
public class TrenDTO {

    @Schema(description = "Identificador único del viaje (GTFS trip_id)", example = "TRAIN_AVE_9876", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tripId;

    @Schema(description = "Código comercial o número del tren", example = "05321")
    private String codTren;

    @Schema(description = "Código identificador de la línea o ruta", example = "L-02")
    private String codLinea;

    @Schema(description = "Minutos de retraso acumulados (mapeado como String desde el origen de datos)", example = "7")
    private String retrasoMin;

    @Schema(description = "Código de la estación actual o la última por la que ha pasado", example = "18000")
    private String codEstAct;

    @Schema(description = "Código de la siguiente estación en la que efectuará parada", example = "71801")
    private String codEstSig;

    @Schema(description = "Hora estimada de llegada a la siguiente estación (HH:mm:ss)", example = "15:42:10")
    private String horaLlegadaSigEst;

    @Schema(description = "Código de la estación de destino final del trayecto", example = "60000")
    private String codEstDest;

    @Schema(description = "Código de la estación de origen donde inició el trayecto", example = "15000")
    private String codEstOrig;

    @Schema(description = "Porcentaje de avance o progreso del tren en su trayecto actual (0-100)", example = "45.5")
    private String porAvanc;

    @Schema(description = "Coordenada de latitud de la posición actual del vehículo", example = "40.4167")
    private Double latitud;

    @Schema(description = "Coordenada de longitud de la posición actual del vehículo", example = "-3.7037")
    private Double longitud;

    @Schema(description = "Identificador del núcleo de Cercanías al que pertenece, si aplica", example = "Madrid")
    private String nucleo;

    @Schema(description = "Indica si el vehículo ferroviario está adaptado para personas con movilidad reducida", example = "true")
    private Boolean accesible;

    @Schema(description = "Número de vía de la estación actual donde se encuentra estacionado o pasando", example = "3")
    private String via;

    @Schema(description = "Número de vía asignado para la próxima estación", example = "2B")
    private String nextVia;
}