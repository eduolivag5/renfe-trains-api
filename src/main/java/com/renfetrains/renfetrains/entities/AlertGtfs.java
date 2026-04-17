package com.renfetrains.renfetrains.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "alertas_gtfs")
@Data
public class AlertGtfs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String alertId; // "AVISO_486178"

    @Column(columnDefinition = "TEXT")
    private String descripcion; // El texto del aviso

    private LocalDateTime fechaInicio;

    @ElementCollection
    @CollectionTable(name = "alerta_lineas", joinColumns = @JoinColumn(name = "alerta_id"))
    @Column(name = "route_id")
    private List<String> routeIds; // Lista de todas las líneas afectadas

    private LocalDateTime ultimaActualizacion;
}