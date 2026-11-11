import { useEffect, useState } from "react";
import { api } from "../../../lib/api.js";

const money = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" });
const apiUrl = import.meta.env.VITE_API_URL || "/api";

/** E3-01: catálogo público. Solo llegan productos con stock y precio vigente. */
export default function CatalogoPage() {
  const [categorias, setCategorias] = useState({ estado: "loading", data: [] });
  const [nombre, setNombre] = useState("");
  const [busqueda, setBusqueda] = useState("");
  const [categoriaId, setCategoriaId] = useState("");
  // El resultado guarda los filtros con los que se pidió: si no coinciden con los actuales, está cargando
  const [resultado, setResultado] = useState(null);

  useEffect(() => {
    const solicitud = new AbortController();
    api.get("/publico/catalogo/categorias", { signal: solicitud.signal }).then(
      ({ data }) => { if (!solicitud.signal.aborted) setCategorias({ estado: "ready", data }); },
      () => { if (!solicitud.signal.aborted) setCategorias({ estado: "error", data: [] }); },
    );
    return () => solicitud.abort();
  }, []);

  // Espera a que el usuario deje de escribir antes de consultar
  useEffect(() => {
    const espera = setTimeout(() => setBusqueda(nombre.trim()), 300);
    return () => clearTimeout(espera);
  }, [nombre]);

  const clave = `${busqueda}|${categoriaId}`;
  useEffect(() => {
    const solicitud = new AbortController();
    const params = {};
    if (busqueda) params.nombre = busqueda;
    if (categoriaId) params.categoriaId = categoriaId;
    api.get("/publico/catalogo", { params, signal: solicitud.signal }).then(
      ({ data }) => { if (!solicitud.signal.aborted) setResultado({ clave, data }); },
      () => { if (!solicitud.signal.aborted) setResultado({ clave, error: true }); },
    );
    return () => solicitud.abort();
  }, [busqueda, categoriaId, clave]);

  const cargando = resultado?.clave !== clave;
  const productos = !cargando && !resultado.error ? resultado.data : [];

  return (
    <section className="shop-range catalog" aria-labelledby="catalogo-titulo">
      <div className="wrap">
        <p className="catalog-kicker">Venta mayorista</p>
        <h2 id="catalogo-titulo">Catálogo</h2>
        <p className="catalog-intro">Productos disponibles, con precio mayorista vigente.</p>
        <div className="catalog-filters">
          <label>Buscar producto
            <input type="search" value={nombre} onChange={(event) => setNombre(event.target.value)} placeholder="Nombre del producto" />
          </label>
        </div>
        <div className="catalog-categories" role="group" aria-label="Filtrar por categoría">
          {categorias.estado === "loading" && <p role="status">Cargando categorías…</p>}
          {categorias.estado === "error" && <p role="alert">No pudimos cargar las categorías.</p>}
          {categorias.estado === "ready" && categorias.data.length > 0 && <>
            <button type="button" className={categoriaId === "" ? "is-selected" : ""} aria-pressed={categoriaId === ""} onClick={() => setCategoriaId("")}>Todas</button>
            {categorias.data.map((categoria) => <button
              className={categoriaId === categoria.id ? "is-selected" : ""}
              type="button"
              key={categoria.id}
              aria-pressed={categoriaId === categoria.id}
              onClick={() => setCategoriaId(categoria.id)}
            >{categoria.nombre}</button>)}
          </>}
        </div>
        {cargando && <p role="status">Cargando productos…</p>}
        {!cargando && resultado.error && <p role="alert">No pudimos cargar el catálogo. Intentá de nuevo.</p>}
        {!cargando && !resultado.error && productos.length === 0 && <p>No hay productos disponibles para esta búsqueda.</p>}
        {productos.length > 0 && <ul className="catalog-grid">
          {productos.map((producto) => <li className="catalog-card" key={producto.id}>
            {producto.imagenId ? <img src={`${apiUrl}/imagenes/${producto.imagenId}`} alt={producto.nombre} loading="lazy" /> : <div className="catalog-card-placeholder" aria-hidden="true">Dulce Mayor</div>}
            <div className="catalog-card-body">
              <span className="catalog-category">{producto.categoriaNombre}</span>
              <h3>{producto.nombre}</h3>
              {producto.descripcion && <p>{producto.descripcion}</p>}
              <p className="catalog-presentation">{producto.presentacion} · {producto.unidadesPorBulto} unidades por bulto</p>
              <strong className="catalog-price">{money.format(producto.precio)} <small>por bulto</small></strong>
            </div>
          </li>)}
        </ul>}
      </div>
    </section>
  );
}
