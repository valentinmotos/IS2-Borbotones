import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../../../lib/api.js";
import { pesos } from "../precios.js";

const apiUrl = import.meta.env.VITE_API_URL || "/api";
const dolares = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "USD",
  currencyDisplay: "code",
});
const tasaEnPesos = new Intl.NumberFormat("es-AR", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 20,
});
const numero = new Intl.NumberFormat("es-AR");
const fechaCorta = new Intl.DateTimeFormat("es-AR", {
  dateStyle: "short",
  timeZone: "UTC",
});

function PrecioEnDolares({ precio, cotizacion }) {
  if (cotizacion === undefined)
    return <p role="status">Consultando cotización…</p>;
  if (!cotizacion)
    return <p role="status">Cotización en dólares no disponible.</p>;

  const { valorVenta, fecha } = cotizacion;
  return (
    <>
      <p>
        <strong>{dolares.format(precio / valorVenta)}</strong> por bulto
      </p>
      <p className="product-detail-rate">
        Cotización de venta del{" "}
        <time dateTime={fecha}>{fechaCorta.format(new Date(fecha))}</time>
        {" · "}1 USD = {tasaEnPesos.format(valorVenta)} ARS
      </p>
    </>
  );
}

function FichaProducto({ productoId }) {
  const [resultado, setResultado] = useState(null);
  const [cotizacion, setCotizacion] = useState(undefined);
  const [imagenFallida, setImagenFallida] = useState(false);

  useEffect(() => {
    const solicitud = new AbortController();
    const opciones = { signal: solicitud.signal, skipAuth: true };
    api
      .get(`/publico/catalogo/${encodeURIComponent(productoId)}`, opciones)
      .then(
        ({ data }) => {
          if (!solicitud.signal.aborted) setResultado({ producto: data });
        },
        (error) => {
          if (!solicitud.signal.aborted)
            setResultado({ noDisponible: error.status === 404 });
        },
      );
    api.get("/publico/cotizacion", opciones).then(
      ({ data }) => {
        if (solicitud.signal.aborted) return;
        const valida =
          Number.isFinite(data?.valorVenta) &&
          data.valorVenta > 0 &&
          typeof data.fecha === "string" &&
          Number.isFinite(Date.parse(data.fecha));
        setCotizacion(valida ? data : null);
      },
      () => {
        if (!solicitud.signal.aborted) setCotizacion(null);
      },
    );
    return () => solicitud.abort();
  }, [productoId]);

  if (!resultado) return <p role="status">Cargando producto…</p>;
  const { producto, noDisponible } = resultado;
  if (!producto) {
    return noDisponible ? (
      <h1>Producto no disponible</h1>
    ) : (
      <p role="alert">
        No pudimos cargar el producto. Volvé al catálogo e intentá de nuevo.
      </p>
    );
  }

  return (
    <article className="product-detail">
      {producto.imagenId && !imagenFallida ? (
        <img
          className="product-detail-image"
          src={`${apiUrl}/imagenes/${encodeURIComponent(producto.imagenId)}`}
          alt={producto.nombre}
          onError={() => setImagenFallida(true)}
        />
      ) : (
        <div className="catalog-card-placeholder">Imagen no disponible</div>
      )}
      <div className="product-detail-body">
        <p className="catalog-category">{producto.categoriaNombre}</p>
        <h1>{producto.nombre}</h1>
        {producto.descripcion && <p>{producto.descripcion}</p>}
        <dl className="product-detail-specs">
          <div>
            <dt>Presentación</dt>
            <dd>
              {producto.presentacion} ·{" "}
              {numero.format(producto.unidadesPorBulto)}{" "}
              {producto.unidadesPorBulto === 1 ? "unidad" : "unidades"} por
              bulto
            </dd>
          </div>
          <div>
            <dt>Stock disponible</dt>
            <dd>
              {numero.format(producto.stock)}{" "}
              {producto.stock === 1 ? "bulto" : "bultos"}
            </dd>
          </div>
        </dl>
        <div className="product-detail-prices">
          <p>
            <strong>{pesos.format(producto.precio)}</strong> ARS por bulto
          </p>
          <PrecioEnDolares precio={producto.precio} cotizacion={cotizacion} />
        </div>
      </div>
    </article>
  );
}

export default function DetalleProductoPage() {
  const { productoId } = useParams();
  return (
    <section className="shop-range catalog" aria-label="Detalle de producto">
      <div className="wrap">
        <Link className="product-detail-back" to="/catalogo">
          Volver al catálogo
        </Link>
        <FichaProducto key={productoId} productoId={productoId} />
      </div>
    </section>
  );
}
