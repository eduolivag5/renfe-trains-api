package com.renfetrains.renfetrains.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "trenes_en_vivo")
@Data
public class TrenEnVivo {
    @Id
    private String tripId;

    private String codTren;
    private String codLinea;
    private String retrasoMin;
    private String codEstAct;
    private String codEstSig;
    private String horaLlegadaSigEst;
    private String codEstDest;
    private String codEstOrig;
    private String porAvanc;
    private Double latitud;
    private Double longitud;
    private String nucleo;
    private Boolean accesible;
    private String via;
    private String nextVia;

    private LocalDateTime ultimaActualizacion;
}