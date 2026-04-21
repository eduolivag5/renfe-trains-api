package com.renfetrains.renfetrains.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "stop_times")
@Getter
@Setter
@IdClass(StopTimeId.class)
public class StopTime {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Id
    @Column(name = "stop_sequence")
    private Integer stopSequence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stop_id")
    private Stop stop;

    @Column(name = "arrival_time")
    private String arrivalTime;

    @Column(name = "departure_time")
    private String departureTime;

    @Column(name = "stop_headsign")
    private String stopHeadsign;
}