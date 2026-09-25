package com.zero.ecommerce.services;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.zero.ecommerce.dto.FilaTablaImagenDTO;
import com.zero.ecommerce.entities.Imagen;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.SubCategoria;
import com.zero.ecommerce.entities.VigenciaPrecio;
import com.zero.ecommerce.entities.enums.TipoImagen;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.ProductoRepository;
import com.zero.ecommerce.utils.TextoUtils;

@Service
@Transactional(readOnly = true)
public class ProductoService {

    private static final int LARGO_MAXIMO_CODIGO = 50;
    private static final int LARGO_MAXIMO_NOMBRE = 150;
    private static final int LARGO_MAXIMO_TALLE = 20;
    private static final int LARGO_MAXIMO_DESCRIPCION = 2000;

    // Se muestran como respaldo cuando todavía no hay precio vigente o hasta integrar el stock.
    private static final String SIN_PRECIO = "Sin precio";
    private static final String SIN_STOCK = "0";

    private final ProductoRepository repository;
    private final SubCategoriaService subCategoriaService;
    private final ImagenService imagenService;
    private final VigenciaPrecioService vigenciaPrecioService;

    public ProductoService(ProductoRepository repository, SubCategoriaService subCategoriaService,
            ImagenService imagenService, VigenciaPrecioService vigenciaPrecioService) {
        this.repository = repository;
        this.subCategoriaService = subCategoriaService;
        this.imagenService = imagenService;
        this.vigenciaPrecioService = vigenciaPrecioService;
    }

    /** Alta con una imagen ya guardada (por ejemplo, desde el seeder con ImagenService). */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public Producto crearProducto(String codigo, String nombre, String descripcion, String talle, boolean enOferta,
            String idImagen, String idSubCategoria) throws ErrorServiceException {
        validarCodigo(codigo);
        SubCategoria subCategoria = validarDatos(nombre, descripcion, talle, idSubCategoria);
        Imagen imagen = validarImagen(idImagen);
        Producto producto = new Producto();
        producto.setCodigo(codigo.strip());
        producto.setImagen(imagen);
        asignarDatos(producto, nombre, descripcion, talle, enOferta, subCategoria);
        return repository.save(producto);
    }

    /**
     * Alta desde el formulario: valida los datos antes de guardar la imagen y, como todo corre
     * en una transacción, si algo falla no queda una imagen suelta.
     */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public Producto crearProductoConImagen(String codigo, String nombre, String descripcion, String talle,
            boolean enOferta, MultipartFile archivo, String idSubCategoria) throws ErrorServiceException {
        if (archivo == null || archivo.isEmpty()) {
            throw new ErrorServiceException("La imagen del producto es obligatoria.");
        }
        validarCodigo(codigo);
        validarDatos(nombre, descripcion, talle, idSubCategoria);
        Imagen imagen = imagenService.crearImagen(archivo, TipoImagen.PRODUCTO);
        return crearProducto(codigo, nombre, descripcion, talle, enOferta, imagen.getId(), idSubCategoria);
    }

    public void validarProducto(String codigo, String nombre, String descripcion, String talle, boolean enOferta,
            String idImagen, String idSubCategoria) throws ErrorServiceException {
        validarCodigo(codigo);
        validarDatos(nombre, descripcion, talle, idSubCategoria);
        validarImagen(idImagen);
    }

    // El código no se modifica (modificarProducto no lo recibe), así que solo se valida en el alta.
    // Solo se comparan los activos: un producto eliminado no se reactiva y su código se puede reutilizar.
    private void validarCodigo(String codigo) throws ErrorServiceException {
        if (codigo == null || codigo.isBlank()) {
            throw new ErrorServiceException("El código del producto es obligatorio.");
        }
        validarLargo(codigo, LARGO_MAXIMO_CODIGO, "El código");
        if (encontrarActivoPorCodigo(codigo).isPresent()) {
            throw new ErrorServiceException("Ya existe un producto activo con el código " + codigo.strip() + ".");
        }
    }

