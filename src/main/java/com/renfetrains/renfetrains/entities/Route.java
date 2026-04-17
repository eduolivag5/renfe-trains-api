package com.renfetrains.renfetrains.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "routes")
@Getter @Setter
public class Route {
    @Id
    @Column(name = "route_id")
    private String routeId;

    @Column(name = "short_name")
    private String shortName;

    @Column(name = "long_name")
    private String longName;

    @Column(name = "route_type")
    private Integer routeType;

    private String color;

    @Column(name = "text_color")
    private String textColor;

    private String source; // "NACIONAL" o "CERCANIAS"

    @Column(name = "tipo_tren")
    private String tipoTren; // "AVE", "AVLO", "MD", "CERCANIAS", etc.
}