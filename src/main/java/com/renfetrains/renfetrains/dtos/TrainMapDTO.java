package com.renfetrains.renfetrains.dtos;

public record TrainMapDTO(
        String tripId,
        String vehicleId,
        double lat,
        double lon,
        String tipo,
        String label,
        String status
) {}