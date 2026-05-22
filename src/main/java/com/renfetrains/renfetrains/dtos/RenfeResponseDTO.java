package com.renfetrains.renfetrains.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "DTO raíz que representa la respuesta estructurada de los datos de Renfe obtenidos en un instante concreto")
public class RenfeResponseDTO {

    @Schema(
            description = "Fecha y hora en la que se generaron o actualizaron los datos por última vez en el sistema",
            example = "2026-05-22T14:03:00Z",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String fechaActualizacion;

    @Schema(
            description = "Lista completa de los trenes procesados en la actualización actual. Este array es mapeado automáticamente por Jackson.",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<TrenDTO> trenes;
}