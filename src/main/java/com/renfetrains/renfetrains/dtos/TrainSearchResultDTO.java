package com.renfetrains.renfetrains.dtos;

public record TrainSearchResultDTO(
        String tripId,
        String departureTime,
        String arrivalTime,
        String trainType,
        String routeName
) {}