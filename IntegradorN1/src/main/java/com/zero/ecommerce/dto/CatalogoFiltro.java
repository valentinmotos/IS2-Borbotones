package com.zero.ecommerce.dto;

/**
 * Alcance base del catalogo. Se mantiene como objeto para que E3-02 pueda sumar
 * busqueda, rangos y orden sin cambiar la firma de CatalogoService.listar.
 */
public record CatalogoFiltro(String categoriaId, String subCategoriaId, Boolean soloOfertas) {

    public static CatalogoFiltro todos() {
        return new CatalogoFiltro(null, null, null);
    }

    public static CatalogoFiltro porCategoria(String categoriaId) {
        return new CatalogoFiltro(categoriaId, null, null);
    }

    public static CatalogoFiltro porSubCategoria(String categoriaId, String subCategoriaId) {
        return new CatalogoFiltro(categoriaId, subCategoriaId, null);
    }

    public static CatalogoFiltro ofertas() {
        return new CatalogoFiltro(null, null, true);
    }
}
