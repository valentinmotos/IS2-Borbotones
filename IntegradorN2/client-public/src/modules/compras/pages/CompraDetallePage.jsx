import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  Alert,
  DataTable,
  Kpi,
  StatusBadge,
} from "../../../../../packages/design/components.jsx";
import { api } from "../../../lib/api.js";
import { pesos } from "../../catalog/precios.js";
import {
  DescargarFacturaButton,
  PagarAhoraButton,
} from "../components/AccionesFactura.jsx";
import LineaTiempo from "../components/LineaTiempo.jsx";
import {
  estadosFactura,
  estadosPedido,
  formatearFecha,
  formatearFechaHora,
} from "../estados.js";
import "../styles.css";

// E4-08: detalle de una compra: productos, importes, estado del envío y factura. Un pedido de
// otro minorista responde 404 y se muestra el mensaje del backend.

const columnas = [
  { key: "codigo", label: "Código" },
  { key: "producto", label: "Producto" },
  { key: "presentacion", label: "Presentación" },
  { key: "cantidad", label: "Cantidad" },
  {
    key: "precioUnitario",
    label: "Precio unitario",
    render: (l) => pesos.format(l.precioUnitario),
  },
  {
    key: "subtotal",
    label: "Subtotal",
    render: (l) => pesos.format(l.subtotal),
  },
];

export default function CompraDetallePage() {
  const { pedidoId } = useParams();
  const [detalle, setDetalle] = useState(null);
  const [aviso, setAviso] = useState(null);
  const [noEncontrado, setNoEncontrado] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    api
      .get(`/minorista/compras/${encodeURIComponent(pedidoId)}`, {
        signal: controller.signal,
      })
      .then(
        ({ data }) => {
          if (controller.signal.aborted) return;
          setDetalle(data);
          document.title = `Pedido N° ${data.compra.numeroPedido} · Dulce Mayor`;
        },
        (fallo) => {
          if (!controller.signal.aborted) setNoEncontrado(fallo.message);
        },
      );
    return () => controller.abort();
  }, [pedidoId]);

  if (noEncontrado) {
    return (
      <div className="compras">
        <h1>Detalle del pedido</h1>
        <Alert tone="danger">{noEncontrado}</Alert>
        <p>
          <Link to=".." relative="path">
            Volver a mis compras
          </Link>
        </p>
      </div>
    );
  }
  if (!detalle) return <p>Cargando el pedido…</p>;

  const { compra } = detalle;
  const envio = estadosPedido[compra.estadoPedido];
  const factura = estadosFactura[compra.estadoFactura];

  return (
    <div className="compras">
      <p>
        <Link to=".." relative="path">
          ← Mis compras
        </Link>
      </p>
      <h1>Pedido N° {compra.numeroPedido}</h1>
      <p className="compras-subtitulo">
        {compra.comercio} · cargado el{" "}
        {formatearFechaHora(compra.fechaHoraCarga)}{" "}
        <StatusBadge tone={envio?.tono}>
          {envio?.etiqueta ?? compra.estadoPedido}
        </StatusBadge>
      </p>
      {aviso && (
        <Alert tone="danger" onDismiss={() => setAviso(null)}>
          {aviso}
        </Alert>
      )}

      <dl className="compras-importes">
        <Kpi label="Subtotal" value={pesos.format(compra.subtotal)} />
        <Kpi
          label="Descuento"
          value={pesos.format(compra.descuento)}
          detail={
            detalle.codigoPromocion
              ? `Código ${detalle.codigoPromocion}`
              : undefined
          }
        />
        <Kpi label="Total" value={pesos.format(compra.total)} />
      </dl>

      <section aria-labelledby="titulo-envio">
        <h2 id="titulo-envio">Estado del envío</h2>
        <LineaTiempo
          eventos={detalle.lineaDeTiempo}
          estadoActual={compra.estadoPedido}
        />
      </section>

      <section aria-labelledby="titulo-factura" className="compras-factura">
        <h2 id="titulo-factura">Factura</h2>
        {compra.facturaId ? (
          <>
            <p>
              Factura N° {compra.numeroFactura} del{" "}
              {formatearFecha(compra.fechaFactura)}{" "}
              <StatusBadge tone={factura?.tono}>
                {factura?.etiqueta ?? compra.estadoFactura}
              </StatusBadge>
            </p>
            {compra.puedePagar && (
              <p>Saldo pendiente: {pesos.format(compra.saldoPendiente)}</p>
            )}
            <div className="compras-acciones">
              <DescargarFacturaButton compra={compra} onError={setAviso} />
              {compra.puedePagar && (
                <PagarAhoraButton compra={compra} onError={setAviso} />
              )}
            </div>
          </>
        ) : (
          <p>
            La factura se emite cuando el almacén procesa el pedido. Te llega
            también por correo.
          </p>
        )}
      </section>

      <DataTable
        caption="Productos del pedido"
        columns={columnas}
        rows={detalle.detalles.map((linea, indice) => ({ ...linea, indice }))}
        rowKey={(l) => l.indice}
      />
    </div>
  );
}
