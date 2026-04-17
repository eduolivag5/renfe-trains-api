package com.renfetrains.renfetrains.dtos;

import lombok.Data;
import java.util.List;

@Data
public class RenfeResponseDTO {
    private String fechaActualizacion;
    private List<TrenDTO> trenes; // Aquí es donde Jackson meterá el array
}