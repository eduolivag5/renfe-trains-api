package com.renfetrains.renfetrains.dtos;

public record StopDetailDTO(
        int sequence,
        String stopId,          // Puede ser null si no lo necesitas
        String stopName,
        String scheduledArrival, // Hora teórica
        String actualArrival,    // Por ahora igual a la teórica hasta que metas retrasos
        boolean isPassed        // Útil para que el frontend pinte en gris las paradas pasadas
) {}
