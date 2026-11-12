import { useEffect, useState } from "react";
import { Plus, Search } from "lucide-react";
import {
  Alert,
  ConfirmDialog,
  DataTable,
  FormField,
  Pagination,
} from "../../../../../packages/design/components.jsx";
import ImageUploader from "../../../components/ImageUploader.jsx";
import { api } from "../../../lib/api.js";
import { crearCrudApi } from "../../../lib/crudApi.js";
import { useProductoImagen } from "../useProductoImagen.js";
import PreciosProducto from "../components/PreciosProducto.jsx";
import "../styles.css";

const productosApi = crearCrudApi("/productos");
const categoriasApi = crearCrudApi("/categorias");
const filtrosVacios = { busqueda: "", categoriaId: "" };
const porPagina = 8;
const campos = [
  { name: "codigo", label: "Código", maxLength: 50 },
  { name: "nombre", label: "Nombre", maxLength: 160 },
  {
    name: "presentacion",
    label: "Presentación",
    maxLength: 80,
    placeholder: "Por ejemplo, Caja",
  },
  {
    name: "unidadesPorBulto",
    label: "Unidades por bulto o pack",
    type: "number",
    min: 1,
    step: 1,
  },
];

function FotoProducto({ producto }) {
  const imagen = useProductoImagen(producto.imagenId);
  if (imagen?.url)
    return (
      <img
        className="producto-miniatura"
        src={imagen.url}
        alt={producto.nombre}
      />
    );
  if (!producto.imagenId || imagen?.error)
    return <span className="producto-sin-imagen">Sin imagen disponible</span>;
  return <span className="producto-sin-imagen">Cargando imagen…</span>;
}

function FormularioProducto({
  producto,
  categorias,
  onSaved,
  onCancel,
  bloqueado,
  guardando,
  setGuardando,
}) {
  const [archivo, setArchivo] = useState(null);
  const [error, setError] = useState(null);
  const [categoriaId, setCategoriaId] = useState(producto?.categoriaId ?? "");
  const imagen = useProductoImagen(producto?.imagenId);

  async function guardar(event) {
    event.preventDefault();
    if (guardando) return;
    if (!archivo && !producto?.imagenId) {
      setError({
        campo: "imagen",
        message: "Seleccioná una imagen para el producto.",
      });
      return;
    }
    const datos = new FormData(event.currentTarget);
    datos.set("enOferta", String(datos.has("enOferta")));
    if (archivo) datos.set("imagen", archivo);
    setGuardando(true);
    setError(null);
    try {
      const guardado = producto
        ? await productosApi.modificar(producto.id, datos)
        : await productosApi.crear(datos);
      onSaved(guardado);
    } catch (failure) {
      setError(failure);
    } finally {
      setGuardando(false);
    }
  }

  return (
    <form
      id="producto-editor"
      className="panel productos-form"
      onSubmit={guardar}
      aria-labelledby="producto-formulario"
    >
      <div className="panel-head">
        <div className="productos-form-titulo">
          <span className="eyebrow">
            {producto ? "Producto seleccionado" : "Alta"}
          </span>
          <h2 id="producto-formulario">
            {producto ? `Editando: ${producto.nombre}` : "Nuevo producto"}
          </h2>
        </div>
        <p>
          {producto
            ? "Modificá los datos y guardá los cambios."
            : "Completá los datos y elegí una imagen. La descripción es opcional."}
        </p>
      </div>
      {error && <Alert tone="danger">{error.message}</Alert>}
      <fieldset disabled={guardando || bloqueado}>
        <div className="productos-form-cuerpo">
          <div className="form-grid productos-form-grid">
            {campos.map(({ name, ...props }) => (
              <FormField
                key={name}
                name={name}
                required
                {...props}
                defaultValue={producto?.[name] ?? ""}
                error={error?.campo === name ? error.message : undefined}
              />
            ))}
            <div className="dm-field">
              <label htmlFor="producto-categoria">Categoría</label>
              <select
                id="producto-categoria"
                className="dm-input"
                name="categoriaId"
                value={categoriaId}
                onChange={(event) => setCategoriaId(event.target.value)}
                required
              >
                <option value="">Seleccioná una categoría</option>
                {categorias.map(({ id, nombre }) => (
                  <option key={id} value={id}>
                    {nombre}
                  </option>
                ))}
              </select>
            </div>
            <label className="productos-check">
              <input
                type="checkbox"
                name="enOferta"
                defaultChecked={producto?.enOferta}
              />{" "}
              En oferta para el newsletter
            </label>
            <label className="dm-field span-all">
              <span>Descripción</span>
              <textarea
                className="dm-input"
                name="descripcion"
                rows={3}
                maxLength={1000}
                defaultValue={producto?.descripcion ?? ""}
              />
            </label>
          </div>
          <div className="productos-form-imagen">
            <ImageUploader
              onFileSelected={setArchivo}
              imageUrl={imagen?.url}
              label="Imagen del producto"
            />
            {producto?.imagenId && (
              <small className="muted">
                Elegí un archivo solo si querés reemplazar la imagen.
              </small>
            )}
          </div>
        </div>
        {!categorias.length && (
          <p className="productos-form-aviso">
            Necesitás una categoría activa para guardar.
          </p>
        )}
        <div className="form-actions productos-form-pie">
          <button
            className="dm-button dm-button-primary"
            type="submit"
            disabled={!categorias.length}
          >
            {guardando ? "Guardando…" : "Guardar producto"}
          </button>
          {producto && (
            <button className="dm-button" type="button" onClick={onCancel}>
              Cancelar edición
            </button>
          )}
        </div>
      </fieldset>
    </form>
  );
}

