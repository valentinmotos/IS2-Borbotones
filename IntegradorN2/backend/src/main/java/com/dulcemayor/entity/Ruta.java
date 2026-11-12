package com.dulcemayor.entity;

import com.dulcemayor.entity.enums.RutaEstado;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "rutas")
@Getter
@Setter
public class Ruta extends BaseEntity {
    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RutaEstado estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preventor_id")
    private Preventor preventor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distribuidor_id")
    private Distribuidor distribuidor;

    /** Composición Ruta 0..n ParadaRuta, en el orden de visita. */
    @OneToMany(mappedBy = "ruta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<ParadaRuta> paradas = new ArrayList<>();

    /** E5-02: una ruta con distribuidor es una ruta de reparto (sus paradas son pedidos a entregar). */
    public boolean esDeReparto() {
        return distribuidor != null;
    }

    /** Agrega una parada manteniendo las dos puntas de la relación. */
    public void agregarParada(ParadaRuta parada) {
        parada.setRuta(this);
        paradas.add(parada);
    }
}
