package com.renfetrains.renfetrains.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "stop_time_updates")
@Data
public class StopTimeUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String stopId;       // "50602"
    private Integer delay;       // 180 (segundos)
    private LocalDateTime tiempoEstimado; // Hora de llegada real calculada
}