package com.renfetrains.renfetrains.dtos;

public record StopInfo(
        String stopName,
        String arrivalTime,
        int sequence
) {}