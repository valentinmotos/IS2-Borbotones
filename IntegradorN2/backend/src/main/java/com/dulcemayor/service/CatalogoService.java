package com.dulcemayor.service;

import com.dulcemayor.dto.ProductoCatalogoDTO;
import com.dulcemayor.dto.CategoriaResponse;
import com.dulcemayor.repository.CatalogoRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogoService {

    private final CatalogoRepository catalogo;
    private final CategoriaService categorias;

    public CatalogoService(CatalogoRepository catalogo, CategoriaService categorias) {
        this.catalogo = catalogo;
        this.categorias = categorias;
    }

    public List<CategoriaResponse> listarCategorias() {
        return categorias.listarCategoriaActivo();
    }

    public List<ProductoCatalogoDTO> buscar(String nombre, String categoriaId) {
        String texto = nombre == null ? "" : nombre.strip();
        String categoria = categoriaId == null ? "" : categoriaId.strip();
        return catalogo.buscarDisponibles(texto, categoria, LocalDate.now());
    }
}
