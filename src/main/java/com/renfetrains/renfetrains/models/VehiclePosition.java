package com.renfetrains.renfetrains.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VehiclePosition {
    private String tripId;
    private double lat;
    private double lon;
    private String nextStopId;
    private int timestamp;
}