import { useEffect, useState } from "react";
import { api } from "../../../lib/api.js";

const money = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" });

export default function CatalogoPage() {
  const [productos, setProductos] = useState([]);
  const [categorias, setCategorias] = useState([]);
  const [nombre, setNombre] = useState("");
  const [categoriaId, setCategoriaId] = useState("");
  const [estado, setEstado] = useState("loading");
  const [errorCategorias, setErrorCategorias] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    api.get("/publico/catalogo/categorias", { signal: controller.signal }).then(
      ({ data }) => setCategorias(data),
      (error) => { if (error.name !== "AbortError") setErrorCategorias(true); },
    );
    return () => controller.abort();
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    setEstado("loading");
    const query = new URLSearchParams();
    if (nombre.trim()) query.set("nombre", nombre.trim());
    if (categoriaId) query.set("categoriaId", categoriaId);
    api.get(`/publico/catalogo${query.size ? `?${query}` : ""}`, { signal: controller.signal }).then(
      ({ data }) => { setProductos(data); setEstado("ready"); },
      (error) => { if (error.name !== "AbortError") setEstado("error"); },
    );
    return () => controller.abort();
  }, [nombre, categoriaId]);

  return (
    <section className="shop-range catalog" aria-labelledby="catalogo-titulo">
      <div className="wrap">
        <p className="catalog-kicker">Venta mayorista</p>
        <h2 id="catalogo-titulo">Catálogo público</h2>
        <p className="catalog-intro">Productos disponibles, con precio mayorista vigente.</p>
        <div className="catalog-categories" aria-label="Categorías disponibles">
          {errorCategorias && <p role="alert">No pudimos cargar las categorías.</p>}
          {!errorCategorias && categorias.length === 0 && <p role="status">Cargando categorías…</p>}
          {categorias.map((categoria) => <button
            className={categoriaId === categoria.id ? "is-selected" : ""}
            type="button"
            key={categoria.id}
            aria-pressed={categoriaId === categoria.id}
            onClick={() => setCategoriaId((actual) => actual === categoria.id ? "" : categoria.id)}
          >{categoria.nombre}</button>)}
        </div>
        <div className="catalog-filters">
          <label>Buscar producto
            <input type="search" value={nombre} onChange={(event) => setNombre(event.target.value)} placeholder="Nombre del producto" />
          </label>
          <label>Categoría
            <select value={categoriaId} onChange={(event) => setCategoriaId(event.target.value)}>
              <option value="">Todas las categorías</option>
              {categorias.map((categoria) => <option value={categoria.id} key={categoria.id}>{categoria.nombre}</option>)}
            </select>
          </label>
        </div>
        {estado === "loading" && <p role="status">Cargando productos…</p>}
        {estado === "error" && <p role="alert">No pudimos cargar el catálogo. Intentá de nuevo.</p>}
        {estado === "ready" && productos.length === 0 && <p>No hay productos disponibles para esta búsqueda.</p>}
        {estado === "ready" && productos.length > 0 && <ul className="catalog-grid">
          {productos.map((producto) => <li className="catalog-card" key={producto.id}>
            {producto.imagenId ? <img src={`${import.meta.env.VITE_API_URL || "/api"}/imagenes/${producto.imagenId}`} alt={producto.nombre} loading="lazy" /> : <div className="catalog-card-placeholder" aria-hidden="true">Dulce Mayor</div>}
            <div className="catalog-card-body">
              <span className="catalog-category">{producto.categoriaNombre}</span>
              <h3>{producto.nombre}</h3>
              {producto.descripcion && <p>{producto.descripcion}</p>}
              <p className="catalog-presentation">{producto.presentacion} · {producto.unidadesPorBulto} unidades por bulto</p>
              <strong className="catalog-price">{money.format(producto.precioMayorista)} <small>por bulto</small></strong>
            </div>
          </li>)}
        </ul>}
      </div>
    </section>
  );
}