    private SubCategoria validarDatos(String nombre, String descripcion, String talle, String idSubCategoria)
            throws ErrorServiceException {
        if (nombre == null || nombre.isBlank()) {
            throw new ErrorServiceException("El nombre del producto es obligatorio.");
        }
        validarLargo(nombre, LARGO_MAXIMO_NOMBRE, "El nombre");
        if (descripcion == null || descripcion.isBlank()) {
            throw new ErrorServiceException("La descripción del producto es obligatoria.");
        }
        validarLargo(descripcion, LARGO_MAXIMO_DESCRIPCION, "La descripción");
        if (talle == null || talle.isBlank()) {
            throw new ErrorServiceException("El talle del producto es obligatorio.");
        }
        validarLargo(talle, LARGO_MAXIMO_TALLE, "El talle");
        if (idSubCategoria == null || idSubCategoria.isBlank()) {
            throw new ErrorServiceException("La subcategoría del producto es obligatoria.");
        }
        SubCategoria subCategoria = subCategoriaService.buscarSubCategoria(idSubCategoria);
        if (subCategoria.getCategoria() == null || subCategoria.getCategoria().isEliminado()) {
            throw new ErrorServiceException("La categoría de la subcategoría seleccionada fue eliminada.");
        }
        return subCategoria;
    }

    private Imagen validarImagen(String idImagen) throws ErrorServiceException {
        if (idImagen == null || idImagen.isBlank()) {
            throw new ErrorServiceException("La imagen del producto es obligatoria.");
        }
        return imagenService.buscarImagen(idImagen);
    }

    private void validarLargo(String valor, int largoMaximo, String campo) throws ErrorServiceException {
        if (valor.strip().length() > largoMaximo) {
            throw new ErrorServiceException(campo + " no puede superar los " + largoMaximo + " caracteres.");
        }
    }

    /** Edición con una imagen ya guardada. Si idImagen viene vacío, se conserva la actual. */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public void modificarProducto(String id, String nombre, String descripcion, String talle, boolean enOferta,
            String idImagen, String idSubCategoria) throws ErrorServiceException {
        Producto producto = buscarProducto(id);
        SubCategoria subCategoria = validarDatos(nombre, descripcion, talle, idSubCategoria);
        if (idImagen != null && !idImagen.isBlank()) {
            producto.setImagen(imagenService.buscarImagen(idImagen));
        }
        asignarDatos(producto, nombre, descripcion, talle, enOferta, subCategoria);
        repository.save(producto);
    }

    /** Edición desde el formulario: si no se elige un archivo nuevo, se conserva la imagen actual. */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public void modificarProductoConImagen(String id, String nombre, String descripcion, String talle,
            boolean enOferta, MultipartFile archivo, String idSubCategoria) throws ErrorServiceException {
        Producto producto = buscarProducto(id);
        validarDatos(nombre, descripcion, talle, idSubCategoria);
        String idImagen = null;
        if (archivo != null && !archivo.isEmpty()) {
            Imagen imagen = producto.getImagen() == null
                    ? imagenService.crearImagen(archivo, TipoImagen.PRODUCTO)
                    : imagenService.modificarImagen(producto.getImagen().getId(), archivo, TipoImagen.PRODUCTO);
            idImagen = imagen.getId();
        }
        modificarProducto(id, nombre, descripcion, talle, enOferta, idImagen, idSubCategoria);
    }

