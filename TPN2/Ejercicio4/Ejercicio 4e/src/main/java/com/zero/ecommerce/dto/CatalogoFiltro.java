package com.zero.ecommerce.dto;

public record CatalogoFiltro(
        String categoriaId,
        String subCategoriaId,
        Boolean soloOfertas,
        Double precioMin,
        Double precioMax,
        String talle,
        String orden
) {

    public static CatalogoFiltro todos() {
        return new CatalogoFiltro(null, null, null, null, null, null, null);
    }

    public static CatalogoFiltro porCategoria(String categoriaId) {
        return new CatalogoFiltro(categoriaId, null, null, null, null, null, null);
    }

    public static CatalogoFiltro porSubCategoria(String categoriaId, String subCategoriaId) {
        return new CatalogoFiltro(categoriaId, subCategoriaId, null, null, null, null, null);
    }

    public static CatalogoFiltro ofertas() {
        return new CatalogoFiltro(null, null, true, null, null, null, null);
    }

    public static CatalogoFiltro busqueda(Double precioMin, Double precioMax,
            String talle, Boolean soloOfertas, String orden) {
        return new CatalogoFiltro(null, null, soloOfertas, precioMin, precioMax, talle, orden);
    }

    public static CatalogoFiltro busquedaEnCategoria(String categoriaId, String subCategoriaId,
            Double precioMin, Double precioMax, String talle, Boolean soloOfertas, String orden) {
        return new CatalogoFiltro(categoriaId, subCategoriaId, soloOfertas,
                precioMin, precioMax, talle, orden);
    }
}
