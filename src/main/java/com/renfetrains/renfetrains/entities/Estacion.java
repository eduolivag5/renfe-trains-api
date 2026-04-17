package com.renfetrains.renfetrains.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "estaciones")
@Data // Si usas Lombok
public class Estacion {
    @Id
    @Column(name = "codigo")
    private String codigo;

    @Column(name = "descripcion")
    private String nombre;

    private Double latitud;
    private Double longitud;
    private String poblacion;
    private String provincia;
}
