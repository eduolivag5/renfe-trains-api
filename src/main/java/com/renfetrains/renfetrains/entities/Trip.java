package com.renfetrains.renfetrains.entities;

import com.renfetrains.renfetrains.entities.Route;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "trips")
@Getter
@Setter
public class Trip {
    @Id
    @Column(name = "trip_id")
    private String tripId;

    @Column(name = "service_id") // <--- NUEVA COLUMNA
    private String serviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;

    private String headsign;

    @Column(name = "wheelchair_accessible")
    private Integer wheelchairAccessible;

    @Column(name = "shape_id")
    private String shapeId;
}