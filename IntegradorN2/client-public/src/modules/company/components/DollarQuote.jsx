import { usePublicData } from "../usePublicData.js";

const pesos = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
});
const date = new Intl.DateTimeFormat("es-AR", {
  dateStyle: "long",
  timeZone: "UTC",
});

export default function DollarQuote() {
  const quote = usePublicData("/publico/cotizacion");

  return (
    <section className="shop-dollar" aria-labelledby="cotizacion">
      <div className="wrap shop-dollar-inner">
        <h2 id="cotizacion">Dólar oficial (venta)</h2>
        <div role="status">
          {quote.status === "loading" && <p>Consultando cotización…</p>}
          {quote.status === "error" && (
            <p>Cotización no disponible por el momento.</p>
          )}
          {quote.status === "ready" && (
            <p>
              <strong>{pesos.format(quote.data.valorVenta)} ARS</strong>
              <time dateTime={quote.data.fecha}>
                {date.format(new Date(quote.data.fecha))}
              </time>
            </p>
          )}
        </div>
      </div>
    </section>
  );
}
