package com.renfetrains.renfetrains.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trip_updates")
@Data
public class TripUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String tripId;

    private Integer delayGlobal;
    private String tipo;
    private LocalDateTime ultimaActualizacion;

    // EL CAMBIO ESTÁ AQUÍ: Inicializa la lista
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "trip_update_id")
    private List<StopTimeUpdate> stopTimeUpdates = new ArrayList<>();
}