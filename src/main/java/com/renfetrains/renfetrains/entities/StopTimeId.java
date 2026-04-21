package com.renfetrains.renfetrains.entities;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Setter
@Getter
public class StopTimeId implements Serializable {
    // Getters y Setters
    // IMPORTANTE: El nombre debe ser 'trip' (como en StopTime)
    // y el tipo debe ser 'String' (como el ID de la clase Trip)
    private String trip;
    private Integer stopSequence;

    public StopTimeId() {}

    public StopTimeId(String trip, Integer stopSequence) {
        this.trip = trip;
        this.stopSequence = stopSequence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StopTimeId that = (StopTimeId) o;
        return Objects.equals(trip, that.trip) &&
                Objects.equals(stopSequence, that.stopSequence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trip, stopSequence);
    }
}