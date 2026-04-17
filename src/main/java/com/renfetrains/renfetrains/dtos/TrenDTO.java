package com.renfetrains.renfetrains.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrenDTO {
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
}