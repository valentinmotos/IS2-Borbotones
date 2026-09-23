package com.borbotones.integrador1.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Entidad Categoria según el Diagrama de Diseño Integrado N.º 1.
 * 
 * Atributos según el diseño:
 * - id: String
 * - nombre: String
 * - eliminado: boolean
 * 
 * Relación futura con SubCategoria (Tarea 2):
 * Categoria (1) ──── (*) SubCategoria
 * Cuando se implemente SubCategoria, se incorporará la siguiente relación:
 * 
 *   @OneToMany(mappedBy = "categoria", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
 *   private List<SubCategoria> subCategorias = new ArrayList<>();
 */
@Entity
@Table(name = "categorias")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(nullable = false)
    private boolean eliminado = false;

    // Constructor por defecto requerido por JPA
    public Categoria() {
    }

    public Categoria(String nombre) {
        this.nombre = nombre;
        this.eliminado = false;
    }

    public Categoria(String id, String nombre, boolean eliminado) {
        this.id = id;
        this.nombre = nombre;
        this.eliminado = eliminado;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public boolean isEliminado() {
        return eliminado;
    }

    public void setEliminado(boolean eliminado) {
        this.eliminado = eliminado;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Categoria categoria = (Categoria) o;
        return Objects.equals(id, categoria.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Categoria{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", eliminado=" + eliminado +
                '}';
    }
}