export default function ProductosPage() {
  const [categorias, setCategorias] = useState([]);
  const [resultado, setResultado] = useState(null);
  const [filtros, setFiltros] = useState(filtrosVacios);
  const [version, setVersion] = useState(0);
  const [versionFormulario, setVersionFormulario] = useState(0);
  const [pagina, setPagina] = useState(1);
  const [editando, setEditando] = useState(null);
  const [aEliminar, setAEliminar] = useState(null);
  const [preciosDe, setPreciosDe] = useState(null);
  const [eliminando, setEliminando] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [aviso, setAviso] = useState(null);

  useEffect(() => {
    const solicitud = new AbortController();
    categoriasApi.listar({ signal: solicitud.signal }).then(
      (data) => {
        if (!solicitud.signal.aborted) setCategorias(data);
      },
      (error) => {
        if (!solicitud.signal.aborted)
          setAviso({ tono: "danger", texto: error.message });
      },
    );
    return () => solicitud.abort();
  }, []);

  useEffect(() => {
    const solicitud = new AbortController();
    api
      .get("/productos/buscar", { params: filtros, signal: solicitud.signal })
      .then(
        ({ data }) => {
          if (!solicitud.signal.aborted)
            setResultado({ filtros, version, data });
        },
        (error) => {
          if (!solicitud.signal.aborted)
            setResultado({ filtros, version, error: error.message });
        },
      );
    return () => solicitud.abort();
  }, [filtros, version]);

  const actual =
    resultado?.filtros === filtros && resultado.version === version
      ? resultado
      : null;
  const productos = actual?.data ?? [];
  const totalPaginas = Math.max(1, Math.ceil(productos.length / porPagina));
  const paginaActual = Math.min(pagina, totalPaginas);
  const visibles = productos.slice(
    (paginaActual - 1) * porPagina,
    paginaActual * porPagina,
  );

  function aplicarFiltros(event) {
    event.preventDefault();
    const datos = new FormData(event.currentTarget);
    setFiltros({
      busqueda: datos.get("busqueda"),
      categoriaId: datos.get("categoriaId"),
    });
    setPagina(1);
  }

  function guardado(producto) {
    setEditando(null);
    setVersionFormulario((anterior) => anterior + 1);
    setAviso({ tono: "success", texto: `Guardaste ${producto.nombre}.` });
    setVersion((anterior) => anterior + 1);
  }

  async function confirmarBaja() {
    if (!aEliminar || eliminando) return;
    const producto = aEliminar;
    setAEliminar(null);
    setEliminando(true);
    try {
      await productosApi.eliminar(producto.id);
      if (editando?.id === producto.id) setEditando(null);
      if (preciosDe?.id === producto.id) setPreciosDe(null);
      setAviso({ tono: "success", texto: `Diste de baja ${producto.nombre}.` });
      setVersion((anterior) => anterior + 1);
    } catch (error) {
      setAviso({ tono: "danger", texto: error.message });
    } finally {
      setEliminando(false);
    }
  }

  const columnas = [
    {
      key: "imagen",
      label: "Imagen",
      render: (producto) => <FotoProducto producto={producto} />,
    },
    { key: "codigo", label: "Código" },
    { key: "nombre", label: "Nombre" },
    { key: "categoriaNombre", label: "Categoría" },
    {
      key: "presentacion",
      label: "Presentación",
      render: (p) => `${p.presentacion} · ${p.unidadesPorBulto} u.`,
    },
    {
      key: "acciones",
      label: "Acciones",
      render: (producto) => (
        <div className="abm-row-actions">
          <button
            className="dm-button"
            type="button"
            disabled={eliminando || guardando}
            onClick={() => setEditando(producto)}
          >
            Editar
          </button>
          <button
            className="dm-button"
            type="button"
            disabled={eliminando}
            onClick={() => setPreciosDe(producto)}
          >
            Precios
          </button>
          <button
            className="dm-button dm-button-danger"
            type="button"
            disabled={eliminando || guardando}
            onClick={() => setAEliminar(producto)}
          >
            Dar de baja
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="productos-page">
      <header className="page-head">
        <div>
          <span className="eyebrow">Catálogo</span>
          <h1>Productos</h1>
          <p className="lead">
            Administrá el catálogo: presentaciones, imágenes y precios
            mayoristas de cada producto.
          </p>
        </div>
        <div className="page-head-actions">
          <a className="dm-button dm-button-primary" href="#producto-editor">
            <Plus size={16} aria-hidden="true" />
            Nuevo producto
          </a>
        </div>
      </header>
      {aviso && (
        <Alert tone={aviso.tono} onDismiss={() => setAviso(null)}>
          {aviso.texto}
        </Alert>
      )}
      <section className="panel" aria-labelledby="productos-listado">
        <div className="panel-head">
          <h2 id="productos-listado">Productos activos</h2>
          {actual?.data && (
            <p>
              {productos.length === 1
                ? "1 producto"
                : `${productos.length} productos`}
            </p>
          )}
        </div>
        <form
          className="toolbar productos-filtros"
          onSubmit={aplicarFiltros}
          aria-label="Filtrar productos"
        >
          <FormField
            name="busqueda"
            type="search"
            label="Buscar por código o nombre"
          />
          <div className="dm-field">
            <label htmlFor="productos-categoria-filtro">
              Filtrar por categoría
            </label>
            <select
              id="productos-categoria-filtro"
              className="dm-input"
              name="categoriaId"
            >
              <option value="">Todas las categorías</option>
              {categorias.map(({ id, nombre }) => (
                <option key={id} value={id}>
                  {nombre}
                </option>
              ))}
            </select>
          </div>
          <button className="dm-button" type="submit">
            <Search size={16} aria-hidden="true" />
            Buscar
          </button>
        </form>
        {actual?.error && <Alert tone="danger">{actual.error}</Alert>}
        <DataTable
          caption="Productos activos"
          columns={columnas}
          rows={visibles}
          rowKey={(producto) => producto.id}
          empty={
            !actual
              ? "Cargando productos…"
              : actual.error
                ? "Listado no disponible."
                : "No hay productos para esta búsqueda. Probá con otro texto o categoría."
          }
        />
        <Pagination
          page={paginaActual}
          pageCount={totalPaginas}
          onPageChange={setPagina}
        />
      </section>
      {preciosDe && (
        <PreciosProducto
          key={preciosDe.id}
          producto={preciosDe}
          onClose={() => setPreciosDe(null)}
        />
      )}
      <FormularioProducto
        key={`${editando?.id ?? "nuevo"}-${versionFormulario}`}
        producto={editando}
        categorias={categorias}
        onSaved={guardado}
        onCancel={() => setEditando(null)}
        bloqueado={eliminando}
        guardando={guardando}
        setGuardando={setGuardando}
      />
      <ConfirmDialog
        open={aEliminar !== null}
        title="Dar de baja producto"
        confirmLabel="Dar de baja"
        onConfirm={confirmarBaja}
        onCancel={() => setAEliminar(null)}
      >
        <p>¿Querés dar de baja «{aEliminar?.nombre}»?</p>
      </ConfirmDialog>
    </div>
  );
}
