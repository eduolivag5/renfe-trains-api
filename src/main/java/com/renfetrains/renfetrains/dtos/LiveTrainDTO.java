package com.renfetrains.renfetrains.dtos;

import java.util.List;

public record LiveTrainDTO(
        String tripId,
        String routeId,
        String trainType,
        String routeName,
        String color,
        double latitude,
        double longitude,
        String status,
        int delayMinutes,
        String nextStopId,
        List<StopDetailDTO> itinerary
) {}