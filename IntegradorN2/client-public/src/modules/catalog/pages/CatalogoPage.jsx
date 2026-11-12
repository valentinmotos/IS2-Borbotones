import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  ArrowRight,
  Candy,
  CircleAlert,
  Info,
  MessageCircle,
  Search,
  SearchX,
} from "lucide-react";
import { api } from "../../../lib/api.js";
import { dolares, pesos } from "../precios.js";
import { usePublicData } from "../../company/usePublicData.js";
import "../styles.css";
const apiUrl = import.meta.env.VITE_API_URL || "/api";
/** HU-02, criterio 4: el catálogo se vuelve a consultar solo, así un producto que llega a stock 0 desaparece. */
const REFRESCO_MS = 20_000;
const esqueletos = Array.from({ length: 8 }, (_, indice) => indice);

/** E3-01: catálogo público. Solo llegan productos con stock y precio vigente. */
export default function CatalogoPage() {
  const [categorias, setCategorias] = useState({ estado: "loading", data: [] });
  const [nombre, setNombre] = useState("");
  const [busqueda, setBusqueda] = useState("");
  const [categoriaId, setCategoriaId] = useState("");
  // El resultado guarda los filtros con los que se pidió: si no coinciden con los actuales, está cargando
  const [resultado, setResultado] = useState(null);
  const [refresco, setRefresco] = useState(0);
  const cotizacion = usePublicData("/publico/cotizacion");
  const dolar =
    cotizacion.status === "ready" ? cotizacion.data.valorVenta : null;

  useEffect(() => {
    const refrescar = () => {
      if (document.visibilityState === "visible") setRefresco((n) => n + 1);
    };
    const intervalo = setInterval(refrescar, REFRESCO_MS);
    document.addEventListener("visibilitychange", refrescar);
    return () => {
      clearInterval(intervalo);
      document.removeEventListener("visibilitychange", refrescar);
    };
  }, []);

  useEffect(() => {
    const solicitud = new AbortController();
    api.get("/publico/catalogo/categorias", { signal: solicitud.signal }).then(
      ({ data }) => {
        if (!solicitud.signal.aborted) setCategorias({ estado: "ready", data });
      },
      () => {
        if (!solicitud.signal.aborted)
          setCategorias({ estado: "error", data: [] });
      },
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
      ({ data }) => {
        if (!solicitud.signal.aborted) setResultado({ clave, data });
      },
      () => {
        if (!solicitud.signal.aborted) setResultado({ clave, error: true });
      },
    );
    return () => solicitud.abort();
    // refresco no cambia la clave: los productos actuales siguen visibles hasta que llega la respuesta
  }, [busqueda, categoriaId, clave, refresco]);

  const cargando = resultado?.clave !== clave;
  const productos = !cargando && !resultado.error ? resultado.data : [];

  return (
    <section className="page catalog-page" aria-labelledby="catalogo-titulo">
      <div className="wrap">
        <header className="page-head">
          <div>
            <p className="eyebrow">Venta mayorista</p>
            <h1 id="catalogo-titulo">Catálogo</h1>
            <p className="lead">
              Productos con stock y precio mayorista vigente. Podés mirar todo
              sin registrarte.
            </p>
          </div>
          <div className="page-head-actions">
            <Link className="dm-button" to="/presupuesto">
              <MessageCircle size={16} aria-hidden="true" /> Armar presupuesto
            </Link>
          </div>
        </header>

        <div className="catalog-info">
          <span className="catalog-info-icon" aria-hidden="true">
            <Info size={20} />
          </span>
          <div className="catalog-info-text">
            <p>
              <strong>Los precios son mayoristas y por bulto.</strong> Los
              pedidos los carga nuestro preventor cuando visita tu comercio.
            </p>
            <p className="catalog-info-links">
              <Link to="/como-comprar">
                Cómo comprar <ArrowRight size={15} aria-hidden="true" />
              </Link>
              <Link to="/presupuesto">
                Pedir una cotización estimada{" "}
                <ArrowRight size={15} aria-hidden="true" />
              </Link>
            </p>
          </div>
          <p className="catalog-dollar" role="status">
            {cotizacion.status === "loading" &&
              "Consultando cotización del dólar…"}
            {cotizacion.status === "error" &&
              "Cotización del dólar no disponible por el momento."}
            {dolar && (
              <>
                Dólar oficial (venta): <strong>{pesos.format(dolar)}</strong> ·
                aceptamos pagos en pesos o dólares
              </>
            )}
          </p>
        </div>

        <div className="catalog-toolbar">
          <label className="catalog-search">
            <span className="visually-hidden">Buscar producto</span>
            <Search size={18} aria-hidden="true" />
            <input
              className="dm-input"
              type="search"
              value={nombre}
              onChange={(event) => setNombre(event.target.value)}
              placeholder="Buscar producto por nombre"
            />
          </label>
          <div
            className="catalog-chips"
            role="group"
            aria-label="Filtrar por categoría"
          >
            {categorias.estado === "loading" && (
              <p className="catalog-chips-note" role="status">
                Cargando categorías…
              </p>
            )}
            {categorias.estado === "error" && (
              <p className="catalog-chips-note" role="alert">
                No pudimos cargar las categorías.
              </p>
            )}
            {categorias.estado === "ready" && categorias.data.length > 0 && (
              <>
                <button
                  type="button"
                  className={categoriaId === "" ? "is-selected" : ""}
                  aria-pressed={categoriaId === ""}
                  onClick={() => setCategoriaId("")}
                >
                  Todas
                </button>
                {categorias.data.map((categoria) => (
                  <button
                    className={
                      categoriaId === categoria.id ? "is-selected" : ""
                    }
                    type="button"
                    key={categoria.id}
                    aria-pressed={categoriaId === categoria.id}
                    onClick={() => setCategoriaId(categoria.id)}
                  >
                    {categoria.nombre}
                  </button>
                ))}
              </>
            )}
          </div>
        </div>

        {cargando && (
          <>
            <p className="visually-hidden" role="status">
              Cargando productos…
            </p>
            <ul className="catalog-grid" role="list" aria-hidden="true">
              {esqueletos.map((indice) => (
                <li className="catalog-card catalog-skeleton" key={indice}>
                  <span className="catalog-card-media" />
                  <span className="catalog-card-body">
                    <span className="catalog-skeleton-line is-short" />
                    <span className="catalog-skeleton-line" />
                    <span className="catalog-skeleton-line is-price" />
                  </span>
                </li>
              ))}
            </ul>
          </>
        )}
        {!cargando && resultado.error && (
          <div className="empty-state catalog-state" role="alert">
            <CircleAlert size={32} aria-hidden="true" />
            <strong>No pudimos cargar el catálogo.</strong>
            <p>Revisá tu conexión e intentá de nuevo en unos segundos.</p>
          </div>
        )}
        {!cargando && !resultado.error && productos.length === 0 && (
          <div className="empty-state catalog-state">
            <SearchX size={32} aria-hidden="true" />
            <strong>No hay productos disponibles para esta búsqueda.</strong>
            <p>Probá con otro nombre o elegí «Todas» en las categorías.</p>
          </div>
        )}
        {productos.length > 0 && (
          <>
            <p className="catalog-count">
              {productos.length}{" "}
              {productos.length === 1
                ? "producto disponible"
                : "productos disponibles"}
            </p>
            <ul className="catalog-grid" role="list">
              {productos.map((producto) => (
                <li className="catalog-card" key={producto.id}>
                  <span className="catalog-card-media">
                    {producto.imagenId ? (
                      <img
                        src={`${apiUrl}/imagenes/${producto.imagenId}`}
                        alt={producto.nombre}
                        loading="lazy"
                      />
                    ) : (
                      <span
                        className="catalog-card-placeholder"
                        aria-hidden="true"
                      >
                        <Candy size={34} />
                        Dulce Mayor
                      </span>
                    )}
                  </span>
                  <div className="catalog-card-body">
                    <span className="catalog-tag">
                      {producto.categoriaNombre}
                    </span>
                    <h3>
                      <Link
                        className="catalog-card-link"
                        to={`/catalogo/${encodeURIComponent(producto.id)}`}
                      >
                        {producto.nombre}
                      </Link>
                    </h3>
                    {producto.descripcion && (
                      <p className="catalog-card-desc">
                        {producto.descripcion}
                      </p>
                    )}
                    <p className="catalog-card-presentation">
                      {producto.presentacion} · {producto.unidadesPorBulto}{" "}
                      {producto.unidadesPorBulto === 1 ? "unidad" : "unidades"}{" "}
                      por bulto
                    </p>
                    <div className="catalog-card-foot">
                      <p className="catalog-price">
                        <strong>{pesos.format(producto.precio)}</strong>
                        <small>por bulto</small>
                        {dolar && (
                          <span className="catalog-price-usd">
                            ≈ {dolares.format(producto.precio / dolar)}
                          </span>
                        )}
                      </p>
                      <span className="catalog-card-more" aria-hidden="true">
                        Ver detalle <ArrowRight size={15} />
                      </span>
                    </div>
                  </div>
                </li>
              ))}
            </ul>
          </>
        )}
      </div>
    </section>
  );
}
