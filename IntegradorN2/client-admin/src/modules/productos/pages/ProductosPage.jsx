import { useEffect, useState } from "react";
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
    return <span>Sin imagen disponible</span>;
  return <span>Cargando imagen…</span>;
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
      className="design-form productos-form"
      onSubmit={guardar}
      aria-labelledby="producto-formulario"
    >
      <h2 id="producto-formulario">
        {producto ? "Editar producto" : "Nuevo producto"}
      </h2>
      {error && <Alert tone="danger">{error.message}</Alert>}
      <fieldset disabled={guardando || bloqueado}>
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
        <label className="dm-field">
          <span>Descripción</span>
          <textarea
            className="dm-input"
            name="descripcion"
            rows={3}
            maxLength={1000}
            defaultValue={producto?.descripcion ?? ""}
          />
        </label>
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
        <ImageUploader
          onFileSelected={setArchivo}
          imageUrl={imagen?.url}
          label="Imagen del producto"
        />
        {producto?.imagenId && (
          <small>Elegí un archivo solo si querés reemplazar la imagen.</small>
        )}
        {!categorias.length && (
          <p>Necesitás una categoría activa para guardar.</p>
        )}
        <div className="dm-actions">
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
        <div className="dm-actions">
          <button
            className="dm-button"
            type="button"
            disabled={eliminando || guardando}
            onClick={() => setEditando(producto)}
          >
            Editar
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
    <>
      <header className="dm-page-header">
        <div>
          <h1>Productos</h1>
          <p>Administrá el catálogo, sus presentaciones e imágenes.</p>
        </div>
      </header>
      {aviso && (
        <Alert tone={aviso.tono} onDismiss={() => setAviso(null)}>
          {aviso.texto}
        </Alert>
      )}
      <section className="design-register" aria-labelledby="productos-listado">
        <h2 id="productos-listado">Listado</h2>
        <form
          className="productos-filtros"
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
                : "No hay productos para esta búsqueda."
          }
        />
        <Pagination
          page={paginaActual}
          pageCount={totalPaginas}
          onPageChange={setPagina}
        />
      </section>
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
    </>
  );
}
