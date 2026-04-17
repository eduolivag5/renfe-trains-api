package com.renfetrains.renfetrains.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "trenes_gtfs_realtime")
@Data
public class TrainGtfsRealtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String vehicleId;

    private String tripId;
    private String label;
    private Double latitud;
    private Double longitud;
    private String stopId;
    private String estado;
    private String tipo;      // "CERCANIAS" o "MEDIA_LARGA_DISTANCIA"

    private LocalDateTime ultimaActualizacion;
}