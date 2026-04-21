package com.renfetrains.renfetrains.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Entity
@Table(name = "trips")
@Getter
@Setter
public class Trip {
    @Id
    @Column(name = "trip_id")
    private String tripId;

    @Column(name = "service_id")
    private String serviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;

    private String headsign;

    @Column(name = "wheelchair_accessible")
    private Integer wheelchairAccessible;

    @Column(name = "shape_id")
    private String shapeId;

    // --- RELACIONES PARA BÚSQUEDAS ---

    // Relación con los horarios de paso (fundamental para buscar estaciones)
    @OneToMany(mappedBy = "trip", fetch = FetchType.LAZY)
    private List<StopTime> stopTimes;

    // Relación con el calendario (para saber qué días circula)
    // Usamos insertable/updatable = false porque el serviceId ya lo tienes arriba
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", referencedColumnName = "service_id", insertable = false, updatable = false)
    private Calendar calendar;
}