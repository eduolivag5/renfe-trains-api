package com.renfetrains.renfetrains.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "stops")
@Getter
@Setter
public class Stop {
    @Id
    @Column(name = "stop_id")
    private String stopId;

    private String name;
    private Double lat;
    private Double lon;

    @Column(name = "wheelchair_boarding")
    private Integer wheelchairBoarding;

    private String source; // Guardará "NACIONAL" o "CERCANIAS_MADRID"
}