    private void asignarDatos(Producto producto, String nombre, String descripcion, String talle, boolean enOferta,
            SubCategoria subCategoria) {
        producto.setNombre(nombre.strip());
        producto.setDescripcion(descripcion.strip());
        producto.setTalle(talle.strip());
        producto.setEnOferta(enOferta);
        producto.setSubCategoria(subCategoria);
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void eliminarProducto(String id) throws ErrorServiceException {
        Producto producto = buscarProducto(id);
        producto.setEliminado(true);
        repository.save(producto);
    }

    public Producto buscarProducto(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("El producto no existe o fue eliminado.");
        }
        return repository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ErrorServiceException("El producto no existe o fue eliminado."));
    }

    public Producto buscarProductoPorCodigo(String codigo) throws ErrorServiceException {
        if (codigo == null || codigo.isBlank()) {
            throw new ErrorServiceException("El producto no existe o fue eliminado.");
        }
        return encontrarActivoPorCodigo(codigo)
                .orElseThrow(() -> new ErrorServiceException("No existe un producto activo con el código "
                        + codigo.strip() + "."));
    }

    /**
     * Como cada talle es un producto distinto, puede haber varios con el mismo nombre:
     * devuelve el primero por talle.
     */
    public Producto buscarProductoPorNombre(String nombre) throws ErrorServiceException {
        if (nombre == null || nombre.isBlank()) {
            throw new ErrorServiceException("El producto no existe o fue eliminado.");
        }
        return listarProductoActivo().stream()
                .filter(p -> TextoUtils.mismoNombre(p.getNombre(), nombre))
                .findFirst()
                .orElseThrow(() -> new ErrorServiceException("No existe un producto activo con el nombre "
                        + nombre.strip() + "."));
    }

    // Se compara en Java por el mismo motivo que en NacionalidadService (Unicode en SQLite).
    private Optional<Producto> encontrarActivoPorCodigo(String codigo) {
        return listarProductoActivo().stream()
                .filter(p -> TextoUtils.mismoNombre(p.getCodigo(), codigo))
                .findFirst();
    }

    public List<Producto> listarProducto() {
        return repository.findAllByOrderByNombreAscTalleAsc();
    }

    public List<Producto> listarProductoActivo() {
        return repository.findByEliminadoFalseOrderByNombreAscTalleAsc();
    }

    /**
     * Productos activos filtrados. Cada filtro es opcional (null o vacío no filtra). texto busca
     * por código o nombre, sin importar mayúsculas ni tildes.
     */
    public List<Producto> listarProductoActivo(String texto, String idCategoria, String idSubCategoria,
            Boolean enOferta) {
        String buscado = texto == null || texto.isBlank() ? null : TextoUtils.normalizar(texto);
        return listarProductoActivo().stream()
                .filter(p -> buscado == null || contiene(p.getCodigo(), buscado) || contiene(p.getNombre(), buscado))
                .filter(p -> vacio(idSubCategoria) || p.getSubCategoria().getId().equals(idSubCategoria))
                .filter(p -> vacio(idCategoria) || p.getSubCategoria().getCategoria().getId().equals(idCategoria))
                .filter(p -> enOferta == null || p.isEnOferta() == enOferta)
                .toList();
    }

    /**
     * Filas del listado del panel, paginadas. pagina empieza en 1; si se pasa del total,
     * se devuelve la última.
     */
    public Page<FilaTablaImagenDTO> listarFilaProductoActivo(String texto, String idCategoria,
            String idSubCategoria, Boolean enOferta, int pagina, int tamanio) {
        List<FilaTablaImagenDTO> filas = listarProductoActivo(texto, idCategoria, idSubCategoria, enOferta).stream()
                .map(this::armarFila)
                .toList();
        int totalPaginas = Math.max(1, (int) Math.ceil((double) filas.size() / tamanio));
        int actual = Math.min(Math.max(pagina, 1), totalPaginas);
        int desde = (actual - 1) * tamanio;
        int hasta = Math.min(desde + tamanio, filas.size());
        return new PageImpl<>(filas.subList(desde, hasta), PageRequest.of(actual - 1, tamanio), filas.size());
    }

    private FilaTablaImagenDTO armarFila(Producto producto) {
        SubCategoria subCategoria = producto.getSubCategoria();
        String imagenId = producto.getImagen() == null ? null : producto.getImagen().getId();
        String precioFormateado = SIN_PRECIO;
        try {
            VigenciaPrecio vigente = vigenciaPrecioService.buscarVigenciaVigente(producto.getId());
            if (vigente != null) {
                precioFormateado = formatearPrecio(vigente.getPrecio());
            }
        } catch (ErrorServiceException e) {
            // Un producto nuevo puede no tener todavía una vigencia de precio.
        }
        return new FilaTablaImagenDTO(producto.getId(), describirProducto(producto), imagenId, List.of(
                producto.getCodigo(),
                producto.getNombre(),
                producto.getTalle(),
                subCategoria.getCategoria().getNombre() + " / " + subCategoria.getNombre(),
                producto.isEnOferta() ? "Sí" : "No",
                precioFormateado,
                SIN_STOCK));
    }

    private String formatearPrecio(double valor) {
        NumberFormat formato = NumberFormat.getNumberInstance(new Locale("es", "AR"));
        formato.setMaximumFractionDigits(0);
        return "$" + formato.format(valor);
    }

    private String describirProducto(Producto producto) {
        return producto.getNombre() + " (talle " + producto.getTalle() + ")";
    }

    private boolean contiene(String valor, String buscado) {
        return valor != null && TextoUtils.normalizar(valor).contains(buscado);
    }

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
