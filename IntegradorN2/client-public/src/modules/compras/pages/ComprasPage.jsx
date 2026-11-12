import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  Alert,
  DataTable,
  StatusBadge,
} from "../../../../../packages/design/components.jsx";
import { api } from "../../../lib/api.js";
import { pesos } from "../../catalog/precios.js";
import {
  DescargarFacturaButton,
  PagarAhoraButton,
} from "../components/AccionesFactura.jsx";
import {
  estadosFactura,
  estadosPedido,
  formatearFechaHora,
} from "../estados.js";
import "../styles.css";

// E4-08: historial de pedidos del minorista autenticado con su factura. El minorista sale del
// token: el backend nunca devuelve pedidos de otro.

export default function ComprasPage() {
  const [compras, setCompras] = useState(null);
  const [aviso, setAviso] = useState(null);

  useEffect(() => {
    document.title = "Mis compras · Dulce Mayor";
    const controller = new AbortController();
    api.get("/minorista/compras", { signal: controller.signal }).then(
      ({ data }) => {
        if (!controller.signal.aborted) setCompras(data);
      },
      (fallo) => {
        if (!controller.signal.aborted) {
          setCompras([]);
          setAviso(fallo.message);
        }
      },
    );
    return () => controller.abort();
  }, []);

  const columnas = [
    {
      key: "numeroPedido",
      label: "Pedido",
      render: (c) => <Link to={c.pedidoId}>N° {c.numeroPedido}</Link>,
    },
    {
      key: "fechaHoraCarga",
      label: "Fecha",
      render: (c) => formatearFechaHora(c.fechaHoraCarga),
    },
    { key: "comercio", label: "Comercio" },
    { key: "total", label: "Total", render: (c) => pesos.format(c.total) },
    {
      key: "estadoPedido",
      label: "Envío",
      render: (c) => {
        const estado = estadosPedido[c.estadoPedido];
        return (
          <StatusBadge tone={estado?.tono}>
            {estado?.etiqueta ?? c.estadoPedido}
          </StatusBadge>
        );
      },
    },
    {
      key: "estadoFactura",
      label: "Factura",
      render: (c) => {
        if (!c.facturaId) return "Todavía no se facturó";
        const estado = estadosFactura[c.estadoFactura];
        return (
          <StatusBadge tone={estado?.tono}>
            {estado?.etiqueta ?? c.estadoFactura}
          </StatusBadge>
        );
      },
    },
    {
      key: "acciones",
      label: "Acciones",
      render: (c) => (
        <div className="compras-acciones">
          <Link className="dm-button" to={c.pedidoId}>
            Ver detalle
          </Link>
          {c.facturaId && (
            <DescargarFacturaButton compra={c} onError={setAviso} />
          )}
          {c.puedePagar && <PagarAhoraButton compra={c} onError={setAviso} />}
        </div>
      ),
    },
  ];

  return (
    <div className="compras">
      <h1>Mis compras</h1>
      {aviso && (
        <Alert tone="danger" onDismiss={() => setAviso(null)}>
          {aviso}
        </Alert>
      )}
      {compras === null ? (
        <p>Cargando tus pedidos…</p>
      ) : (
        <DataTable
          caption="Tus pedidos, del más nuevo al más viejo"
          columns={columnas}
          rows={compras}
          rowKey={(c) => c.pedidoId}
          empty="Todavía no tenés pedidos. Tu preventor los carga cuando visita tu comercio."
        />
      )}
    </div>
  );
}
