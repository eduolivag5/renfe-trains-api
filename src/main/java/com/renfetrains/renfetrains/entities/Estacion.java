package com.renfetrains.renfetrains.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "estaciones")
@Data
public class Estacion {
    @Id
    @Column(name = "codigo")
    private String codigo;

    @Column(name = "descripcion", length = 500)
    private String nombre;

    private Double latitud;
    private Double longitud;

    @Column(name = "direccion", length = 500)
    private String direccion;

    @Column(name = "cp")
    private String cp;

    private String poblacion;
    private String provincia;
    private String pais;

    // Campos del CSV para identificar tipos de red (vienen como "SI" / "NO" habitualmente)
    @Column(name = "cercanias")
    private String cercanias;

    @Column(name = "feve")
    private String feve;

    @Column(name = "comun")
    private String comun;
}